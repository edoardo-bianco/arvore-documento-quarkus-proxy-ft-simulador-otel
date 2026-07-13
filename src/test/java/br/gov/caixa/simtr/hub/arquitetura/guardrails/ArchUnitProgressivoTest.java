package br.gov.caixa.simtr.hub.arquitetura.guardrails;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoGateway;
import br.gov.caixa.simtr.hub.dominio.falso.ViolacaoGuardrail;
import br.gov.caixa.simtr.hub.parametrizacao.falso.DependenciaEntreDominiosViolacao;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AnalyzeClasses(packages = "br.gov.caixa.simtr.hub", importOptions = DoNotIncludeTests.class)
class ArchUnitProgressivoTest {

    private static final String PACOTE_DTO_ERRO_REST = "..arquitetura.excecao.dto..";

    @ArchTest
    static final ArchRule dominio_nao_deve_dependender_de_framework_ou_bordas = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..recurso..",
                    "..integracao..",
                    "..adaptador..",
                    "..mapeamento..",
                    "..fachada..",
                    "jakarta.enterprise..",
                    "jakarta.inject..",
                    "io.quarkus..",
                    "org.eclipse.microprofile..",
                    "com.fasterxml.jackson..");

    @ArchTest
    static final ArchRule erro_rest_tecnico_pode_ser_usado_nas_bordas_permitidas = classes()
            .that().resideInAnyPackage("..recurso..", "..integracao..", "..arquitetura.excecao..")
            .and().haveSimpleNameNotEndingWith("Dto")
            .should().dependOnClassesThat()
            .resideInAPackage(PACOTE_DTO_ERRO_REST);

    @ArchTest
    static final ArchRule erro_rest_tecnico_nao_pode_vazar_para_o_nucleo = noClasses()
            .that().resideInAnyPackage("..dominio..", "..servico..", "..fachada..", "..mapeamento..")
            .should().dependOnClassesThat()
            .resideInAPackage(PACOTE_DTO_ERRO_REST);

    @ArchTest
    static final ArchRule dominios_nao_devem_dependender_uns_dos_outros = SlicesRuleDefinition.slices()
            .matching("br.gov.caixa.simtr.hub.(parametrizacao|dossieproduto|gestaodocumento)..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule aplicacao_migrada_nao_deve_depender_de_bordas = noClasses()
            .that().resideInAPackage("..dossieproduto.aplicacao..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..integracao..", "..adaptador..", "..recurso..", "..mapeamento..", "..fachada..");

    @ArchTest
    static final ArchRule porta_de_entrada_nao_deve_expor_implementacao = noClasses()
            .that().resideInAPackage("..dossieproduto.aplicacao.porta.entrada..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..aplicacao.porta.saida..", "..aplicacao.casodeuso..");

    @ArchTest
    static final ArchRule adapters_de_saida_migrados_nao_devem_reutilizar_contratos_de_outras_bordas = noClasses()
            .that().resideInAPackage("..dossieproduto.adaptador.saida..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..recurso..", "..integracao..", "..arquitetura.excecao.dto..");

    @Test
    void regraDeErroRestDetectaUsoProibidoNoNucleo() {
        ArchRule regra = noClasses()
                .that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat()
                .resideInAPackage(PACOTE_DTO_ERRO_REST);

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(ViolacaoGuardrail.class)));
    }

    @Test
    void regraDeFrameworkDetectaUsoProibidoNoDominio() {
        ArchRule regra = noClasses()
                .that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.enterprise..", "jakarta.inject..", "io.quarkus..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(ViolacaoGuardrail.class)));
    }

    @Test
    void regraDeIsolamentoEntreDominiosDetectaDependenciaEntreSlices() {
        ArchRule regra = SlicesRuleDefinition.slices()
                .matching("br.gov.caixa.simtr.hub.(parametrizacao|dossieproduto|gestaodocumento)..")
                .should().notDependOnEachOther();

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(DependenciaEntreDominiosViolacao.class, DossieProdutoCriadoVo.class)));
    }

    @Test
    void regraDeAplicacaoDetectaDependenciaDeAdapter() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..integracao..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(AplicacaoComAdapterViolacao.class)));
    }

    @Test
    void regraDePortaDeEntradaDetectaExposicaoDaPortaDeSaida() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..aplicacao.porta.saida..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(PortaEntradaComSaidaViolacao.class)));
    }

    @Test
    void regraDeAdapterMtrDetectaReusoDeDtoPublico() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..recurso.rest..dto..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(AdapterMtrComDtoPublicoViolacao.class)));
    }

    private static final class AplicacaoComAdapterViolacao {
        private DossieProdutoGateway gateway;
    }

    private static final class PortaEntradaComSaidaViolacao {
        private br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto portaSaida;
    }

    private static final class AdapterMtrComDtoPublicoViolacao {
        private br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto dto;
    }
}

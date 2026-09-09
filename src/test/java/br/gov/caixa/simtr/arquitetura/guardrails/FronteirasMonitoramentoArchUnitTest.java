package br.gov.caixa.simtr.arquitetura.guardrails;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.acl.simtrhub.falso.AcessosHub;
import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.falso.AcessosDto;
import br.gov.caixa.simtr.monitoramento.dominio.falso.DependenciaAplicacaoViolacao;
import br.gov.caixa.simtr.orquestrador.adaptador.saida.acl.monitoramento.falso.AcessosMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.falso.PortaEntradaComInternos;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FronteirasMonitoramentoArchUnitTest {

    private static final String MONITORAMENTO = "br.gov.caixa.simtr.monitoramento";
    private static final String ORQUESTRADOR = "br.gov.caixa.simtr.orquestrador";
    private static final String HUB = "br.gov.caixa.simtr.hub";

    private static final JavaClasses PRODUCAO = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(MONITORAMENTO, ORQUESTRADOR, "br.gov.caixa.simtr.arquitetura");

    private static final ArchRule ACL_MONITORAMENTO = classes()
            .that().resideInAPackage(ORQUESTRADOR + ".adaptador.saida.acl.monitoramento..")
            .should().onlyDependOnClassesThat(resideOutsideOfPackage(MONITORAMENTO + "..")
                    .or(resideInAnyPackage(MONITORAMENTO + ".aplicacao.porta.entrada..",
                            MONITORAMENTO + ".dominio.modelo..")));

    private static final ArchRule ACL_HUB = classes()
            .that().resideInAPackage(MONITORAMENTO + ".adaptador.saida.acl.simtrhub..")
            .should().onlyDependOnClassesThat(resideOutsideOfPackage(HUB + "..")
                    .or(resideInAnyPackage(HUB + ".dossieproduto.aplicacao.porta.entrada..",
                            HUB + ".dossieproduto.dominio.modelo..")));

    private static final ArchRule DOMINIO_SEM_APLICACAO = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("..aplicacao..");

    private static final ArchRule PORTA_ENTRADA_SEM_INTERNOS = noClasses()
            .that().resideInAPackage("..aplicacao.porta.entrada..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..aplicacao.porta.saida..", "..aplicacao.casodeuso..");

    @ParameterizedTest
    @MethodSource("bordas")
    void dtoPertenceExclusivamenteASuaBorda(String borda) {
        isolamentoDto(borda).check(PRODUCAO);
    }

    @Test
    void aclsAcessamSomenteApiPublicaDoFornecedor() {
        ACL_MONITORAMENTO.check(PRODUCAO);
        ACL_HUB.check(PRODUCAO);
    }

    @Test
    void dominioEPortasDeEntradaPreservamDirecaoDasDependencias() {
        DOMINIO_SEM_APLICACAO.check(PRODUCAO);
        PORTA_ENTRADA_SEM_INTERNOS.check(PRODUCAO);
    }

    @ParameterizedTest
    @MethodSource("violacoes")
    void guardrailDeveRejeitarDependenciaProibida(Class<?> fixture, ArchRule regra) {
        var classes = new ClassFileImporter().importClasses(fixture);

        assertThrows(AssertionError.class, () -> regra.check(classes));
    }

    @ParameterizedTest
    @MethodSource("permitidos")
    void guardrailDeveAceitarPortasModelosPublicosEDtoNaPropriaBorda(Class<?> fixture, ArchRule regra) {
        var classes = new ClassFileImporter().importClasses(fixture,
                br.gov.caixa.simtr.monitoramento.aplicacao.falso.QuarkusReativoPermitido.class);

        assertDoesNotThrow(() -> regra.check(classes));
    }

    static Stream<String> bordas() {
        return Stream.of(ORQUESTRADOR + ".adaptador.entrada.rest.v1",
                ORQUESTRADOR + ".adaptador.entrada.servicebus",
                ORQUESTRADOR + ".adaptador.saida.servicebus",
                MONITORAMENTO + ".adaptador.entrada.servicebus",
                MONITORAMENTO + ".adaptador.saida.servicebus",
                MONITORAMENTO + ".adaptador.saida.simulador.prevalidacao");
    }

    private static ArchRule isolamentoDto(String borda) {
        return noClasses().that().resideOutsideOfPackage(borda + "..")
                .should().dependOnClassesThat().resideInAPackage(borda + ".dto..");
    }

    static Stream<Arguments> violacoes() {
        return Stream.of(
                Arguments.of(AcessosMonitoramento.CasoDeUso.class, ACL_MONITORAMENTO),
                Arguments.of(AcessosMonitoramento.PortaSaida.class, ACL_MONITORAMENTO),
                Arguments.of(AcessosMonitoramento.Dto.class, ACL_MONITORAMENTO),
                Arguments.of(AcessosHub.Interno.class, ACL_HUB),
                Arguments.of(AcessosDto.OutraBorda.class, isolamentoDto(MONITORAMENTO + ".adaptador.entrada.servicebus")),
                Arguments.of(AcessosDto.OutroComponente.class, isolamentoDto(ORQUESTRADOR + ".adaptador.entrada.servicebus")),
                Arguments.of(DependenciaAplicacaoViolacao.class, DOMINIO_SEM_APLICACAO),
                Arguments.of(PortaEntradaComInternos.class, PORTA_ENTRADA_SEM_INTERNOS));
    }

    static Stream<Arguments> permitidos() {
        return Stream.of(
                Arguments.of(AcessosMonitoramento.Permitido.class, ACL_MONITORAMENTO),
                Arguments.of(AcessosHub.Permitido.class, ACL_HUB),
                Arguments.of(AcessosDto.Permitido.class, isolamentoDto(MONITORAMENTO + ".adaptador.saida.servicebus")));
    }
}

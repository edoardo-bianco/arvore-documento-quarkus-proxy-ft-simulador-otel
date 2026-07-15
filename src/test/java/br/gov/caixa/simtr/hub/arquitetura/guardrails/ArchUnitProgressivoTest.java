package br.gov.caixa.simtr.hub.arquitetura.guardrails;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.acl.falso.AcessoInternoDossieProdutoViolacao;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.falso.DependenciaDossieProdutoViolacao;
import br.gov.caixa.simtr.hub.conformidade.falso.DependenciaArvoreDocumentoViolacao;
import br.gov.caixa.simtr.hub.dominio.falso.DependenciaAdapterNoDominioViolacao;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.falso.DependenciaAdapterNaAplicacaoViolacao;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.falso.UsoQuarkusNaAplicacaoPermitido;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.falso.AdapterEntradaComPortaSaidaViolacao;
import br.gov.caixa.simtr.hub.dominio.falso.UsoQuarkusNoDominioPermitido;
import br.gov.caixa.simtr.hub.dominio.falso.ViolacaoGuardrail;
import com.azure.storage.falso.BlobClientViolacao;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.lang.ArchRule;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArchUnitProgressivoTest {

    private static final String PACOTE_DTO_ERRO_REST = "..arquitetura.excecao.dto..";
    private static final String[] PACOTES_NUCLEO_GESTAO_DOCUMENTO = {
            "..gestaodocumento.dominio..",
            "..gestaodocumento.aplicacao.."
    };
    private static final String PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO =
            "(?i).*(cache|renew|refresh|renov|upload|blob|armazen|reutiliz).*";
    private static final JavaClasses CODIGO_PRODUCAO = new ClassFileImporter()
            .withImportOption(new DoNotIncludeTests())
            .importPackages("br.gov.caixa.simtr.hub");

    static final ArchRule dominio_nao_deve_depender_de_bordas = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..recurso..",
                    "..integracao..",
                    "..adaptador..",
                    "..mapeamento..",
                    "..fachada..",
                    "..aplicacao..");

    static final ArchRule erro_rest_tecnico_pode_ser_usado_nas_bordas_permitidas = noClasses()
            .that().resideOutsideOfPackages(
                    "..recurso.rest..",
                    "..adaptador.entrada.rest..",
                    "..arquitetura.excecao..")
            .should().dependOnClassesThat()
            .resideInAPackage(PACOTE_DTO_ERRO_REST);

    static final ArchRule erro_rest_tecnico_nao_pode_vazar_para_o_nucleo = noClasses()
            .that().resideInAnyPackage("..dominio..", "..aplicacao..", "..adaptador.saida..")
            .should().dependOnClassesThat()
            .resideInAPackage(PACOTE_DTO_ERRO_REST);

    static final ArchRule dossie_produto_nao_deve_depender_de_outros_dominios_fora_de_acl =
            dependenciaEntreDominiosSomentePorAcl(
                    "dossieproduto", "arvoredocumento", "conformidade", "gestaodocumento");

    static final ArchRule arvore_documento_nao_deve_depender_de_outros_dominios =
            dependenciaEntreDominiosSomentePorAcl(
                    "arvoredocumento", "dossieproduto", "conformidade", "gestaodocumento");

    static final ArchRule conformidade_nao_deve_depender_de_outros_dominios =
            dependenciaEntreDominiosSomentePorAcl(
                    "conformidade", "dossieproduto", "arvoredocumento", "gestaodocumento");

    static final ArchRule gestao_documento_nao_deve_depender_de_outros_dominios_fora_de_acl =
            dependenciaEntreDominiosSomentePorAcl(
                    "gestaodocumento", "dossieproduto", "arvoredocumento", "conformidade");

    static final ArchRule aplicacao_migrada_nao_deve_depender_de_bordas = noClasses()
            .that().resideInAnyPackage(
                    "..dossieproduto.aplicacao..",
                    "..arvoredocumento.aplicacao..",
                    "..conformidade.aplicacao..",
                    "..gestaodocumento.aplicacao..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..integracao..",
                    "..adaptador..",
                    "..recurso..",
                    "..mapeamento..",
                    "..fachada..");

    static final ArchRule porta_de_entrada_nao_deve_expor_implementacao = noClasses()
            .that().resideInAnyPackage(
                    "..dossieproduto.aplicacao.porta.entrada..",
                    "..arvoredocumento.aplicacao.porta.entrada..",
                    "..conformidade.aplicacao.porta.entrada..",
                    "..gestaodocumento.aplicacao.porta.entrada..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..aplicacao.porta.saida..", "..aplicacao.casodeuso..");

    static final ArchRule adapters_rest_nao_devem_acessar_internos_ou_bordas_de_saida = noClasses()
            .that().resideInAnyPackage("..adaptador.entrada.rest..", "..recurso.rest..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..aplicacao.porta.saida..",
                    "..aplicacao.casodeuso..",
                    "..adaptador.saida..");

    static final ArchRule adapters_de_saida_migrados_nao_devem_reutilizar_contratos_de_outras_bordas = noClasses()
            .that().resideInAnyPackage(
                    "..dossieproduto.adaptador.saida..",
                    "..arvoredocumento.adaptador.saida..",
                    "..conformidade.adaptador.saida..",
                    "..gestaodocumento.adaptador.saida..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..recurso..", "..integracao..", "..arquitetura.excecao.dto..");

    static final ArchRule adapters_simulador_migrados_nao_devem_depender_da_borda_mtr = noClasses()
            .that().resideInAPackage("..adaptador.saida.simulador..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adaptador.saida.mtr..");

    static final ArchRule dto_rest_nao_deve_vazar_da_borda_de_entrada = noClasses()
            .that().resideOutsideOfPackages("..adaptador.entrada.rest..", "..recurso.rest..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adaptador.entrada.rest..dto..", "..recurso.rest..dto..");

    static final ArchRule dto_mtr_nao_deve_vazar_da_borda_mtr = noClasses()
            .that().resideOutsideOfPackage("..adaptador.saida.mtr..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adaptador.saida.mtr..dto..");

    static final ArchRule dto_simulador_nao_deve_vazar_da_borda_simulador = noClasses()
            .that().resideOutsideOfPackage("..adaptador.saida.simulador..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adaptador.saida.simulador..dto..");

    static final ArchRule rest_clients_devem_residir_na_borda_mtr = classes()
            .that().areAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage("..adaptador.saida.mtr.client..");

    static final ArchRule rest_clients_so_podem_ser_consumidos_pela_borda_mtr = noClasses()
            .that().resideOutsideOfPackage("..adaptador.saida.mtr..")
            .should().dependOnClassesThat()
            .areAnnotatedWith(RegisterRestClient.class);

    static final ArchRule acl_dossie_produto_so_deve_acessar_api_publica_de_outro_dominio =
            aclSoDeveAcessarApiPublica(
                    "dossieproduto", "arvoredocumento", "conformidade", "gestaodocumento");

    static final ArchRule acl_arvore_documento_so_deve_acessar_api_publica_de_outro_dominio =
            aclSoDeveAcessarApiPublica(
                    "arvoredocumento", "dossieproduto", "conformidade", "gestaodocumento");

    static final ArchRule acl_conformidade_so_deve_acessar_api_publica_de_outro_dominio =
            aclSoDeveAcessarApiPublica(
                    "conformidade", "dossieproduto", "arvoredocumento", "gestaodocumento");

    static final ArchRule acl_gestao_documento_so_deve_acessar_api_publica_de_outro_dominio =
            aclSoDeveAcessarApiPublica(
                    "gestaodocumento", "dossieproduto", "arvoredocumento", "conformidade");

    static final ArchRule nucleo_gestao_documento_nao_deve_depender_de_storage_ou_cache = noClasses()
            .that().resideInAnyPackage(PACOTES_NUCLEO_GESTAO_DOCUMENTO)
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "com.azure.storage..",
                    "io.quarkus.cache..",
                    "jakarta.cache..",
                    "javax.cache..",
                    "com.github.benmanes.caffeine..",
                    "org.infinispan..",
                    "redis.clients..");

    static final ArchRule nucleo_gestao_documento_nao_deve_declarar_tipos_fora_do_escopo = noClasses()
            .that().resideInAnyPackage(PACOTES_NUCLEO_GESTAO_DOCUMENTO)
            .should().haveNameMatching(PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO);

    static final ArchRule nucleo_gestao_documento_nao_deve_declarar_metodos_fora_do_escopo = noMethods()
            .that().areDeclaredInClassesThat().resideInAnyPackage(PACOTES_NUCLEO_GESTAO_DOCUMENTO)
            .should().haveNameMatching(PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO);

    static final ArchRule nucleo_gestao_documento_nao_deve_declarar_campos_fora_do_escopo = noFields()
            .that().areDeclaredInClassesThat().resideInAnyPackage(PACOTES_NUCLEO_GESTAO_DOCUMENTO)
            .should().haveNameMatching(PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO);

    private static ArchRule dependenciaEntreDominiosSomentePorAcl(
            String consumidor,
            String... fornecedores) {
        return noClasses()
                .that().resideInAPackage(".." + consumidor + "..")
                .and().resideOutsideOfPackage(".." + consumidor + ".adaptador.saida.acl..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(pacotesDosDominios(fornecedores));
    }

    private static ArchRule aclSoDeveAcessarApiPublica(
            String consumidor,
            String... fornecedores) {
        return noClasses()
                .that().resideInAPackage(".." + consumidor + ".adaptador.saida.acl..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(pacotesInternosDosDominios(fornecedores))
                .allowEmptyShould(true);
    }

    private static String[] pacotesDosDominios(String... dominios) {
        String[] pacotes = new String[dominios.length];
        for (int indice = 0; indice < dominios.length; indice++) {
            pacotes[indice] = ".." + dominios[indice] + "..";
        }
        return pacotes;
    }

    private static String[] pacotesInternosDosDominios(String... dominios) {
        String[] pacotes = new String[dominios.length * 4];
        for (int indice = 0; indice < dominios.length; indice++) {
            int inicio = indice * 4;
            String dominio = ".." + dominios[indice];
            pacotes[inicio] = dominio + ".aplicacao.casodeuso..";
            pacotes[inicio + 1] = dominio + ".aplicacao.porta.saida..";
            pacotes[inicio + 2] = dominio + ".adaptador..";
            pacotes[inicio + 3] = dominio + ".recurso..";
        }
        return pacotes;
    }

    @Test
    void dominioNaoDependeDeBordas() {
        dominio_nao_deve_depender_de_bordas.check(CODIGO_PRODUCAO);
    }

    @Test
    void erroRestTecnicoPermaneceNasBordasPermitidas() {
        erro_rest_tecnico_pode_ser_usado_nas_bordas_permitidas.check(CODIGO_PRODUCAO);
    }

    @Test
    void erroRestTecnicoNaoVazaParaONucleo() {
        erro_rest_tecnico_nao_pode_vazar_para_o_nucleo.check(CODIGO_PRODUCAO);
    }

    @Test
    void dependenciasEntreDominiosSoPodemPartirDeAcl() {
        dossie_produto_nao_deve_depender_de_outros_dominios_fora_de_acl.check(CODIGO_PRODUCAO);
        arvore_documento_nao_deve_depender_de_outros_dominios.check(CODIGO_PRODUCAO);
        conformidade_nao_deve_depender_de_outros_dominios.check(CODIGO_PRODUCAO);
        gestao_documento_nao_deve_depender_de_outros_dominios_fora_de_acl.check(CODIGO_PRODUCAO);
    }

    @Test
    void aclsSoPodemAcessarApiPublicaDeAplicacaoDeOutroDominio() {
        acl_dossie_produto_so_deve_acessar_api_publica_de_outro_dominio.check(CODIGO_PRODUCAO);
        acl_arvore_documento_so_deve_acessar_api_publica_de_outro_dominio.check(CODIGO_PRODUCAO);
        acl_conformidade_so_deve_acessar_api_publica_de_outro_dominio.check(CODIGO_PRODUCAO);
        acl_gestao_documento_so_deve_acessar_api_publica_de_outro_dominio.check(CODIGO_PRODUCAO);
    }

    @Test
    void arvoreDocumentoNaoDependeDeOutrosDominios() {
        arvore_documento_nao_deve_depender_de_outros_dominios.check(CODIGO_PRODUCAO);
    }

    @Test
    void conformidadeNaoDependeDeOutrosDominios() {
        conformidade_nao_deve_depender_de_outros_dominios.check(CODIGO_PRODUCAO);
    }

    @Test
    void aplicacaoMigradaNaoDependeDeBordas() {
        aplicacao_migrada_nao_deve_depender_de_bordas.check(CODIGO_PRODUCAO);
    }

    @Test
    void portaDeEntradaNaoExpoeImplementacao() {
        porta_de_entrada_nao_deve_expor_implementacao.check(CODIGO_PRODUCAO);
    }

    @Test
    void adaptersRestDependemSomenteDaApiPublicaDeEntradaEDosModelosInternos() {
        adapters_rest_nao_devem_acessar_internos_ou_bordas_de_saida.check(CODIGO_PRODUCAO);
    }

    @Test
    void adaptersDeSaidaMigradosNaoReutilizamContratosDeOutrasBordas() {
        adapters_de_saida_migrados_nao_devem_reutilizar_contratos_de_outras_bordas
                .check(CODIGO_PRODUCAO);
    }

    @Test
    void adaptersSimuladorMigradosNaoDependemDaBordaMtr() {
        adapters_simulador_migrados_nao_devem_depender_da_borda_mtr.check(CODIGO_PRODUCAO);
    }

    @Test
    void dtosPermanecemConfinadosAsRespectivasBordas() {
        dto_rest_nao_deve_vazar_da_borda_de_entrada.check(CODIGO_PRODUCAO);
        dto_mtr_nao_deve_vazar_da_borda_mtr.check(CODIGO_PRODUCAO);
        dto_simulador_nao_deve_vazar_da_borda_simulador.check(CODIGO_PRODUCAO);
    }

    @Test
    void restClientsPermanecemDeclaradosEConsumidosSomenteNaBordaMtr() {
        rest_clients_devem_residir_na_borda_mtr.check(CODIGO_PRODUCAO);
        rest_clients_so_podem_ser_consumidos_pela_borda_mtr.check(CODIGO_PRODUCAO);
    }

    @Test
    void nucleoGestaoDocumentoNaoImplementaStorageCacheRenovacaoOuUpload() {
        nucleo_gestao_documento_nao_deve_depender_de_storage_ou_cache.check(CODIGO_PRODUCAO);
        nucleo_gestao_documento_nao_deve_declarar_tipos_fora_do_escopo.check(CODIGO_PRODUCAO);
        nucleo_gestao_documento_nao_deve_declarar_metodos_fora_do_escopo.check(CODIGO_PRODUCAO);
        nucleo_gestao_documento_nao_deve_declarar_campos_fora_do_escopo.check(CODIGO_PRODUCAO);
    }

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
    void quarkusPodeSerUsadoNoDominio() {
        JavaClasses classes = new ClassFileImporter()
                .importClasses(UsoQuarkusNoDominioPermitido.class);

        assertDoesNotThrow(() -> dominio_nao_deve_depender_de_bordas.check(classes));
    }

    @Test
    void quarkusPodeSerUsadoNaAplicacao() {
        JavaClasses classes = new ClassFileImporter()
                .importClasses(UsoQuarkusNaAplicacaoPermitido.class);

        assertDoesNotThrow(() -> aplicacao_migrada_nao_deve_depender_de_bordas.check(classes));
    }

    @Test
    void regraDeIsolamentoEntreDominiosDetectaDependenciaForaDeAcl() {
        assertThrows(AssertionError.class, () ->
                arvore_documento_nao_deve_depender_de_outros_dominios.check(
                new ClassFileImporter().importClasses(
                        DependenciaDossieProdutoViolacao.class,
                        IdentificadorDossieProduto.class)));
    }

    @Test
    void regraDeIsolamentoDetectaConformidadeCompartilhandoModeloDeArvoreDocumento() {
        assertThrows(AssertionError.class, () ->
                conformidade_nao_deve_depender_de_outros_dominios.check(
                        new ClassFileImporter().importClasses(
                                DependenciaArvoreDocumentoViolacao.class,
                                ProcessoParametrizado.class)));
    }

    @Test
    void regraDeConformidadeDetectaDependenciaDeArvoreDocumento() {
        assertThrows(AssertionError.class, () ->
                conformidade_nao_deve_depender_de_outros_dominios.check(
                        new ClassFileImporter().importClasses(
                                DependenciaArvoreDocumentoViolacao.class,
                                ProcessoParametrizado.class)));
    }

    @Test
    void regraDeArvoreDocumentoDetectaDependenciaDeOutroDominio() {
        assertThrows(AssertionError.class, () ->
                arvore_documento_nao_deve_depender_de_outros_dominios.check(
                        new ClassFileImporter().importClasses(
                                DependenciaDossieProdutoViolacao.class,
                                IdentificadorDossieProduto.class)));
    }

    @Test
    void regrasDeCamadaDetectamDependenciaDeAdapterNoDominioENaAplicacao() {
        assertThrows(AssertionError.class, () -> dominio_nao_deve_depender_de_bordas.check(
                new ClassFileImporter().importClasses(DependenciaAdapterNoDominioViolacao.class)));
        assertThrows(AssertionError.class, () -> aplicacao_migrada_nao_deve_depender_de_bordas.check(
                new ClassFileImporter().importClasses(DependenciaAdapterNaAplicacaoViolacao.class)));
    }

    @Test
    void regraDePortaDeEntradaDetectaExposicaoDaPortaDeSaida() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..aplicacao.porta.saida..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(PortaEntradaComSaidaViolacao.class)));
    }

    @Test
    void regraFinalDeCamadasDetectaAdapterRestAcessandoPortaDeSaida() {
        assertThrows(AssertionError.class, () ->
                adapters_rest_nao_devem_acessar_internos_ou_bordas_de_saida.check(
                        new ClassFileImporter().importClasses(AdapterEntradaComPortaSaidaViolacao.class)));
    }

    @Test
    void regraDeAdapterMtrDetectaReusoDeDtoPublico() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..recurso.rest..dto..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(AdapterMtrComDtoPublicoViolacao.class)));
    }

    @Test
    void regraDeAdapterSimuladorDetectaReusoDeDtoMtr() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..adaptador.saida.mtr..dto..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(AdapterSimuladorComDtoMtrViolacao.class)));
    }

    @Test
    void regrasFinaisDeDtoPorBordaPossuemProvasNegativas() {
        assertThrows(AssertionError.class, () ->
                dto_rest_nao_deve_vazar_da_borda_de_entrada.check(
                        new ClassFileImporter().importClasses(AdapterMtrComDtoPublicoViolacao.class)));
        assertThrows(AssertionError.class, () ->
                dto_mtr_nao_deve_vazar_da_borda_mtr.check(
                        new ClassFileImporter().importClasses(AdapterSimuladorComDtoMtrViolacao.class)));
        assertThrows(AssertionError.class, () ->
                dto_simulador_nao_deve_vazar_da_borda_simulador.check(
                        new ClassFileImporter().importClasses(AdapterMtrComDtoSimuladorViolacao.class)));
    }

    @Test
    void regraFinalDeRestClientDetectaDeclaracaoForaDaBordaMtr() {
        JavaClasses classes = new ClassFileImporter().importClasses(
                RestClientForaDaBordaMtrViolacao.class,
                ConsumidorRestClientForaDaBordaMtrViolacao.class);

        assertThrows(AssertionError.class, () ->
                rest_clients_devem_residir_na_borda_mtr.check(classes));
        assertThrows(AssertionError.class, () ->
                rest_clients_so_podem_ser_consumidos_pela_borda_mtr.check(classes));
    }

    @Test
    void regraFinalDeApiPublicaDetectaAclAcessandoCasoDeUsoDeOutroDominio() {
        assertThrows(AssertionError.class, () ->
                acl_arvore_documento_so_deve_acessar_api_publica_de_outro_dominio.check(
                        new ClassFileImporter().importClasses(AcessoInternoDossieProdutoViolacao.class)));
    }

    @Test
    void regraDeEscopoGestaoDocumentoDetectaAzureBlobNoNucleo() {
        ArchRule regra = noClasses().should().dependOnClassesThat()
                .resideInAPackage("com.azure.storage..");

        assertThrows(AssertionError.class, () -> regra.check(
                new ClassFileImporter().importClasses(
                        NucleoComAzureBlobViolacao.class,
                        BlobClientViolacao.class)));
    }

    @Test
    void regraDeEscopoGestaoDocumentoDetectaCacheRenovacaoEUpload() {
        ArchRule regraDeTipo = noClasses().should()
                .haveNameMatching(PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO);
        ArchRule regraDeMetodo = noMethods().should()
                .haveNameMatching(PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO);
        ArchRule regraDeCampo = noFields().should()
                .haveNameMatching(PADRAO_OPERACAO_FORA_ESCOPO_GESTAO_DOCUMENTO);
        JavaClasses classes = new ClassFileImporter().importClasses(
                CacheCredencialViolacao.class,
                OperacoesCredencialViolacao.class);

        assertThrows(AssertionError.class, () -> regraDeTipo.check(classes));
        assertThrows(AssertionError.class, () -> regraDeMetodo.check(classes));
        assertThrows(AssertionError.class, () -> regraDeCampo.check(classes));
    }

    private static final class PortaEntradaComSaidaViolacao {
        private br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto portaSaida;
    }

    private static final class AdapterMtrComDtoPublicoViolacao {
        private br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto dto;
    }

    private static final class AdapterSimuladorComDtoMtrViolacao {
        private br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.dto.v2.processo
                .ProcessoParametrizadoMtrResponse dto;
    }

    private static final class AdapterMtrComDtoSimuladorViolacao {
        private br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.dto
                .GestaoDocumentoSimuladorResponse dto;
    }

    @RegisterRestClient
    private interface RestClientForaDaBordaMtrViolacao {
    }

    private static final class ConsumidorRestClientForaDaBordaMtrViolacao {
        private RestClientForaDaBordaMtrViolacao client;
    }

    private static final class NucleoComAzureBlobViolacao {
        private BlobClientViolacao blobClient;
    }

    private static final class CacheCredencialViolacao {
    }

    private static final class OperacoesCredencialViolacao {
        private Object blobClient;

        void renovarSas() {
        }

        void uploadArquivo() {
        }
    }
}

package br.gov.caixa.simtr.arquitetura.guardrails;

import br.gov.caixa.simtr.monitoramento.aplicacao.falso.DependenciaSdkViolacao;
import br.gov.caixa.simtr.monitoramento.aplicacao.falso.QuarkusReativoPermitido;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstruturaMonitoramentoArchUnitTest {

    private static final String ORQUESTRADOR = "br.gov.caixa.simtr.orquestrador..";
    private static final String MONITORAMENTO = "br.gov.caixa.simtr.monitoramento..";
    private static final String INFRAESTRUTURA = "br.gov.caixa.simtr.arquitetura.infraestrutura..";
    private static final String HUB = "br.gov.caixa.simtr.hub..";
    private static final String SDK_AZURE = "com.azure..";

    private static final JavaClasses PRODUCAO = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.gov.caixa.simtr.orquestrador",
                    "br.gov.caixa.simtr.monitoramento", "br.gov.caixa.simtr.arquitetura");

    private static final ArchRule NUCLEO_SEM_BORDA = noClasses()
            .that().resideInAnyPackage("..dominio..", "..aplicacao..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adaptador..", INFRAESTRUTURA, SDK_AZURE);

    @Test
    void nucleoNaoDependeDeTransporteOuInfraestrutura() {
        NUCLEO_SEM_BORDA.check(PRODUCAO);
    }

    @Test
    void componentesColaboramSomentePelasAclsPlanejadas() {
        noClasses().that().resideInAPackage(ORQUESTRADOR)
                .and().resideOutsideOfPackage("..adaptador.saida.acl.monitoramento..")
                .should().dependOnClassesThat().resideInAnyPackage(MONITORAMENTO, HUB)
                .check(PRODUCAO);
        noClasses().that().resideInAPackage(MONITORAMENTO)
                .and().resideOutsideOfPackage("..adaptador.saida.acl.simtrhub..")
                .should().dependOnClassesThat().resideInAnyPackage(ORQUESTRADOR, HUB)
                .check(PRODUCAO);
    }

    @Test
    void infraestruturaNaoDependeDosComponentesDeNegocio() {
        noClasses().that().resideInAPackage(INFRAESTRUTURA)
                .should().dependOnClassesThat().resideInAnyPackage(ORQUESTRADOR, MONITORAMENTO, HUB)
                .check(PRODUCAO);
    }

    @Test
    void sdkPermaneceNasBordasServiceBusEFabricaTecnica() {
        noClasses().that().resideOutsideOfPackages("..adaptador.entrada.servicebus..",
                        "..adaptador.saida.servicebus..",
                        "br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus..")
                .should().dependOnClassesThat().resideInAPackage(SDK_AZURE)
                .check(PRODUCAO);
    }

    @Test
    void portasDeclaramContratosSemImplementacao() {
        classes().that().resideInAnyPackage("..aplicacao.porta.entrada..", "..aplicacao.porta.saida..")
                .should().beInterfaces().check(PRODUCAO);
    }

    @Test
    void guardrailRejeitaSdkNoNucleo() {
        JavaClasses violacao = new ClassFileImporter().importClasses(DependenciaSdkViolacao.class);
        assertThrows(AssertionError.class, () -> NUCLEO_SEM_BORDA.check(violacao));
    }

    @Test
    void guardrailPermiteQuarkusMutinyECdiNoNucleo() {
        JavaClasses permitido = new ClassFileImporter().importClasses(QuarkusReativoPermitido.class);
        assertDoesNotThrow(() -> NUCLEO_SEM_BORDA.check(permitido));
    }

    private static final ArchRule BUILDER_EXCLUSIVO = noClasses()
            .that().resideOutsideOfPackage("br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus..")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.azure.messaging.servicebus.ServiceBusClientBuilder");

    private static final ArchRule FABRICA_SOMENTE_SERVICE_BUS = noClasses()
            .that().resideOutsideOfPackages("..adaptador.entrada.servicebus..", "..adaptador.saida.servicebus..",
                    "br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus..")
            .should().dependOnClassesThat().resideInAPackage("br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus..");

    @Test
    void somenteFabricaConfiguraBuilderESomenteBordasServiceBusAcessamClientes() {
        BUILDER_EXCLUSIVO.check(PRODUCAO);
        FABRICA_SOMENTE_SERVICE_BUS.check(PRODUCAO);
    }

    @Test
    void deveRejeitarBuilderForaDaFabrica() {
        var classes = new ClassFileImporter().importClasses(
                br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.falso.AcessosServiceBusProibidos.BuilderForaFabrica.class);
        assertThrows(AssertionError.class, () -> BUILDER_EXCLUSIVO.check(classes));
    }

    @Test
    void deveRejeitarFabricaForaDasBordasServiceBus() {
        var classes = new ClassFileImporter().importClasses(
                br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.falso.AcessosServiceBusProibidos.FabricaForaBordaServiceBus.class);
        assertThrows(AssertionError.class, () -> FABRICA_SOMENTE_SERVICE_BUS.check(classes));
    }
}

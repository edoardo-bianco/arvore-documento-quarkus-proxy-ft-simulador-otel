package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.ConsultaDocumentosDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDocumentosDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ConsultaDocumentosDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsultaDocumentosDossieProdutoSimuladorAdapterTest {

    private static final long IDENTIFICADOR = 4_081_899L;
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/4081899-v4-consulta-documentos-dossie-produto.md";

    @Inject
    @ConsultaDocumentosDossieProdutoSimulador
    ConsultaDocumentosDossieProdutoSimuladorAdapter adapter;

    @Test
    void identificaOsDoisAdaptersComQualifiersExclusivosESemAmbiguidade() {
        var tipoSimulador = ConsultaDocumentosDossieProdutoSimuladorAdapter.class;
        var tipoMtr = ConsultaDocumentosDossieProdutoMtrAdapter.class;

        assertTrue(ObterDocumentosDossieProduto.class.isAssignableFrom(tipoSimulador));
        assertNotNull(tipoSimulador.getAnnotation(ApplicationScoped.class));
        assertNotNull(tipoSimulador.getAnnotation(
                ConsultaDocumentosDossieProdutoSimulador.class));
        assertNull(tipoSimulador.getAnnotation(ConsultaDocumentosDossieProdutoMtr.class));
        assertNotNull(tipoMtr.getAnnotation(ConsultaDocumentosDossieProdutoMtr.class));
        assertNull(tipoMtr.getAnnotation(ConsultaDocumentosDossieProdutoSimulador.class));
        assertQualifier(ConsultaDocumentosDossieProdutoMtr.class);
        assertQualifier(ConsultaDocumentosDossieProdutoSimulador.class);
    }

    @Test
    void obtemOsQuatorzeDocumentosDaFixturePeloIdIgnorandoFiltrosSemChamarRede() {
        var documentos = adapter.obter(criteriosComFiltrosSentinela()).await().indefinitely();

        assertEquals(14, documentos.size());
        assertEquals(1132220L, documentos.getFirst().idInstanciaDocumento());
        assertEquals("GED-SIMULADO-0001", documentos.getFirst().codigoGed());
        assertEquals("00000000000", documentos.getFirst().vinculoDossie().cliente().cpf());
        assertEquals("CLIENTE SIMULADO 01",
                documentos.getFirst().vinculoDossie().cliente().nome());
        assertEquals(1132177L, documentos.getLast().idInstanciaDocumento());
    }

    @Test
    void usaSomenteReaderEMapperLocaisEPreservaAListaMapeada() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(ConsultaDocumentosDossieProdutoSimuladorMapper.class);
        var resposta = mock(ConsultaDocumentosDossieProdutoSimuladorResponse.class);
        var esperado = List.of(mock(DocumentoDossieProdutoConsultado.class));
        when(reader.readFirstJsonObject(
                MOCK_RESOURCE,
                ConsultaDocumentosDossieProdutoSimuladorResponse.class)).thenReturn(resposta);
        when(mapper.paraDominio(resposta)).thenReturn(esperado);
        var adapterLocal = new ConsultaDocumentosDossieProdutoSimuladorAdapter(reader, mapper);

        var resultado = adapterLocal.obter(criteriosComFiltrosSentinela())
                .await().indefinitely();

        assertSame(esperado, resultado);
        verify(reader).readFirstJsonObject(
                MOCK_RESOURCE,
                ConsultaDocumentosDossieProdutoSimuladorResponse.class);
        verify(mapper).paraDominio(resposta);
        verifyNoMoreInteractions(reader, mapper);
    }

    @Test
    void retornaFalhaDeNegocio404QuandoFixtureDoIdentificadorNaoExiste() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(ConsultaDocumentosDossieProdutoSimuladorMapper.class);
        var recursoAusente =
                "mock/dossieproduto/9999999-v4-consulta-documentos-dossie-produto.md";
        when(reader.readFirstJsonObject(
                recursoAusente,
                ConsultaDocumentosDossieProdutoSimuladorResponse.class)).thenReturn(null);
        var adapterLocal = new ConsultaDocumentosDossieProdutoSimuladorAdapter(reader, mapper);

        var falha = executarFalha(adapterLocal, criterios(9_999_999L));

        assertNaoEncontrado(falha, 9_999_999L);
        verify(reader).readFirstJsonObject(
                recursoAusente,
                ConsultaDocumentosDossieProdutoSimuladorResponse.class);
        verifyNoMoreInteractions(reader);
        verifyNoInteractions(mapper);
    }

    @Test
    void rejeitaCriteriosOuIdentificadorAusentesSemTentarResolverFixture() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(ConsultaDocumentosDossieProdutoSimuladorMapper.class);
        var adapterLocal = new ConsultaDocumentosDossieProdutoSimuladorAdapter(reader, mapper);

        var falhaSemCriterios = executarFalha(adapterLocal, null);
        var falhaSemIdentificador = executarFalha(
                adapterLocal,
                new CriteriosConsultaDocumentosDossieProduto(
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null));

        assertNaoEncontrado(falhaSemCriterios, null);
        assertNaoEncontrado(falhaSemIdentificador, null);
        verifyNoInteractions(reader, mapper);
    }

    @Test
    void naoAplicaFaultToleranceNemCriaSpanClientNoAdapterSimulado()
            throws NoSuchMethodException {
        var tipo = ConsultaDocumentosDossieProdutoSimuladorAdapter.class;
        var metodo = tipo.getMethod(
                "obter", CriteriosConsultaDocumentosDossieProduto.class);
        var construtor = tipo.getConstructor(
                MarkdownJsonMockReader.class,
                ConsultaDocumentosDossieProdutoSimuladorMapper.class);

        assertFalse(tipo.isAnnotationPresent(Timeout.class));
        assertFalse(tipo.isAnnotationPresent(Retry.class));
        assertFalse(tipo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(tipo.isAnnotationPresent(WithSpan.class));
        assertFalse(metodo.isAnnotationPresent(Timeout.class));
        assertFalse(metodo.isAnnotationPresent(Retry.class));
        assertFalse(metodo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(metodo.isAnnotationPresent(WithSpan.class));
        assertEquals(List.of(
                MarkdownJsonMockReader.class,
                ConsultaDocumentosDossieProdutoSimuladorMapper.class),
                Arrays.asList(construtor.getParameterTypes()));
    }

    private static CriteriosConsultaDocumentosDossieProduto criteriosComFiltrosSentinela() {
        return new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(IDENTIFICADOR),
                "CNPJ_NAO_USADO",
                "CPF_NAO_USADO",
                99L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                "IP_NAO_USADO",
                "TIPOLOGIA_NAO_USADA");
    }

    private static CriteriosConsultaDocumentosDossieProduto criterios(Long identificador) {
        return new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(identificador),
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static FalhaConsultaDocumentosDossieProduto executarFalha(
            ConsultaDocumentosDossieProdutoSimuladorAdapter adapterLocal,
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        var espera = adapterLocal.obter(criterios).await();
        return assertThrows(
                FalhaConsultaDocumentosDossieProduto.class,
                espera::indefinitely);
    }

    private static void assertNaoEncontrado(
            FalhaConsultaDocumentosDossieProduto falha,
            Long identificador
    ) {
        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-dossie-produto", falha.recurso());
        assertEquals("mock-consulta-documentos-dossie-produto-" + identificador,
                falha.idErro());
        assertEquals("DOSSIE_PRODUTO_NAO_ENCONTRADO", falha.codigoErro());
        assertEquals("Dossie produto " + identificador + " nao encontrado no simulador.",
                falha.mensagens().getFirst());
        assertNull(falha.detalhe());
        assertNull(falha.stacktraceExterno());
    }

    private static void assertQualifier(Class<?> qualifier) {
        assertNotNull(qualifier.getAnnotation(Qualifier.class));
        assertEquals(RUNTIME, qualifier.getAnnotation(Retention.class).value());
        assertEquals(Set.of(TYPE, FIELD, PARAMETER, METHOD),
                Set.of(qualifier.getAnnotation(Target.class).value()));
    }
}

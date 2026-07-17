package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.ClientErrorBodyReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ValidacaoNegocialDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ValidacaoNegocialDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.client.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrErrorType;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ExceptionMapperTest {

    private static final String RECURSO_SIMTR_PARAMETRIZACAO = "simtr-parametrizacao";
    private static final String RECURSO_SIMTR_DOSSIE_PRODUTO = "simtr-dossie-produto";

    @Test
    void mtrRestClientExceptionMapperPreservaPayloadDeErroDeNegocio() {
        ErroPadraoDto erro = erro(400, RECURSO_SIMTR_PARAMETRIZACAO, "MTR400", "erro de negocio");
        MtrBusinessErrorException exception = new MtrBusinessErrorException(400, erro);

        Response response = new MtrRestClientExceptionMapper().toResponse(exception);

        assertEquals(400, response.getStatus());
        assertSame(erro, response.getEntity());
        assertEquals(MtrErrorType.NEGOCIO, exception.tipoErro());
        assertEquals("erro de negocio", exception.getMessage());
    }

    @Test
    void mtrRestClientExceptionMapperPreservaPayloadTecnicoMesmoIncompleto() {
        MtrServerErrorException exception = new MtrServerErrorException(502, null);

        Response response = new MtrRestClientExceptionMapper().toResponse(exception);

        assertEquals(502, response.getStatus());
        assertNull(response.getEntity());
        assertEquals(MtrErrorType.TECNICO_SERVIDOR, exception.tipoErro());
        assertEquals("Erro retornado pelo servico MTR", semAcento(exception.getMessage()));
    }

    @Test
    void genericExceptionMapperPreservaWebApplicationExceptionERetorna500ParaErroNaoTratado() {
        Response webResponse = Response.status(418).entity("teapot").build();
        GenericExceptionMapper mapper = new GenericExceptionMapper();

        assertSame(webResponse, mapper.toResponse(new WebApplicationException(webResponse)));

        Response response = mapper.toResponse(new RuntimeException("falha inesperada"));

        assertEquals(500, response.getStatus());
        ErroPadraoDto erro = (ErroPadraoDto) response.getEntity();
        assertEquals("ARVDOCP9999", erro.codigoErro());
        assertEquals("simtr-hub", erro.recurso());
    }

    @Test
    void constraintViolationExceptionMapperRetorna400ComMensagens() {
        Set<ConstraintViolation<EntradaValidada>> violacoes = Validation
                .buildDefaultValidatorFactory()
                .getValidator()
                .validate(new EntradaValidada(null));

        Response response = new ConstraintViolationExceptionMapper()
                .toResponse(new ConstraintViolationException(violacoes));

        assertEquals(400, response.getStatus());
        ErroPadraoDto erro = (ErroPadraoDto) response.getEntity();
        assertEquals("ARVDOCP0001", erro.codigoErro());
        assertEquals("valor obrigatorio", erro.erros().getFirst().mensagem());
    }

    @Test
    void clientErrorBodyReaderNormalizaPayloadMtrIncompleto() {
        Response response = responseMock(409, erro(null, null, "MTR409", "conflito"));

        ErroPadraoDto erro = ClientErrorBodyReader.read(response, RECURSO_SIMTR_DOSSIE_PRODUTO);

        assertEquals(409, erro.codigoHttp());
        assertEquals(RECURSO_SIMTR_DOSSIE_PRODUTO, erro.recurso());
        assertNotNull(erro.idErro());
        assertEquals("MTR409", erro.codigoErro());
        assertEquals("conflito", erro.erros().getFirst().mensagem());
    }

    @Test
    void clientErrorBodyReaderRetornaErroPadraoQuandoPayloadNaoPodeSerLido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(502);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ErroPadraoDto.class)).thenThrow(new IllegalStateException("payload invalido"));

        ErroPadraoDto erro = ClientErrorBodyReader.read(response, RECURSO_SIMTR_PARAMETRIZACAO);

        assertEquals(502, erro.codigoHttp());
        assertEquals(RECURSO_SIMTR_PARAMETRIZACAO, erro.recurso());
        assertEquals("ARVDOCP0002", erro.codigoErro());
    }

    @Test
    void clientExceptionMappersClassificamStatusHttp() {
        assertNull(ValidacaoNegocialDossieProdutoMtrClient.toException(statusSemPayload(200)));

        RuntimeException negocio = ValidacaoNegocialDossieProdutoMtrClient.toException(statusSemPayload(400));
        RuntimeException tecnicoCliente = ParametrizacaoChecklistClient.toException(statusSemPayload(401));
        RuntimeException tecnicoServidor = ParametrizacaoChecklistClient.toException(statusSemPayload(500));

        assertInstanceOf(ValidacaoNegocialDossieProdutoMtrException.Negocio.class, negocio);
        assertInstanceOf(ChecklistMtrException.TecnicaCliente.class, tecnicoCliente);
        assertInstanceOf(ChecklistMtrException.Servidor.class, tecnicoServidor);
        assertEquals(RECURSO_SIMTR_DOSSIE_PRODUTO,
                ((ValidacaoNegocialDossieProdutoMtrException.Negocio) negocio).erro().recurso());
        assertEquals(RECURSO_SIMTR_PARAMETRIZACAO,
                ((ChecklistMtrException.TecnicaCliente) tecnicoCliente).erro().recurso());
    }

    @Test
    void mtrExceptionTypesExponhemCodigosEsperados() {
        assertEquals("negocio", MtrErrorType.NEGOCIO.codigo());
        assertEquals("tecnico_cliente", MtrErrorType.TECNICO_CLIENTE.codigo());
        assertEquals("tecnico_servidor", MtrErrorType.TECNICO_SERVIDOR.codigo());
        assertEquals(MtrErrorType.TECNICO_CLIENTE,
                new MtrClientTechnicalException(429, null).tipoErro());
    }

    private static ErroPadraoDto erro(Integer status, String recurso, String codigo, String mensagem) {
        return new ErroPadraoDto(
                status,
                recurso,
                "erro-1",
                codigo,
                List.of(new ErroMensagemDto(mensagem)),
                "detalhe",
                null
        );
    }

    private static Response responseMock(int status, ErroPadraoDto erro) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ErroPadraoDto.class)).thenReturn(erro);
        return response;
    }

    private static Response statusSemPayload(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }

    private static String semAcento(String valor) {
        return valor.replace("ç", "c");
    }

    private static class EntradaValidada {
        @NotNull(message = "valor obrigatorio")
        private final String valor;

        private EntradaValidada(String valor) {
            this.valor = valor;
        }
    }
}

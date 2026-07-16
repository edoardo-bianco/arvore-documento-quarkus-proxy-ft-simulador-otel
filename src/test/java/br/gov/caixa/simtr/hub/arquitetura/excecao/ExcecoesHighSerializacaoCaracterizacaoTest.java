package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.DocumentoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.erro.ProcessoParametrizadoMtrException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.erro.GestaoDocumentoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CriacaoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.FormularioDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ValidacaoNegocialDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.WorkflowDossieProdutoMtrException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExcecoesHighSerializacaoCaracterizacaoTest {

    @Test
    void familiaGestaoDocumentoPreservaPayloadNoRoundTrip() throws Exception {
        var erro = new GestaoDocumentoMtrException.Erro(
                409, "credencial", "id-2", "codigo-2",
                List.of(new GestaoDocumentoMtrException.Mensagem("conflito")),
                "detalhe", "stacktrace");
        var excecao = new GestaoDocumentoMtrException.Negocio(409, erro);

        var restaurada = (GestaoDocumentoMtrException.Negocio) desserializar(serializar(excecao));
        assertEquals(excecao.getMessage(), restaurada.getMessage());
        assertEquals(excecao.erro(), restaurada.erro());
    }

    @Test
    void familiasRestantesAindaPossuemPayloadNaoSerializavel() throws Exception {
        for (Class<?> tipo : List.of(
                ChecklistMtrException.class,
                ProcessoParametrizadoMtrException.class,
                ValidacaoNegocialDossieProdutoMtrException.class,
                FormularioDossieProdutoMtrException.class,
                CriacaoDossieProdutoMtrException.class,
                WorkflowDossieProdutoMtrException.class)) {
            var campoErro = tipo.getDeclaredField("erro");
            assertFalse(Serializable.class.isAssignableFrom(campoErro.getType()), tipo.getName());
        }
    }

    @Test
    void excecoesMtrNaoSaoSerializaveisPorJavaEDevemSerTratadasComoContratoJson()
            throws Exception {
        var erro = new DocumentoDossieProdutoMtrException.Erro(
                422, "documento", "id-1", "codigo-1",
                List.of(new DocumentoDossieProdutoMtrException.Mensagem("falha")),
                "detalhe", "stacktrace");
        var excecao = new DocumentoDossieProdutoMtrException.Negocio(422, erro);

        assertEquals("falha", excecao.getMessage());
        var restaurada = desserializar(serializar(excecao));
        var roundTrip = (DocumentoDossieProdutoMtrException.Negocio) restaurada;
        assertEquals(excecao.getMessage(), roundTrip.getMessage());
        assertEquals(excecao.erro(), roundTrip.erro());
    }

    @Test
    void dtoTecnicoMantemPayloadEContinuaSendoObjetoDeTransporte() {
        var dto = new ErroPadraoDto(422, "documento", "id-1", "codigo-1",
                List.of(new ErroMensagemDto("falha")), "detalhe", "stacktrace");

        assertEquals(422, dto.codigoHttp());
        assertEquals("falha", dto.erros().getFirst().mensagem());
        assertEquals("stacktrace", dto.stacktrace());
    }

    private static byte[] serializar(Object valor) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(valor);
        }
        return bytes.toByteArray();
    }

    private static Object desserializar(byte[] bytes) throws Exception {
        try (var input = new ObjectInputStream(new java.io.ByteArrayInputStream(bytes))) {
            return input.readObject();
        }
    }
}

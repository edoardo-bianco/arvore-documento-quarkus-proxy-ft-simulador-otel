package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.DocumentoDossieProdutoMtrException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcecoesHighSerializacaoCaracterizacaoTest {

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

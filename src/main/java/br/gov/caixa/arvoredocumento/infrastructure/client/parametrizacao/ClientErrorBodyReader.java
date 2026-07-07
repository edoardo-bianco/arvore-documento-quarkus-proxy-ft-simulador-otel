package br.gov.caixa.arvoredocumento.infrastructure.client.parametrizacao;

import br.gov.caixa.arvoredocumento.api.dto.erro.ErroMensagemDto;
import br.gov.caixa.arvoredocumento.api.dto.erro.ErroPadraoDto;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

final class ClientErrorBodyReader {

    private ClientErrorBodyReader() {
    }

    static ErroPadraoDto read(Response response, String recurso) {
        try {
            if (response.hasEntity()) {
                ErroPadraoDto erro = response.readEntity(ErroPadraoDto.class);
                if (erro != null) {
                    return normalize(erro, response.getStatus(), recurso);
                }
            }
        } catch (RuntimeException ignored) {
            // O corpo de erro veio fora do contrato esperado. A API externa ainda deve responder no padrão definido.
        }

        return new ErroPadraoDto(
                response.getStatus(),
                recurso,
                UUID.randomUUID().toString(),
                "ARVDOCP0002",
                List.of(new ErroMensagemDto("Erro retornado pelo serviço MTR fora do contrato esperado.")),
                null,
                null
        );
    }

    private static ErroPadraoDto normalize(ErroPadraoDto erro, int status, String recurso) {
        return new ErroPadraoDto(
                erro.codigoHttp() != null ? erro.codigoHttp() : status,
                erro.recurso() != null ? erro.recurso() : recurso,
                erro.idErro() != null ? erro.idErro() : UUID.randomUUID().toString(),
                erro.codigoErro(),
                erro.erros(),
                erro.detalhe(),
                erro.stacktrace()
        );
    }
}

package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrErrorType;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrRestClientException;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class MtrRestClientExceptionMapper implements ExceptionMapper<MtrRestClientException> {

    private static final Logger LOG = Logger.getLogger(MtrRestClientExceptionMapper.class);

    @Override
    public Response toResponse(MtrRestClientException exception) {
        registrarErroMtr(exception);

        return Response.status(exception.status())
                .entity(exception.erro())
                .build();
    }

    private static void registrarErroMtr(MtrRestClientException exception) {
        ErroPadraoDto erro = exception.erro();

        Map<String, Object> campos = ObservabilityLog.fields(
                "camada", "api",
                "componente", "MtrRestClientExceptionMapper",
                "dependencia", erro != null && erro.recurso() != null ? erro.recurso() : "mtr",
                "status_http", exception.status(),
                "tipo_erro_mtr", exception.tipoErro().codigo(),
                "classe_exception", exception.getClass().getName(),
                "erro_body_preservado", erro != null,
                "erro_body_incompleto", erroPayloadIncompleto(erro),
                "codigo_http_mtr", erro != null ? erro.codigoHttp() : null,
                "recurso", erro != null ? erro.recurso() : null,
                "id_erro", erro != null ? erro.idErro() : null,
                "codigo_erro", erro != null ? erro.codigoErro() : null,
                "detalhe", erro != null ? erro.detalhe() : null,
                "erros", mensagensErro(erro),
                "resultado", "erro"
        );

        if (exception.tipoErro() == MtrErrorType.NEGOCIO) {
            ObservabilityLog.info(LOG, "mtr.erro.negocio.retornado", campos);
            return;
        }

        ObservabilityLog.error(LOG, "mtr.erro.tecnico.retornado", exception, campos);
    }

    private static String mensagensErro(ErroPadraoDto erro) {
        if (erro == null || erro.erros() == null || erro.erros().isEmpty()) {
            return null;
        }
        return erro.erros().stream()
                .filter(mensagem -> mensagem != null && mensagem.mensagem() != null)
                .map(mensagem -> mensagem.mensagem())
                .collect(Collectors.joining(" | "));
    }

    private static boolean erroPayloadIncompleto(ErroPadraoDto erro) {
        if (erro == null) {
            return true;
        }
        return isBlank(erro.codigoErro())
                && (erro.erros() == null || erro.erros().isEmpty())
                && isBlank(erro.detalhe());
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}

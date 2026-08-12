package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

public abstract class ConsultaDossieProdutoMtrException extends RuntimeException {

    private final int status;
    private final Erro erro;

    protected ConsultaDossieProdutoMtrException(int status, Erro erro) {
        super(mensagem(erro));
        this.status = status;
        this.erro = erro;
    }

    public int status() {
        return status;
    }

    public Erro erro() {
        return erro;
    }

    public static final class Negocio extends ConsultaDossieProdutoMtrException {
        public Negocio(int status, Erro erro) {
            super(status, erro);
        }
    }

    public static final class TecnicaCliente extends ConsultaDossieProdutoMtrException {
        public TecnicaCliente(int status, Erro erro) {
            super(status, erro);
        }
    }

    public static final class Servidor extends ConsultaDossieProdutoMtrException {
        public Servidor(int status, Erro erro) {
            super(status, erro);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Erro(
            @JsonProperty("codigo_http") Integer codigoHttp,
            @JsonProperty("recurso") String recurso,
            @JsonProperty("id_erro") String idErro,
            @JsonProperty("codigo_erro") String codigoErro,
            @JsonProperty("erros") List<Mensagem> erros,
            @JsonProperty("detalhe") String detalhe,
            @JsonProperty("stacktrace") String stacktrace
    ) implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Mensagem(
            @JsonProperty("mensagem") String mensagem
    ) implements Serializable {
    }

    private static String mensagem(Erro erro) {
        if (erro == null || erro.erros() == null || erro.erros().isEmpty()
                || erro.erros().getFirst() == null) {
            return "Erro retornado pelo servico MTR";
        }
        return erro.erros().getFirst().mensagem();
    }
}

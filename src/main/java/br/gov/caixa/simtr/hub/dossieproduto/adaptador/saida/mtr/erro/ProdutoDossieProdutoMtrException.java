package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ProdutoDossieProdutoMtrException extends RuntimeException {

    private final int status;
    private final Erro erro;

    protected ProdutoDossieProdutoMtrException(int status, Erro erro) {
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

    public static final class Negocio extends ProdutoDossieProdutoMtrException {
        public Negocio(int status, Erro erro) {
            super(status, erro);
        }
    }

    public static final class TecnicaCliente extends ProdutoDossieProdutoMtrException {
        public TecnicaCliente(int status, Erro erro) {
            super(status, erro);
        }
    }

    public static final class Servidor extends ProdutoDossieProdutoMtrException {
        public Servidor(int status, Erro erro) {
            super(status, erro);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Erro(
            @JsonProperty("codigo_http") Integer codigoHttp,
            String recurso,
            @JsonProperty("id_erro") String idErro,
            @JsonProperty("codigo_erro") String codigoErro,
            List<Mensagem> erros,
            String detalhe,
            String stacktrace
    ) implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Mensagem(String mensagem) implements Serializable {
    }

    private static String mensagem(Erro erro) {
        if (erro == null || erro.erros() == null || erro.erros().isEmpty()
                || erro.erros().getFirst() == null) {
            return "Erro retornado pelo servico MTR";
        }
        return erro.erros().getFirst().mensagem();
    }
}

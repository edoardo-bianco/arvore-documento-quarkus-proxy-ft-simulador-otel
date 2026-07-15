package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import java.util.List;

public final class FalhaCriacaoDossieProduto extends RuntimeException {

    public enum Tipo {
        NEGOCIO,
        TECNICA_CLIENTE,
        DEPENDENCIA_INDISPONIVEL,
        TIMEOUT
    }

    private final Tipo tipo;
    private final Integer status;
    private final String recurso;
    private final String idErro;
    private final String codigoErro;
    private final List<String> mensagens;
    private final String detalhe;
    private final String stacktraceExterno;

    public FalhaCriacaoDossieProduto(
            Tipo tipo,
            Integer status,
            String recurso,
            String idErro,
            String codigoErro,
            List<String> mensagens,
            String detalhe,
            String stacktraceExterno,
            Throwable causa
    ) {
        super(mensagem(mensagens, causa), causa);
        this.tipo = tipo;
        this.status = status;
        this.recurso = recurso;
        this.idErro = idErro;
        this.codigoErro = codigoErro;
        this.mensagens = mensagens;
        this.detalhe = detalhe;
        this.stacktraceExterno = stacktraceExterno;
    }

    public Tipo tipo() {
        return tipo;
    }

    public Integer status() {
        return status;
    }

    public String recurso() {
        return recurso;
    }

    public String idErro() {
        return idErro;
    }

    public String codigoErro() {
        return codigoErro;
    }

    public List<String> mensagens() {
        return mensagens;
    }

    public String detalhe() {
        return detalhe;
    }

    public String stacktraceExterno() {
        return stacktraceExterno;
    }

    private static String mensagem(List<String> mensagens, Throwable causa) {
        if (mensagens != null && !mensagens.isEmpty() && mensagens.getFirst() != null) {
            return mensagens.getFirst();
        }
        return causa != null ? causa.getMessage() : "Falha ao criar dossie produto";
    }
}

package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import java.util.List;

class FalhaDossieProduto extends RuntimeException {

    private final Enum<?> tipo;
    private final Integer status;
    private final String recurso;
    private final String idErro;
    private final String codigoErro;
    private final List<String> mensagens;
    private final String detalhe;
    private final String stacktraceExterno;

    FalhaDossieProduto(
            Enum<?> tipo,
            Dados dados,
            Throwable causa,
            String mensagemPadrao
    ) {
        super(mensagem(dados.mensagens(), causa, mensagemPadrao), causa);
        this.tipo = tipo;
        this.status = dados.status();
        this.recurso = dados.recurso();
        this.idErro = dados.idErro();
        this.codigoErro = dados.codigoErro();
        this.mensagens = dados.mensagens();
        this.detalhe = dados.detalhe();
        this.stacktraceExterno = dados.stacktraceExterno();
    }

    protected final <E extends Enum<E>> E tipo(Class<E> classe) {
        return classe.cast(tipo);
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

    record Dados(
            Integer status,
            String recurso,
            String idErro,
            String codigoErro,
            List<String> mensagens,
            String detalhe,
            String stacktraceExterno
    ) {
    }

    private static String mensagem(
            List<String> mensagens,
            Throwable causa,
            String mensagemPadrao
    ) {
        if (mensagens != null && !mensagens.isEmpty() && mensagens.getFirst() != null) {
            return mensagens.getFirst();
        }
        return causa != null ? causa.getMessage() : mensagemPadrao;
    }
}

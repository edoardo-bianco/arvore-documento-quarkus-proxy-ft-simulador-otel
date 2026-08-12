package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import java.util.List;

public final class FalhaConsultaDossieProduto extends FalhaDossieProduto {

    public enum Tipo {
        NEGOCIO,
        TECNICA_CLIENTE,
        DEPENDENCIA_INDISPONIVEL,
        TIMEOUT
    }

    @SuppressWarnings("java:S107") // Os nove campos preservam sem perda o erro externo aprovado.
    public FalhaConsultaDossieProduto(
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
        super(
                tipo,
                new Dados(
                        status,
                        recurso,
                        idErro,
                        codigoErro,
                        mensagens,
                        detalhe,
                        stacktraceExterno),
                causa,
                "Falha ao consultar dossie produto");
    }

    public Tipo tipo() {
        return super.tipo(Tipo.class);
    }
}

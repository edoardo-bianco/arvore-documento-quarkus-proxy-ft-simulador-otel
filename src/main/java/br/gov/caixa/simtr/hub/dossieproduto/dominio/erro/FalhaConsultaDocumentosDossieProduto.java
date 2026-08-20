package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import java.util.List;

public final class FalhaConsultaDocumentosDossieProduto extends FalhaDossieProduto {

    private static final String MENSAGEM_OBSERVAVEL =
            "Falha ao consultar documentos do dossie produto";

    public enum Tipo {
        NEGOCIO,
        TECNICA_CLIENTE,
        DEPENDENCIA_INDISPONIVEL,
        TIMEOUT
    }

    @SuppressWarnings("java:S107") // Os nove campos preservam sem perda o erro externo aprovado.
    public FalhaConsultaDocumentosDossieProduto(
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
                MENSAGEM_OBSERVAVEL);
    }

    @Override
    public String getMessage() {
        return MENSAGEM_OBSERVAVEL;
    }

    public Tipo tipo() {
        return super.tipo(Tipo.class);
    }
}

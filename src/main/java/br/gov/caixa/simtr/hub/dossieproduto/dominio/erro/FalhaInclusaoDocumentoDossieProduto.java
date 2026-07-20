package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import java.util.List;

public final class FalhaInclusaoDocumentoDossieProduto extends FalhaDossieProduto {

    public enum Tipo {
        NEGOCIO,
        TECNICA_CLIENTE,
        DEPENDENCIA_INDISPONIVEL,
        TIMEOUT
    }

    public FalhaInclusaoDocumentoDossieProduto(
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
                "Falha ao incluir documento no dossie produto");
    }

    public Tipo tipo() {
        return super.tipo(Tipo.class);
    }

    @Override
    public List<String> mensagens() {
        return super.mensagens();
    }
}

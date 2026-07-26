package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;

public record VisaoAnaliseConformidade(
        String correlationId,
        String instanceId,
        String identificadorDocumento,
        Long identificadorChecklist,
        Integer versaoChecklist,
        StatusAnaliseConformidade status,
        ResultadoAnaliseConformidade resultadoPreliminar,
        ResultadoAnaliseConformidade resultadoFinal,
        String mensagemErro) {

    public VisaoAnaliseConformidade {
        validarIdentidades(
                correlationId,
                instanceId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                resultadoPreliminar,
                resultadoFinal);
        validar(status != null);
        switch (status) {
            case EM_PROCESSAMENTO -> validar(
                    resultadoPreliminar == null && resultadoFinal == null && mensagemErro == null);
            case AGUARDANDO_REVISAO -> validar(
                    resultadoPreliminar != null && resultadoFinal == null && mensagemErro == null);
            case CONCLUIDA -> validar(
                    resultadoPreliminar != null
                            && resultadoFinal != null
                            && resultadoFinal.origem() == OrigemResultado.REVISAO_HUMANA
                            && mensagemErro == null);
            case FALHOU -> validar(
                    resultadoFinal == null && mensagemErro != null && !mensagemErro.isBlank());
        }
    }

    public static VisaoAnaliseConformidade emProcessamento(
            String correlationId,
            String instanceId,
            String identificadorDocumento,
            Long identificadorChecklist,
            Integer versaoChecklist) {
        return new VisaoAnaliseConformidade(
                correlationId,
                instanceId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                StatusAnaliseConformidade.EM_PROCESSAMENTO,
                null,
                null,
                null);
    }

    public VisaoAnaliseConformidade aguardandoRevisao(
            ResultadoAnaliseConformidade novoResultadoPreliminar) {
        return new VisaoAnaliseConformidade(
                correlationId,
                instanceId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                novoResultadoPreliminar,
                null,
                null);
    }

    public VisaoAnaliseConformidade concluida(
            ResultadoAnaliseConformidade novoResultadoFinal) {
        return new VisaoAnaliseConformidade(
                correlationId,
                instanceId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                StatusAnaliseConformidade.CONCLUIDA,
                resultadoPreliminar,
                novoResultadoFinal,
                null);
    }

    public VisaoAnaliseConformidade falhou(String novaMensagemErro) {
        return new VisaoAnaliseConformidade(
                correlationId,
                instanceId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                StatusAnaliseConformidade.FALHOU,
                resultadoPreliminar,
                null,
                novaMensagemErro);
    }

    public void validarIdentidadesPersistidas() {
        validarIdentidades(
                correlationId,
                instanceId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                resultadoPreliminar,
                resultadoFinal);
    }

    private static void validarIdentidades(
            String correlationId,
            String instanceId,
            String identificadorDocumento,
            Long identificadorChecklist,
            Integer versaoChecklist,
            ResultadoAnaliseConformidade resultadoPreliminar,
            ResultadoAnaliseConformidade resultadoFinal) {
        validar(correlationId != null && !correlationId.isBlank());
        validar(instanceId != null && !instanceId.isBlank());
        validar(identificadorDocumento != null && !identificadorDocumento.isBlank());
        validar(identificadorChecklist != null && identificadorChecklist > 0);
        validar(versaoChecklist != null && versaoChecklist > 0);
        validarResultado(
                identificadorChecklist,
                versaoChecklist,
                resultadoPreliminar);
        validarResultado(
                identificadorChecklist,
                versaoChecklist,
                resultadoFinal);
    }

    private static void validarResultado(
            Long identificadorChecklist,
            Integer versaoChecklist,
            ResultadoAnaliseConformidade resultado) {
        validar(resultado == null
                || identificadorChecklist.equals(resultado.identificadorChecklist())
                && versaoChecklist.equals(resultado.versaoChecklist()));
    }

    private static void validar(boolean condicao) {
        if (!condicao) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }
}

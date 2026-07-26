package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;

public record VisaoAnaliseConformidade(
        String instanceId,
        StatusAnaliseConformidade status,
        ResultadoAnaliseConformidade resultadoPreliminar,
        ResultadoAnaliseConformidade resultadoFinal,
        String mensagemErro) {

    public VisaoAnaliseConformidade {
        if (instanceId == null || instanceId.isBlank() || status == null) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
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

    public static VisaoAnaliseConformidade emProcessamento(String instanceId) {
        return new VisaoAnaliseConformidade(
                instanceId,
                StatusAnaliseConformidade.EM_PROCESSAMENTO,
                null,
                null,
                null);
    }

    public static VisaoAnaliseConformidade aguardandoRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultadoPreliminar) {
        return new VisaoAnaliseConformidade(
                instanceId,
                StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                resultadoPreliminar,
                null,
                null);
    }

    public static VisaoAnaliseConformidade concluida(
            String instanceId,
            ResultadoAnaliseConformidade resultadoPreliminar,
            ResultadoAnaliseConformidade resultadoFinal) {
        return new VisaoAnaliseConformidade(
                instanceId,
                StatusAnaliseConformidade.CONCLUIDA,
                resultadoPreliminar,
                resultadoFinal,
                null);
    }

    public static VisaoAnaliseConformidade falhou(
            String instanceId,
            ResultadoAnaliseConformidade resultadoPreliminar,
            String mensagemErro) {
        return new VisaoAnaliseConformidade(
                instanceId,
                StatusAnaliseConformidade.FALHOU,
                resultadoPreliminar,
                null,
                mensagemErro);
    }

    private static void validar(boolean condicao) {
        if (!condicao) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }
}

package br.gov.caixa.simtr.hub.conformidade.dominio.erro;

public final class FalhaAnaliseConformidade extends RuntimeException {

    public enum Tipo {
        SOLICITACAO_INVALIDA,
        CHECKLIST_INVALIDO,
        RESULTADO_INVALIDO,
        REVISAO_INCONSISTENTE,
        INSTANCIA_NAO_ENCONTRADA,
        TRANSICAO_INVALIDA,
        INDISPONIBILIDADE_TECNICA
    }

    private final Tipo tipo;

    private FalhaAnaliseConformidade(Tipo tipo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
    }

    public Tipo tipo() {
        return tipo;
    }

    public static FalhaAnaliseConformidade solicitacaoInvalida(String mensagem) {
        return new FalhaAnaliseConformidade(Tipo.SOLICITACAO_INVALIDA, mensagem);
    }

    public static FalhaAnaliseConformidade checklistInvalido(String mensagem) {
        return new FalhaAnaliseConformidade(Tipo.CHECKLIST_INVALIDO, mensagem);
    }

    public static FalhaAnaliseConformidade resultadoInvalido(String mensagem) {
        return new FalhaAnaliseConformidade(Tipo.RESULTADO_INVALIDO, mensagem);
    }

    public static FalhaAnaliseConformidade revisaoInconsistente(String mensagem) {
        return new FalhaAnaliseConformidade(Tipo.REVISAO_INCONSISTENTE, mensagem);
    }

    public static FalhaAnaliseConformidade instanciaNaoEncontrada() {
        return new FalhaAnaliseConformidade(
                Tipo.INSTANCIA_NAO_ENCONTRADA,
                "Análise de conformidade não localizada");
    }

    public static FalhaAnaliseConformidade transicaoInvalida() {
        return new FalhaAnaliseConformidade(
                Tipo.TRANSICAO_INVALIDA,
                "Transição inválida para o estado atual da análise");
    }

    public static FalhaAnaliseConformidade indisponibilidadeTecnica() {
        return new FalhaAnaliseConformidade(
                Tipo.INDISPONIBILIDADE_TECNICA,
                "Serviço de análise temporariamente indisponível");
    }
}

package br.gov.caixa.simtr.hub.dossieproduto.dominio;

public record DossieProdutoValidacaoNegocialParecerApontamentoVo(
        Long identificadorApontamento,
        String resultado,
        String comentario,
        Boolean necessidadeReanalise,
        Double indiceIa
) {
}

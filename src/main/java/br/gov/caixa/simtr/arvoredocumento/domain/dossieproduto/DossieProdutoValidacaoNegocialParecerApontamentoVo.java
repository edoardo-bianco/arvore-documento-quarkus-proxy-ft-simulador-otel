package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

public record DossieProdutoValidacaoNegocialParecerApontamentoVo(
        Long identificadorApontamento,
        String resultado,
        String comentario,
        Boolean necessidadeReanalise,
        Double indiceIa
) {
}

package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

public record ParecerApontamentoValidacaoNegocialDossieProduto(
        Long identificadorApontamento,
        String resultado,
        String comentario,
        Boolean necessidadeReanalise,
        Double indiceIa
) {
}

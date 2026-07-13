package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record ComandoAtualizacaoFormularioDossieProduto(
        Long identificadorDossieProduto,
        List<FormularioDossieProduto> formularios
) {
}

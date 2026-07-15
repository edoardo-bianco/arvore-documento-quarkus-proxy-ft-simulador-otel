package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record RelacionamentoProcessoParametrizado(
        Long identificadorNegocial,
        String nome,
        String tipoPessoa,
        Boolean principal,
        Boolean obrigatorio,
        Boolean relacionado,
        Boolean sequencia,
        List<CampoFormularioProcessoParametrizado> camposFormulario,
        List<DocumentoProcessoParametrizado> documentos
) {
}

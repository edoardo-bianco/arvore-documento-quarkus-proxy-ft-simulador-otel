package br.gov.caixa.simtr.hub.parametrizacao.dominio.processo;

import java.util.List;

public record RelacionamentoVo(
        Long identificadorNegocial,
        String nome,
        String tipoPessoa,
        Boolean principal,
        Boolean obrigatorio,
        Boolean relacionado,
        Boolean sequencia,
        List<CampoFormularioVo> camposFormulario,
        List<DocumentoVo> documentos
) {
}

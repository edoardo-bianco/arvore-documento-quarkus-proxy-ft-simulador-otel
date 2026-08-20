package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

public record CriteriosConsultaDocumentosDossieProduto(
        IdentificadorDossieProduto identificador,
        String cnpj,
        String cpf,
        Long fase,
        Boolean incluiArmazenamento,
        Boolean incluiAssinaturas,
        Boolean incluiAtributos,
        Boolean incluiConformidade,
        Boolean incluiOutsourcing,
        Boolean incluiPropriedades,
        Boolean incluiUrl,
        String ipUsuario,
        String tipologia
) {
}

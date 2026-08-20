package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos;

import jakarta.ws.rs.QueryParam;

public class ConsultaDocumentosDossieProdutoMtrQuery {

    @QueryParam("cnpj")
    private String cnpj;

    @QueryParam("cpf")
    private String cpf;

    @QueryParam("fase")
    private Long fase;

    @QueryParam("inclui-armazenamento")
    private Boolean incluiArmazenamento;

    @QueryParam("inclui-assinaturas")
    private Boolean incluiAssinaturas;

    @QueryParam("inclui-atributos")
    private Boolean incluiAtributos;

    @QueryParam("inclui-conformidade")
    private Boolean incluiConformidade;

    @QueryParam("inclui-outsourcing")
    private Boolean incluiOutsourcing;

    @QueryParam("inclui-propriedades")
    private Boolean incluiPropriedades;

    @QueryParam("inclui-url")
    private Boolean incluiUrl;

    @QueryParam("ip-usuario")
    private String ipUsuario;

    @QueryParam("tipologia")
    private String tipologia;

    public ConsultaDocumentosDossieProdutoMtrQuery() {
    }

    @SuppressWarnings("java:S107") // Os campos refletem os doze filtros MTR v4 aprovados.
    public ConsultaDocumentosDossieProdutoMtrQuery(
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
        this.cnpj = cnpj;
        this.cpf = cpf;
        this.fase = fase;
        this.incluiArmazenamento = incluiArmazenamento;
        this.incluiAssinaturas = incluiAssinaturas;
        this.incluiAtributos = incluiAtributos;
        this.incluiConformidade = incluiConformidade;
        this.incluiOutsourcing = incluiOutsourcing;
        this.incluiPropriedades = incluiPropriedades;
        this.incluiUrl = incluiUrl;
        this.ipUsuario = ipUsuario;
        this.tipologia = tipologia;
    }

    public String cnpj() {
        return cnpj;
    }

    public String cpf() {
        return cpf;
    }

    public Long fase() {
        return fase;
    }

    public Boolean incluiArmazenamento() {
        return incluiArmazenamento;
    }

    public Boolean incluiAssinaturas() {
        return incluiAssinaturas;
    }

    public Boolean incluiAtributos() {
        return incluiAtributos;
    }

    public Boolean incluiConformidade() {
        return incluiConformidade;
    }

    public Boolean incluiOutsourcing() {
        return incluiOutsourcing;
    }

    public Boolean incluiPropriedades() {
        return incluiPropriedades;
    }

    public Boolean incluiUrl() {
        return incluiUrl;
    }

    public String ipUsuario() {
        return ipUsuario;
    }

    public String tipologia() {
        return tipologia;
    }
}

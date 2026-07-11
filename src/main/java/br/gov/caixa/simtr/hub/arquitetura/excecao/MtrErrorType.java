package br.gov.caixa.simtr.hub.arquitetura.excecao;

public enum MtrErrorType {

    NEGOCIO("negocio"),
    TECNICO_CLIENTE("tecnico_cliente"),
    TECNICO_SERVIDOR("tecnico_servidor");

    private final String codigo;

    MtrErrorType(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}

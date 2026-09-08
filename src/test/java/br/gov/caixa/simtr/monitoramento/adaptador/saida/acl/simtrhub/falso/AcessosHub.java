package br.gov.caixa.simtr.monitoramento.adaptador.saida.acl.simtrhub.falso;

public final class AcessosHub {
    private AcessosHub() {
    }

    public interface Permitido {
        br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado executar(
                br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto porta,
                br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto identificador);
    }

    public interface Interno {
        br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter acessar();
    }
}

package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.dto.GestaoDocumentoSimuladorResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GestaoDocumentoSimuladorMapper {

    public CredencialContainer paraDominio(GestaoDocumentoSimuladorResponse resposta) {
        if (resposta == null) {
            return null;
        }
        return new CredencialContainer(
                resposta.sas(),
                resposta.validade(),
                resposta.urlStorage(),
                resposta.nomeContainer()
        );
    }
}

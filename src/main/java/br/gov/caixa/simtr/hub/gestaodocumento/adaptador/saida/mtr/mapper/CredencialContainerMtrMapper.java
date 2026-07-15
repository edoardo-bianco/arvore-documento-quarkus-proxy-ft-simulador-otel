package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.dto.v1.credencial.CredencialContainerMtrResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CredencialContainerMtrMapper {

    public CredencialContainer paraDominio(CredencialContainerMtrResponse resposta) {
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

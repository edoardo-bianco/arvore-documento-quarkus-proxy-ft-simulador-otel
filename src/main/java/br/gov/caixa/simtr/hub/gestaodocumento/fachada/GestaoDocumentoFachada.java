package br.gov.caixa.simtr.hub.gestaodocumento.fachada;

import br.gov.caixa.simtr.hub.gestaodocumento.dominio.GestaoDocumentoCredencialContainerVo;
import br.gov.caixa.simtr.hub.gestaodocumento.servico.GestaoDocumentoService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GestaoDocumentoFachada {

    private final GestaoDocumentoService gestaoDocumentoService;

    @Inject
    public GestaoDocumentoFachada(GestaoDocumentoService gestaoDocumentoService) {
        this.gestaoDocumentoService = gestaoDocumentoService;
    }

    public Uni<GestaoDocumentoCredencialContainerVo> gerarCredencialContainer() {
        return gestaoDocumentoService.gerarCredencialContainer();
    }
}

package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import java.util.HashMap;
import java.util.Map;

public final class DossieProdutoSimuladorMtrStubTestResource
        extends DossieProdutoMtrStubTestResource {

    @Override
    public Map<String, String> start() {
        var configuracao = new HashMap<>(super.start());
        configuracao.put("simtr-hub.simulador.dossie-produto.habilitado", "true");
        return Map.copyOf(configuracao);
    }
}

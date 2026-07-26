package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.contrato.ArmazenarEstadoAnaliseConformidadeContractTest;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AnaliseConformidadeMemoryStoreContractTest
        extends ArmazenarEstadoAnaliseConformidadeContractTest {

    @Override
    protected ArmazenarEstadoAnaliseConformidade novoStore() {
        return new AnaliseConformidadeMemoryStore();
    }
}

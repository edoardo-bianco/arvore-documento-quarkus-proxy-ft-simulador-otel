package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import java.util.List;

import org.jboss.logging.Logger;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.CapturaDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.CapturaDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@CapturaSimulador
public class CapturaDossieProdutoSimuladorAdapter implements SolicitarCapturaDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            CapturaDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/captura-dossie-produto.md";
    private static final String SERVICO = "simtr-dossie-produto";
    private static final String CODIGO_NAO_ENCONTRADO = "DOSSIE_PRODUTO_NAO_ENCONTRADO";

    private final MarkdownJsonMockReader mockReader;
    private final CapturaDossieProdutoSimuladorMapper mapper;

    @Inject
    public CapturaDossieProdutoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            CapturaDossieProdutoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<ResultadoCapturaDossieProduto> capturar(
            IdentificadorDossieProduto identificador
    ) {
        Long id = identificador != null ? identificador.valor() : null;
        registrarUso(id);
        if (id == null) {
            return Uni.createFrom().failure(naoEncontrado(null, null));
        }

        CapturaDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                CapturaDossieProdutoSimuladorResponse.class);
        Long idDisponivel = resposta != null ? resposta.id() : null;
        if (idDisponivel == null || !id.equals(idDisponivel)) {
            return Uni.createFrom().failure(naoEncontrado(id, idDisponivel));
        }
        return Uni.createFrom().item(mapper.paraResultado(resposta));
    }

    private static void registrarUso(Long identificador) {
        Span span = Span.current();
        span.setAttribute("simtr_hub.origem_dados", "mock");
        if (identificador != null) {
            span.setAttribute("dossie_produto.id", identificador);
        }
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.captura.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "DossieProdutoGateway",
                        "operacao", "capturar-dossie-produto-v1",
                        "dossie_produto_id", identificador,
                        "origem", "mock"));
    }

    private static FalhaCapturaDossieProduto naoEncontrado(
            Long identificador,
            Long identificadorDisponivel
    ) {
        String valor = String.valueOf(identificador);
        String mensagem = "Dossie produto " + valor + " nao encontrado no simulador.";
        if (identificadorDisponivel != null) {
            mensagem += " O unico identificador disponivel no simulador e "
                    + identificadorDisponivel + ".";
        }
        return new FalhaCapturaDossieProduto(
                FalhaCapturaDossieProduto.Tipo.NEGOCIO,
                404,
                SERVICO,
                "mock-captura-dossie-produto-" + valor,
                CODIGO_NAO_ENCONTRADO,
                List.of(mensagem),
                null,
                null,
                null);
    }
}

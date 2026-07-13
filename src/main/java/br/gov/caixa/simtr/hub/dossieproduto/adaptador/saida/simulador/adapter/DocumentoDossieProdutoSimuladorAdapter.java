package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.DocumentoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.DocumentoDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@DocumentoSimulador
public class DocumentoDossieProdutoSimuladorAdapter
        implements SolicitarInclusaoDocumentoDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            DocumentoDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/documento-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public DocumentoDossieProdutoSimuladorAdapter(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    @Override
    public Uni<ResultadoInclusaoDocumentoDossieProduto> incluir(
            ComandoInclusaoDocumentoDossieProduto comando
    ) {
        DocumentoDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                DocumentoDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.documento.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "incluir-documento-dossie-produto",
                        "dossie_produto_id", identificadorDossie(comando),
                        "tipo_documento", tipoDocumento(comando),
                        "origem", "mock"));
        return Uni.createFrom().item(new ResultadoInclusaoDocumentoDossieProduto(
                resposta.idDocumento(), resposta.idInstanciaDocumento()));
    }

    private static Long identificadorDossie(ComandoInclusaoDocumentoDossieProduto comando) {
        return comando != null ? comando.identificadorDossieProduto() : null;
    }

    private static String tipoDocumento(ComandoInclusaoDocumentoDossieProduto comando) {
        return comando != null ? comando.tipoDocumento() : null;
    }
}

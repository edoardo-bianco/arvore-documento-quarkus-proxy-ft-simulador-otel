package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDocumentosDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ConsultaDocumentosDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
@ConsultaDocumentosDossieProdutoSimulador
public class ConsultaDocumentosDossieProdutoSimuladorAdapter
        implements ObterDocumentosDossieProduto {

    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/dossieproduto/%d-v4-consulta-documentos-dossie-produto.md";
    private static final String SERVICO = "simtr-dossie-produto";
    private static final String CODIGO_NAO_ENCONTRADO = "DOSSIE_PRODUTO_NAO_ENCONTRADO";

    private final MarkdownJsonMockReader mockReader;
    private final ConsultaDocumentosDossieProdutoSimuladorMapper mapper;

    @Inject
    public ConsultaDocumentosDossieProdutoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            ConsultaDocumentosDossieProdutoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<List<DocumentoDossieProdutoConsultado>> obter(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        Long identificador = criterios != null && criterios.identificador() != null
                ? criterios.identificador().valor()
                : null;
        if (identificador == null) {
            return Uni.createFrom().failure(naoEncontrado(null));
        }

        String recurso = MOCK_RESOURCE_TEMPLATE.formatted(identificador);
        ConsultaDocumentosDossieProdutoSimuladorResponse resposta =
                mockReader.readFirstJsonObject(
                        recurso,
                        ConsultaDocumentosDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            return Uni.createFrom().failure(naoEncontrado(identificador));
        }
        return Uni.createFrom().item(mapper.paraDominio(resposta));
    }

    private static FalhaConsultaDocumentosDossieProduto naoEncontrado(Long identificador) {
        String valor = String.valueOf(identificador);
        return new FalhaConsultaDocumentosDossieProduto(
                FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO,
                404,
                SERVICO,
                "mock-consulta-documentos-dossie-produto-" + valor,
                CODIGO_NAO_ENCONTRADO,
                List.of("Dossie produto " + valor + " nao encontrado no simulador."),
                null,
                null,
                null);
    }
}

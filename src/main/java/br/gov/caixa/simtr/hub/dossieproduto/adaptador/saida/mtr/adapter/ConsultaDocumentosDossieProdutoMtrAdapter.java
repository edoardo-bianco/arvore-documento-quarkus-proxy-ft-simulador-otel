package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ConsultaDocumentosDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrQuery;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDocumentosDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ConsultaDocumentosDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@ConsultaDocumentosDossieProdutoMtr
public class ConsultaDocumentosDossieProdutoMtrAdapter
        implements ObterDocumentosDossieProduto {

    private static final Logger LOG =
            Logger.getLogger(ConsultaDocumentosDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String CONSULTAR = "consultar-documentos-dossie-produto-v4";
    private static final String DOSSIE_PRODUTO_ID = "dossie_produto_id";

    private final ConsultaDocumentosDossieProdutoMtrClient client;
    private final ConsultaDocumentosDossieProdutoMtrMapper mapper;

    @Inject
    public ConsultaDocumentosDossieProdutoMtrAdapter(
            @RestClient ConsultaDocumentosDossieProdutoMtrClient client,
            ConsultaDocumentosDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.documentos.consultar", kind = SpanKind.CLIENT)
    public Uni<List<DocumentoDossieProdutoConsultado>> obter(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        Long identificador = criterios != null && criterios.identificador() != null
                ? criterios.identificador().valor()
                : null;
        Span span = Span.current();
        span.setAttribute("mtr.servico", DOSSIE);
        span.setAttribute("mtr.api", "dossie-produto-v4");
        span.setAttribute("http.request.method", "GET");
        span.setAttribute(
                "url.path",
                "/simtr/dossie-produto/v4/dossie-produto/{id}/documentos");
        setLongAttribute(span, "dossie_produto.id", identificador);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.documentos.consulta.chamada.iniciada",
                campos(identificador, null, null, null));

        return client.consultar(identificador, paraQuery(criterios))
                .invoke(respostas -> registrarConclusao(span, identificador, respostas))
                .onFailure().invoke(falha -> registrarFalha(span, identificador, falha))
                .map(mapper::paraDominio)
                .onFailure().transform(ConsultaDocumentosDossieProdutoMtrAdapter::traduzir);
    }

    private static ConsultaDocumentosDossieProdutoMtrQuery paraQuery(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        if (criterios == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoMtrQuery(
                criterios.cnpj(),
                criterios.cpf(),
                criterios.fase(),
                criterios.incluiArmazenamento(),
                criterios.incluiAssinaturas(),
                criterios.incluiAtributos(),
                criterios.incluiConformidade(),
                criterios.incluiOutsourcing(),
                criterios.incluiPropriedades(),
                criterios.incluiUrl(),
                criterios.ipUsuario(),
                criterios.tipologia());
    }

    private static Throwable traduzir(Throwable falha) {
        if (!(falha instanceof ConsultaDocumentosDossieProdutoMtrException mtr)) {
            FalhaConsultaDocumentosDossieProduto.Tipo tipo = falha instanceof TimeoutException
                    ? FalhaConsultaDocumentosDossieProduto.Tipo.TIMEOUT
                    : FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            return new FalhaConsultaDocumentosDossieProduto(
                    tipo,
                    null,
                    DOSSIE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    falha);
        }
        ConsultaDocumentosDossieProdutoMtrException.Erro erro = mtr.erro();
        FalhaConsultaDocumentosDossieProduto.Tipo tipo = switch (mtr) {
            case ConsultaDocumentosDossieProdutoMtrException.Negocio _ ->
                    FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO;
            case ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente _ ->
                    FalhaConsultaDocumentosDossieProduto.Tipo.TECNICA_CLIENTE;
            case ConsultaDocumentosDossieProdutoMtrException.Servidor _ ->
                    FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaConsultaDocumentosDossieProduto(
                tipo,
                mtr.status(),
                erro != null ? erro.recurso() : null,
                erro != null ? erro.idErro() : null,
                erro != null ? erro.codigoErro() : null,
                mensagens(erro),
                erro != null ? erro.detalhe() : null,
                erro != null ? erro.stacktrace() : null,
                mtr);
    }

    @SuppressWarnings("java:S1168") // Null preserva a ausência da coleção no erro externo.
    private static List<String> mensagens(
            ConsultaDocumentosDossieProdutoMtrException.Erro erro
    ) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (ConsultaDocumentosDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }

    private static void registrarConclusao(
            Span span,
            Long identificador,
            List<?> respostas
    ) {
        int quantidade = respostas != null ? respostas.size() : 0;
        span.setAttribute("mtr.resposta.sucesso", true);
        span.setAttribute("dossie_produto.documentos.quantidade", quantidade);
        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.documentos.consulta.chamada.concluida",
                campos(identificador, quantidade, "sucesso", null));
    }

    private static void registrarFalha(Span span, Long identificador, Throwable falha) {
        String tipoErro = falha.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, "falha na consulta MTR de documentos do dossie produto");
        span.setAttribute("mtr.resposta.sucesso", false);
        span.setAttribute("erro.tipo", tipoErro);
        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.documentos.consulta.chamada.falhou",
                campos(identificador, null, "erro", tipoErro));
    }

    private static Map<String, Object> campos(
            Long identificador,
            Integer quantidade,
            String resultado,
            String tipoErro
    ) {
        return ObservabilityLog.fields(
                CAMADA, INFRASTRUCTURE,
                COMPONENTE, GATEWAY,
                DEPENDENCIA, DOSSIE,
                OPERACAO, CONSULTAR,
                DOSSIE_PRODUTO_ID, identificador,
                "documentos_quantidade", quantidade,
                "erro_tipo", tipoErro,
                "resultado", resultado);
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}

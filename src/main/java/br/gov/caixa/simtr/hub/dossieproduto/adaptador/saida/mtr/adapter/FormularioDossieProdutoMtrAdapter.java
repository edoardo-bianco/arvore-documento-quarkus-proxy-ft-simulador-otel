package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.FormularioMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.FormularioDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.FormularioDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.FormularioDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
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
import java.util.Objects;

@ApplicationScoped
@FormularioMtr
public class FormularioDossieProdutoMtrAdapter
        implements SolicitarAtualizacaoFormularioDossieProduto {

    private static final Logger LOG = Logger.getLogger(FormularioDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String ATUALIZAR = "atualizar-formulario-dossie-produto-v1";

    private final FormularioDossieProdutoMtrClient client;
    private final FormularioDossieProdutoMtrMapper mapper;

    @Inject
    public FormularioDossieProdutoMtrAdapter(
            @RestClient FormularioDossieProdutoMtrClient client,
            FormularioDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.formulario.atualizar", kind = SpanKind.CLIENT)
    public Uni<ResultadoAtualizacaoFormularioDossieProduto> atualizar(
            ComandoAtualizacaoFormularioDossieProduto comando
    ) {
        Long identificador = comando != null ? comando.identificadorDossieProduto() : null;
        Integer quantidadeVinculos = quantidadeVinculos(comando);
        Integer quantidadeRespostas = quantidadeRespostas(comando);

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "PATCH");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto/"
                + identificador + "/formulario");
        setLongAttribute(span, "dossie_produto.id", identificador);
        setIntAttribute(span, "dossie_produto.formulario.vinculos.quantidade",
                quantidadeVinculos);
        setIntAttribute(span, "dossie_produto.formulario.respostas.quantidade",
                quantidadeRespostas);

        ObservabilityLog.info(LOG, "mtr.dossie-produto.formulario.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE, OPERACAO, ATUALIZAR,
                        "dossie_produto_id", identificador,
                        "formulario_vinculos_quantidade", quantidadeVinculos,
                        "formulario_respostas_quantidade", quantidadeRespostas));

        return client.atualizar(identificador, mapper.paraMtr(comando))
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id_resposta", resposta.id());
                    }
                    ObservabilityLog.info(LOG,
                            "mtr.dossie-produto.formulario.chamada.concluida",
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE, OPERACAO, ATUALIZAR,
                                    "dossie_produto_id", identificador,
                                    "dossie_produto_id_resposta",
                                    resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", erro.getClass().getName());
                    ObservabilityLog.error(LOG,
                            "mtr.dossie-produto.formulario.chamada.falhou", erro,
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE, OPERACAO, ATUALIZAR,
                                    "dossie_produto_id", identificador,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                })
                .map(mapper::paraResultado)
                .onFailure().transform(FormularioDossieProdutoMtrAdapter::traduzir);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof FormularioDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaAtualizacaoFormularioDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaAtualizacaoFormularioDossieProduto.Tipo.TIMEOUT
                : FalhaAtualizacaoFormularioDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaAtualizacaoFormularioDossieProduto(
                tipo, null, "simtr-dossie-produto", null, null,
                null, null, null, falha);
    }

    private static FalhaAtualizacaoFormularioDossieProduto traduzirMtr(
            FormularioDossieProdutoMtrException falha
    ) {
        FormularioDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaAtualizacaoFormularioDossieProduto.Tipo tipo = switch (falha) {
            case FormularioDossieProdutoMtrException.Negocio ignored ->
                    FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO;
            case FormularioDossieProdutoMtrException.TecnicaCliente ignored ->
                    FalhaAtualizacaoFormularioDossieProduto.Tipo.TECNICA_CLIENTE;
            case FormularioDossieProdutoMtrException.Servidor ignored ->
                    FalhaAtualizacaoFormularioDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaAtualizacaoFormularioDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaAtualizacaoFormularioDossieProduto(
                tipo,
                falha.status(),
                erro != null ? erro.recurso() : null,
                erro != null ? erro.idErro() : null,
                erro != null ? erro.codigoErro() : null,
                mensagens(erro),
                erro != null ? erro.detalhe() : null,
                erro != null ? erro.stacktrace() : null,
                falha);
    }

    private static List<String> mensagens(FormularioDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (FormularioDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }

    private static Integer quantidadeVinculos(
            ComandoAtualizacaoFormularioDossieProduto comando
    ) {
        return comando != null && comando.formularios() != null
                ? comando.formularios().size() : null;
    }

    private static Integer quantidadeRespostas(
            ComandoAtualizacaoFormularioDossieProduto comando
    ) {
        if (comando == null || comando.formularios() == null) {
            return null;
        }
        return comando.formularios().stream()
                .filter(Objects::nonNull)
                .map(FormularioDossieProduto::vinculoDossie)
                .filter(Objects::nonNull)
                .map(vinculo -> vinculo.respostasFormulario())
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static void setIntAttribute(Span span, String nome, Integer valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}

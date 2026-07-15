package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.CriarDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CriarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CriacaoDossieProdutoObservabilidade implements CriarDossieProduto {

    private static final Logger LOG = Logger.getLogger(CriacaoDossieProdutoObservabilidade.class);

    private final CriarDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public CriacaoDossieProdutoObservabilidade(
            SolicitarCriacaoDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new CriarDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.dossie-produto.criar")
    public Uni<ResultadoCriacaoDossieProduto> executar(ComandoCriacaoDossieProduto comando) {
        Long processo = comando != null ? comando.processo() : null;
        Long chaveCorrelacaoCanal = comando != null ? comando.chaveCorrelacaoCanal() : null;
        Integer quantidadeClientes = comando != null && comando.clientes() != null
                ? comando.clientes().size() : null;

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado", simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        setLongAttribute(span, "dossie_produto.processo", processo);
        setLongAttribute(span, "dossie_produto.chave_correlacao_canal", chaveCorrelacaoCanal);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", quantidadeClientes);

        ObservabilityLog.info(LOG, "simtr-hub.dossie-produto.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "criar-dossie-produto",
                        "processo", processo,
                        "chave_correlacao_canal", chaveCorrelacaoCanal,
                        "clientes_quantidade", quantidadeClientes,
                        "simulador_habilitado", simuladorHabilitado));

        return casoDeUso.executar(comando)
                .invoke(resposta -> {
                    if (resposta != null && resposta.identificadorDossieProduto() != null) {
                        span.setAttribute("dossie_produto.id", resposta.identificadorDossieProduto());
                    }
                    ObservabilityLog.info(LOG, "simtr-hub.dossie-produto.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "criar-dossie-produto",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "dossie_produto_id", resposta != null
                                            ? resposta.identificadorDossieProduto() : null,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    ObservabilityLog.error(LOG, "simtr-hub.dossie-produto.service.falhou", erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "criar-dossie-produto",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                });
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

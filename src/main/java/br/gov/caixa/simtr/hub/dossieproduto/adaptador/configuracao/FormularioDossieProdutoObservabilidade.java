package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.AtualizarFormularioDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AtualizarFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class FormularioDossieProdutoObservabilidade
        implements AtualizarFormularioDossieProduto {

    private static final Logger LOG = Logger.getLogger(FormularioDossieProdutoObservabilidade.class);
    private static final String CAMADA = "application";
    private static final String COMPONENTE = "DossieProdutoService";
    private static final String OPERACAO = "atualizar-formulario-dossie-produto";
    private static final String CAMADA_KEY = "camada";
    private static final String COMPONENTE_KEY = "componente";
    private static final String OPERACAO_KEY = "operacao";
    private static final String DOSSIE_PRODUTO_ID_KEY = "dossie_produto_id";

    private final AtualizarFormularioDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public FormularioDossieProdutoObservabilidade(
            SolicitarAtualizacaoFormularioDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new AtualizarFormularioDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.dossie-produto.formulario.atualizar")
    public Uni<ResultadoAtualizacaoFormularioDossieProduto> executar(
            ComandoAtualizacaoFormularioDossieProduto comando) {
        Long id = comando != null ? comando.identificadorDossieProduto() : null;
        List<FormularioDossieProduto> formularios = comando != null ? comando.formularios() : null;
        Integer quantidadeVinculos = quantidadeVinculos(formularios);
        Integer quantidadeRespostas = quantidadeRespostas(formularios);

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado", simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.formulario.vinculos.quantidade", quantidadeVinculos);
        setIntAttribute(span, "dossie_produto.formulario.respostas.quantidade", quantidadeRespostas);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.formulario.service.iniciado",
                ObservabilityLog.fields(
                        CAMADA_KEY, CAMADA,
                        COMPONENTE_KEY, COMPONENTE,
                        OPERACAO_KEY, OPERACAO,
                        DOSSIE_PRODUTO_ID_KEY, id,
                        "formulario_vinculos_quantidade", quantidadeVinculos,
                        "formulario_respostas_quantidade", quantidadeRespostas,
                        "simulador_habilitado", simuladorHabilitado));

        return casoDeUso.executar(comando)
                .invoke(resposta -> {
                    if (resposta != null && resposta.identificadorDossieProduto() != null) {
                        span.setAttribute(
                                "dossie_produto.id_resposta",
                                resposta.identificadorDossieProduto());
                    }
                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.dossie-produto.formulario.service.concluido",
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    DOSSIE_PRODUTO_ID_KEY, id,
                                    "dossie_produto_id_resposta",
                                    resposta != null ? resposta.identificadorDossieProduto() : null,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.formulario.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    DOSSIE_PRODUTO_ID_KEY, id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                });
    }

    private static Integer quantidadeVinculos(List<FormularioDossieProduto> formularios) {
        return formularios != null ? formularios.size() : null;
    }

    private static Integer quantidadeRespostas(List<FormularioDossieProduto> formularios) {
        if (formularios == null) {
            return null;
        }
        return formularios.stream()
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

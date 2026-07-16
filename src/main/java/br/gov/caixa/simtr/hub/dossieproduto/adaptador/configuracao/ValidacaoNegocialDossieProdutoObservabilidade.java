package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.RegistrarValidacaoNegocialDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.RegistrarValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ValidacaoNegocialDossieProdutoObservabilidade
        implements RegistrarValidacaoNegocialDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ValidacaoNegocialDossieProdutoObservabilidade.class);
    private static final String CAMADA = "application";
    private static final String COMPONENTE = "DossieProdutoService";
    private static final String OPERACAO = "registrar-validacao-negocial-dossie-produto";
    private static final String CAMADA_KEY = "camada";
    private static final String COMPONENTE_KEY = "componente";
    private static final String OPERACAO_KEY = "operacao";
    private static final String DOSSIE_PRODUTO_ID_KEY = "dossie_produto_id";

    private final RegistrarValidacaoNegocialDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public ValidacaoNegocialDossieProdutoObservabilidade(
            SolicitarRegistroValidacaoNegocialDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new RegistrarValidacaoNegocialDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.dossie-produto.validacao-negocial.registrar")
    public Uni<Void> executar(ComandoRegistroValidacaoNegocialDossieProduto comando) {
        Long id = comando != null ? comando.identificadorDossieProduto() : null;
        Integer quantidadeVerificacoes = comando != null && comando.verificacoes() != null
                ? comando.verificacoes().size() : null;
        Integer quantidadeRespostas = comando != null && comando.respostasFormulario() != null
                ? comando.respostasFormulario().size() : null;

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado",
                simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.validacao.verificacoes.quantidade",
                quantidadeVerificacoes);
        setIntAttribute(span, "dossie_produto.validacao.respostas_formulario.quantidade",
                quantidadeRespostas);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.validacao-negocial.service.iniciado",
                ObservabilityLog.fields(
                        CAMADA_KEY, CAMADA,
                        COMPONENTE_KEY, COMPONENTE,
                        OPERACAO_KEY, OPERACAO,
                        DOSSIE_PRODUTO_ID_KEY, id,
                        "validacao_verificacoes_quantidade", quantidadeVerificacoes,
                        "validacao_respostas_formulario_quantidade", quantidadeRespostas,
                        "simulador_habilitado", simuladorHabilitado));

        return casoDeUso.executar(comando)
                .invoke(resposta -> ObservabilityLog.info(
                        LOG,
                        "simtr-hub.dossie-produto.validacao-negocial.service.concluido",
                        ObservabilityLog.fields(
                                CAMADA_KEY, CAMADA,
                                COMPONENTE_KEY, COMPONENTE,
                                OPERACAO_KEY, OPERACAO,
                                DOSSIE_PRODUTO_ID_KEY, id,
                                "resultado", "sucesso")))
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.validacao-negocial.service.falhou",
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

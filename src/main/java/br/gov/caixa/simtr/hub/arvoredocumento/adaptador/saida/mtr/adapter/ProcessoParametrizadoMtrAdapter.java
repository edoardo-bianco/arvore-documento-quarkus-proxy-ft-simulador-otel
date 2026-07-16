package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao.qualificador.ProcessoMtr;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.client.ParametrizacaoProcessoClient;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.erro.ProcessoParametrizadoMtrException;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.mapper.ProcessoParametrizadoMtrMapper;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
@ProcessoMtr
public class ProcessoParametrizadoMtrAdapter implements ObterProcessoParametrizado {

    private static final Logger LOG = Logger.getLogger(ProcessoParametrizadoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "ParametrizacaoProcessoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String PARAMETRIZACAO = "simtr-parametrizacao";
    private static final String OPERACAO = "operacao";
    private static final String CONSULTAR = "consultar-processo-parametrizacao-v2";
    private static final String PACOTE_EXCECAO_MTR_LEGADA =
            "br.gov.caixa.simtr.hub.arquitetura.excecao.";

    private final ParametrizacaoProcessoClient client;
    private final ProcessoParametrizadoMtrMapper mapper;

    @Inject
    public ProcessoParametrizadoMtrAdapter(
            @RestClient ParametrizacaoProcessoClient client,
            ProcessoParametrizadoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.parametrizacao.processo.consultar", kind = SpanKind.CLIENT)
    public Uni<ProcessoParametrizado> obter(IdentificadorNegocialProcesso identificador) {
        Long id = identificador.valor();
        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-parametrizacao");
        span.setAttribute("mtr.api", "patriarca-processo-v2");
        span.setAttribute("http.request.method", "GET");
        span.setAttribute(
                "url.path",
                "/simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}"
        );
        if (id != null) {
            span.setAttribute("mtr.parametrizacao.processo.identificador_negocial", id);
        }

        ObservabilityLog.info(
                LOG,
                "mtr.parametrizacao.processo.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, PARAMETRIZACAO, OPERACAO, CONSULTAR,
                        "identificador_negocial", id
                )
        );

        return client.consultarPorIdentificadorNegocial(id)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null) {
                        if (resposta.identificadorNegocial() != null) {
                            span.setAttribute(
                                    "processo.identificador_negocial",
                                    resposta.identificadorNegocial()
                            );
                        }
                        if (resposta.nome() != null) {
                            span.setAttribute("processo.nome", resposta.nome());
                        }
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.parametrizacao.processo.chamada.concluida",
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, PARAMETRIZACAO, OPERACAO, CONSULTAR,
                                    "identificador_negocial", id,
                                    "processo_nome", resposta != null ? resposta.nome() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", tipoErroTelemetria(erro));

                    ObservabilityLog.error(
                            LOG,
                            "mtr.parametrizacao.processo.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, PARAMETRIZACAO, OPERACAO, CONSULTAR,
                                    "identificador_negocial", id,
                                    "erro_tipo", tipoErroSimplesTelemetria(erro),
                                    "resultado", "erro"
                            )
                    );
                })
                .map(mapper::paraDominio)
                .onFailure().transform(ProcessoParametrizadoMtrAdapter::traduzir);
    }

    private static String tipoErroTelemetria(Throwable erro) {
        if (erro instanceof ProcessoParametrizadoMtrException.Negocio) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrBusinessErrorException";
        }
        if (erro instanceof ProcessoParametrizadoMtrException.TecnicaCliente) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrClientTechnicalException";
        }
        if (erro instanceof ProcessoParametrizadoMtrException.Servidor) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrServerErrorException";
        }
        return erro.getClass().getName();
    }

    private static String tipoErroSimplesTelemetria(Throwable erro) {
        String nomeCompleto = tipoErroTelemetria(erro);
        return nomeCompleto.substring(nomeCompleto.lastIndexOf('.') + 1);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof ProcessoParametrizadoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaConsultaProcessoParametrizado.Tipo tipo = falha instanceof TimeoutException
                ? FalhaConsultaProcessoParametrizado.Tipo.TIMEOUT
                : FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaConsultaProcessoParametrizado(
                tipo,
                null,
                "simtr-parametrizacao",
                null,
                null,
                null,
                null,
                null,
                falha
        );
    }

    private static FalhaConsultaProcessoParametrizado traduzirMtr(
            ProcessoParametrizadoMtrException falha
    ) {
        ProcessoParametrizadoMtrException.Erro erro = falha.erro();
        FalhaConsultaProcessoParametrizado.Tipo tipo = switch (falha) {
            case ProcessoParametrizadoMtrException.Negocio ignored ->
                    FalhaConsultaProcessoParametrizado.Tipo.NEGOCIO;
            case ProcessoParametrizadoMtrException.TecnicaCliente ignored ->
                    FalhaConsultaProcessoParametrizado.Tipo.TECNICA_CLIENTE;
            case ProcessoParametrizadoMtrException.Servidor ignored ->
                    FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaConsultaProcessoParametrizado(
                tipo,
                falha.status(),
                erro != null ? erro.recurso() : null,
                erro != null ? erro.idErro() : null,
                erro != null ? erro.codigoErro() : null,
                mensagens(erro),
                erro != null ? erro.detalhe() : null,
                erro != null ? erro.stacktrace() : null,
                falha
        );
    }

    private static List<String> mensagens(ProcessoParametrizadoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (ProcessoParametrizadoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }
}

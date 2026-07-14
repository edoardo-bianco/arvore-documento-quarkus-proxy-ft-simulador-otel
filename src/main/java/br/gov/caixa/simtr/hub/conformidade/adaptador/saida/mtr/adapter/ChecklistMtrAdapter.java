package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.qualificador.ChecklistMtr;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.client.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.mapper.ChecklistMtrMapper;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
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
@ChecklistMtr
public class ChecklistMtrAdapter implements ObterChecklist {

    private static final Logger LOG = Logger.getLogger(ChecklistMtrAdapter.class);
    private static final String PACOTE_EXCECAO_MTR_LEGADA =
            "br.gov.caixa.simtr.hub.arquitetura.excecao.";

    private final ParametrizacaoChecklistClient client;
    private final ChecklistMtrMapper mapper;

    @Inject
    public ChecklistMtrAdapter(
            @RestClient ParametrizacaoChecklistClient client,
            ChecklistMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.parametrizacao.checklist.consultar", kind = SpanKind.CLIENT)
    public Uni<Checklist> obter(ComandoConsultaChecklist comando) {
        Long identificador = comando.identificadorNegocial();
        Integer versao = comando.versao();
        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-parametrizacao");
        span.setAttribute("mtr.api", "cadastro-checklist-v1");
        span.setAttribute("http.request.method", "GET");
        span.setAttribute(
                "url.path",
                "/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/"
                        + "{identificador}/versao/{versao}"
        );
        if (identificador != null) {
            span.setAttribute("mtr.parametrizacao.checklist.identificador_negocial", identificador);
        }
        if (versao != null) {
            span.setAttribute("mtr.parametrizacao.checklist.versao", versao.longValue());
        }

        ObservabilityLog.info(
                LOG,
                "mtr.parametrizacao.checklist.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "ParametrizacaoChecklistGateway",
                        "dependencia", "simtr-parametrizacao",
                        "operacao", "consultar-checklist-parametrizacao-v1",
                        "identificador_negocial", identificador,
                        "versao", versao
                )
        );

        return client.consultarPorIdentificadorNegocialEVersao(identificador, versao)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null) {
                        if (resposta.identificadorNegocial() != null) {
                            span.setAttribute(
                                    "checklist.identificador_negocial",
                                    resposta.identificadorNegocial()
                            );
                        }
                        if (resposta.nome() != null) {
                            span.setAttribute("checklist.nome", resposta.nome());
                        }
                        if (resposta.apontamentos() != null) {
                            span.setAttribute(
                                    "checklist.apontamentos.quantidade",
                                    resposta.apontamentos().size()
                            );
                        }
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.parametrizacao.checklist.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "ParametrizacaoChecklistGateway",
                                    "dependencia", "simtr-parametrizacao",
                                    "operacao", "consultar-checklist-parametrizacao-v1",
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "checklist_nome", resposta != null ? resposta.nome() : null,
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
                            "mtr.parametrizacao.checklist.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "ParametrizacaoChecklistGateway",
                                    "dependencia", "simtr-parametrizacao",
                                    "operacao", "consultar-checklist-parametrizacao-v1",
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "erro_tipo", tipoErroSimplesTelemetria(erro),
                                    "resultado", "erro"
                            )
                    );
                })
                .map(mapper::paraDominio)
                .onFailure().transform(ChecklistMtrAdapter::traduzir);
    }

    private static String tipoErroTelemetria(Throwable erro) {
        if (erro instanceof ChecklistMtrException.Negocio) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrBusinessErrorException";
        }
        if (erro instanceof ChecklistMtrException.TecnicaCliente) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrClientTechnicalException";
        }
        if (erro instanceof ChecklistMtrException.Servidor) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrServerErrorException";
        }
        return erro.getClass().getName();
    }

    private static String tipoErroSimplesTelemetria(Throwable erro) {
        String nomeCompleto = tipoErroTelemetria(erro);
        return nomeCompleto.substring(nomeCompleto.lastIndexOf('.') + 1);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof ChecklistMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaConsultaChecklist.Tipo tipo = falha instanceof TimeoutException
                ? FalhaConsultaChecklist.Tipo.TIMEOUT
                : FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaConsultaChecklist(
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

    private static FalhaConsultaChecklist traduzirMtr(ChecklistMtrException falha) {
        ChecklistMtrException.Erro erro = falha.erro();
        FalhaConsultaChecklist.Tipo tipo = switch (falha) {
            case ChecklistMtrException.Negocio ignored -> FalhaConsultaChecklist.Tipo.NEGOCIO;
            case ChecklistMtrException.TecnicaCliente ignored ->
                    FalhaConsultaChecklist.Tipo.TECNICA_CLIENTE;
            case ChecklistMtrException.Servidor ignored ->
                    FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaConsultaChecklist(
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

    private static List<String> mensagens(ChecklistMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (ChecklistMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }
}

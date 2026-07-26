package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.IniciarAnaliseConformidadeRequest;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.IniciarAnaliseConformidadeResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.ParecerConformidadeDto;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.ResultadoAnaliseConformidadeResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.ResultadoApontamentoConformidadeResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.RevisaoAnaliseConformidadeRequest;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.StatusAnaliseConformidadeDto;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.VisaoAnaliseConformidadeResponse;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.UUID;

@Provider
public class AnaliseConformidadeRestMapper
        implements ExceptionMapper<FalhaAnaliseConformidade> {

    static SolicitacaoAnaliseConformidade paraSolicitacao(
            IniciarAnaliseConformidadeRequest request) {
        if (request == null) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "A solicitação da análise é obrigatória");
        }
        return SolicitacaoAnaliseConformidade.nova(
                request.identificadorDocumento(),
                request.texto(),
                request.identificadorChecklist(),
                request.versaoChecklist());
    }

    static RevisaoHumanaConformidade paraRevisao(
            RevisaoAnaliseConformidadeRequest request) {
        if (request == null) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "A revisão da análise é obrigatória");
        }
        List<ResultadoApontamentoConformidade> apontamentos =
                request.apontamentos().stream()
                        .map(item -> new ResultadoApontamentoConformidade(
                                item.identificadorApontamento(),
                                item.nomeApontamento(),
                                ParecerConformidade.valueOf(item.parecer().name()),
                                item.justificativa(),
                                item.evidencia(),
                                item.confianca()))
                        .toList();
        return new RevisaoHumanaConformidade(request.observacao(), apontamentos);
    }

    static IniciarAnaliseConformidadeResponse paraInicio(
            VisaoAnaliseConformidade visao) {
        return new IniciarAnaliseConformidadeResponse(
                visao.correlationId(),
                visao.instanceId(),
                visao.identificadorDocumento(),
                visao.identificadorChecklist(),
                visao.versaoChecklist(),
                StatusAnaliseConformidadeDto.valueOf(visao.status().name()));
    }

    static VisaoAnaliseConformidadeResponse paraVisao(
            VisaoAnaliseConformidade visao) {
        return new VisaoAnaliseConformidadeResponse(
                visao.correlationId(),
                visao.instanceId(),
                visao.identificadorDocumento(),
                visao.identificadorChecklist(),
                visao.versaoChecklist(),
                StatusAnaliseConformidadeDto.valueOf(visao.status().name()),
                paraResultado(visao.resultadoPreliminar()),
                paraResultado(visao.resultadoFinal()),
                visao.mensagemErro());
    }

    @Override
    public Response toResponse(FalhaAnaliseConformidade falha) {
        ErroHttp erroHttp = erroHttp(falha.tipo());
        ErroPadraoDto corpo = new ErroPadraoDto(
                erroHttp.status(),
                "simtr-hub",
                UUID.randomUUID().toString(),
                erroHttp.codigo(),
                List.of(new ErroMensagemDto(falha.getMessage())),
                null,
                null);
        return Response.status(erroHttp.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }

    private static ResultadoAnaliseConformidadeResponse paraResultado(
            ResultadoAnaliseConformidade resultado) {
        if (resultado == null) {
            return null;
        }
        return new ResultadoAnaliseConformidadeResponse(
                resultado.identificadorChecklist(),
                resultado.versaoChecklist(),
                resultado.nomeChecklist(),
                resultado.resumo(),
                resultado.apontamentos().stream()
                        .map(AnaliseConformidadeRestMapper::paraApontamento)
                        .toList());
    }

    private static ResultadoApontamentoConformidadeResponse paraApontamento(
            ResultadoApontamentoConformidade apontamento) {
        return new ResultadoApontamentoConformidadeResponse(
                apontamento.identificadorApontamento(),
                apontamento.nomeApontamento(),
                ParecerConformidadeDto.valueOf(apontamento.parecer().name()),
                apontamento.justificativa(),
                apontamento.evidencia(),
                apontamento.confianca());
    }

    private static ErroHttp erroHttp(FalhaAnaliseConformidade.Tipo tipo) {
        return switch (tipo) {
            case SOLICITACAO_INVALIDA ->
                    new ErroHttp(Response.Status.BAD_REQUEST.getStatusCode(), "ARVDOCP0001");
            case INSTANCIA_NAO_ENCONTRADA ->
                    new ErroHttp(Response.Status.NOT_FOUND.getStatusCode(), "ARVDOCP1002");
            case TRANSICAO_INVALIDA ->
                    new ErroHttp(Response.Status.CONFLICT.getStatusCode(), "ARVDOCP1003");
            case CHECKLIST_INVALIDO, RESULTADO_INVALIDO, REVISAO_INCONSISTENTE ->
                    new ErroHttp(422, "ARVDOCP1004");
            case INDISPONIBILIDADE_TECNICA ->
                    new ErroHttp(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), "ARVDOCP1005");
        };
    }

    private record ErroHttp(int status, String codigo) {
    }
}

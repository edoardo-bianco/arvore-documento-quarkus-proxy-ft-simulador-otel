package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.CriacaoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AtualizarFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CriarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IncluirDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.RegistrarValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ValidacaoNegocialDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.InclusaoDocumentoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.InclusaoDocumentoDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IniciarOuAvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;

@Path("/simtr-hub/v1/dossie-produto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Negócio - Dossiê Produto", description = "Proxy da API de Apoio ao Negócio de Dossiês Produto do SIMTR")
public class DossieProdutoResource {

    private static final Logger LOG = Logger.getLogger(DossieProdutoResource.class);
    private static final String CAMADA = "api";
    private static final String COMPONENTE = "DossieProdutoResource";
    private static final String CAMADA_KEY = "camada";
    private static final String COMPONENTE_KEY = "componente";

    private final CriarDossieProduto criarDossieProduto;
    private final AtualizarFormularioDossieProduto atualizarFormularioDossieProduto;
    private final IncluirDocumentoDossieProduto incluirDocumentoDossieProduto;
    private final IniciarOuAvancarWorkflowDossieProduto iniciarOuAvancarWorkflow;
    private final RegistrarValidacaoNegocialDossieProduto registrarValidacaoNegocial;

    @Inject
    public DossieProdutoResource(CriarDossieProduto criarDossieProduto,
                                 AtualizarFormularioDossieProduto atualizarFormularioDossieProduto,
                                 IncluirDocumentoDossieProduto incluirDocumentoDossieProduto,
                                 IniciarOuAvancarWorkflowDossieProduto iniciarOuAvancarWorkflow,
                                 RegistrarValidacaoNegocialDossieProduto registrarValidacaoNegocial) {
        this.criarDossieProduto = criarDossieProduto;
        this.atualizarFormularioDossieProduto = atualizarFormularioDossieProduto;
        this.incluirDocumentoDossieProduto = incluirDocumentoDossieProduto;
        this.iniciarOuAvancarWorkflow = iniciarOuAvancarWorkflow;
        this.registrarValidacaoNegocial = registrarValidacaoNegocial;
    }

    @POST
    @WithSpan(value = "simtr-hub.api.dossie-produto.criar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Cria dossiê de produto em modo rascunho",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o serviço de aplicação e cria o dossiê de produto no simtr-dossie-produto."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Dossiê de Produto criado com sucesso.",
                    content = @Content(schema = @Schema(implementation = DossieProdutoCriadoDto.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Não autorizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Canal ou usuário sem permissão para criar o Dossiê de Produto.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Parâmetro não localizado para a criação do Dossiê de Produto.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflito ao processar a requisição.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<Response> criarDossieProduto(
            @NotNull(message = "O corpo da requisicao deve ser informado.")
            @Valid CriacaoDossieProdutoRequest requisicao) {

        Long processo = processo(requisicao);
        Long chaveCorrelacaoCanal = chaveCorrelacaoCanal(requisicao);
        Integer quantidadeClientes = quantidadeClientes(requisicao);

        Span span = Span.current();
        span.setAttribute("http.route", "/simtr-hub/v1/dossie-produto");
        span.setAttribute("simtr_hub.api", "dossie-produto-v1");
        setLongAttribute(span, "dossie_produto.processo", processo);
        setLongAttribute(span, "dossie_produto.chave_correlacao_canal", chaveCorrelacaoCanal);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", quantidadeClientes);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.requisicao.recebida",
                ObservabilityLog.fields(
                        CAMADA_KEY, CAMADA,
                        COMPONENTE_KEY, COMPONENTE,
                        "operacao", "criar-dossie-produto",
                        "processo", processo,
                        "chave_correlacao_canal", chaveCorrelacaoCanal,
                        "clientes_quantidade", quantidadeClientes
                )
        );

        return criarDossieProduto.executar(CriacaoDossieProdutoRestMapper.paraComando(requisicao))
                .onFailure(FalhaCriacaoDossieProduto.class)
                .transform(erro -> CriacaoDossieProdutoRestMapper.paraExcecaoRest(
                        (FalhaCriacaoDossieProduto) erro))
                .map(CriacaoDossieProdutoRestMapper::paraResposta)
                .invoke(resposta -> {
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.dossie-produto.resposta.enviada",
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    "operacao", "criar-dossie-produto",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "dossie_produto_id", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .map(resposta -> Response.status(Response.Status.CREATED)
                        .entity(resposta)
                        .build())
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    "operacao", "criar-dossie-produto",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    @PATCH
    @Path("/{id}/formulario")
    @WithSpan(value = "simtr-hub.api.dossie-produto.formulario.atualizar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Inclui ou edita respostas de formulário no dossiê de produto",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o serviço de aplicação e atualiza respostas de formulário no simtr-dossie-produto."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Inclusão ou edição de Respostas de Formulário no Dossiê de Produto feita com sucesso.",
                    content = @Content(schema = @Schema(implementation = DossieProdutoCriadoDto.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Não autorizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Canal ou usuário sem permissão para atualizar o formulário do Dossiê de Produto.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Dossiê de Produto não localizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflito ao processar a requisição.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<Response> atualizarFormularioDossieProduto(
            @PathParam("id")
            @NotNull(message = "O identificador do dossie produto deve ser informado.")
            @Min(value = 1, message = "O identificador do dossie produto deve ser maior que zero.")
            Long id,
            @NotNull(message = "O corpo da requisicao deve ser informado.")
            List<@Valid DossieProdutoFormularioDto> requisicao) {

        Integer quantidadeVinculos = quantidadeVinculosFormulario(requisicao);
        Integer quantidadeRespostas = quantidadeRespostasFormulario(requisicao);

        Span span = Span.current();
        span.setAttribute("http.route", "/simtr-hub/v1/dossie-produto/{id}/formulario");
        span.setAttribute("simtr_hub.api", "dossie-produto-v1");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.formulario.vinculos.quantidade", quantidadeVinculos);
        setIntAttribute(span, "dossie_produto.formulario.respostas.quantidade", quantidadeRespostas);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.formulario.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", "atualizar-formulario-dossie-produto",
                        "dossie_produto_id", id,
                        "formulario_vinculos_quantidade", quantidadeVinculos,
                        "formulario_respostas_quantidade", quantidadeRespostas
                )
        );

        return atualizarFormularioDossieProduto.executar(
                        FormularioDossieProdutoRestMapper.paraComando(id, requisicao))
                .onFailure(FalhaAtualizacaoFormularioDossieProduto.class)
                .transform(erro -> FormularioDossieProdutoRestMapper.paraExcecaoRest(
                        (FalhaAtualizacaoFormularioDossieProduto) erro))
                .map(FormularioDossieProdutoRestMapper::paraResposta)
                .invoke(resposta -> {
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id_resposta", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.dossie-produto.formulario.resposta.enviada",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "atualizar-formulario-dossie-produto",
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .map(resposta -> Response.status(Response.Status.CREATED)
                        .entity(resposta)
                        .build())
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.formulario.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "atualizar-formulario-dossie-produto",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    @POST
    @Path("/{id}/documento")
    @WithSpan(value = "simtr-hub.api.dossie-produto.documento.incluir", kind = SpanKind.SERVER)
    @Operation(
            summary = "Inclui documento no dossiê de produto",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o serviço de aplicação e vincula documento ao dossiê de produto no simtr-dossie-produto v2."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Documento criado com sucesso.",
                    content = @Content(schema = @Schema(implementation = InclusaoDocumentoDossieProdutoResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Não autorizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Canal ou usuário sem permissão para criar Documentos em Dossiê.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Parâmetro não localizado para a criação de Documento.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflito ao processar a requisição.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<Response> incluirDocumentoDossieProduto(
            @PathParam("id")
            @NotNull(message = "O identificador do dossie produto deve ser informado.")
            @Min(value = 1, message = "O identificador do dossie produto deve ser maior que zero.")
            Long id,
            @NotNull(message = "O corpo da requisicao deve ser informado.")
            @Valid InclusaoDocumentoDossieProdutoRequest requisicao) {

        Integer quantidadeAtributos = quantidadeAtributosDocumento(requisicao);
        Integer quantidadePropriedades = quantidadePropriedadesDocumento(requisicao);
        String tipoDocumento = tipoDocumento(requisicao);

        Span span = Span.current();
        span.setAttribute("http.route", "/simtr-hub/v1/dossie-produto/{id}/documento");
        span.setAttribute("simtr_hub.api", "dossie-produto-v2");
        setLongAttribute(span, "dossie_produto.id", id);
        setStringAttribute(span, "dossie_produto.documento.tipo", tipoDocumento);
        setIntAttribute(span, "dossie_produto.documento.atributos.quantidade", quantidadeAtributos);
        setIntAttribute(span, "dossie_produto.documento.propriedades.quantidade", quantidadePropriedades);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.documento.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", "incluir-documento-dossie-produto",
                        "dossie_produto_id", id,
                        "tipo_documento", tipoDocumento,
                        "documento_atributos_quantidade", quantidadeAtributos,
                        "documento_propriedades_quantidade", quantidadePropriedades
                )
        );

        return incluirDocumentoDossieProduto.executar(
                        DocumentoDossieProdutoRestMapper.paraComando(id, requisicao))
                .onFailure(FalhaInclusaoDocumentoDossieProduto.class)
                .transform(erro -> DocumentoDossieProdutoRestMapper.paraExcecaoRest(
                        (FalhaInclusaoDocumentoDossieProduto) erro))
                .map(DocumentoDossieProdutoRestMapper::paraResposta)
                .invoke(resposta -> {
                    if (resposta != null && resposta.idDocumento() != null) {
                        span.setAttribute("dossie_produto.documento.id", resposta.idDocumento());
                    }
                    if (resposta != null && resposta.idInstanciaDocumento() != null) {
                        span.setAttribute("dossie_produto.documento.instancia.id", resposta.idInstanciaDocumento());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.dossie-produto.documento.resposta.enviada",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "incluir-documento-dossie-produto",
                                    "dossie_produto_id", id,
                                    "id_documento", resposta != null ? resposta.idDocumento() : null,
                                    "id_instancia_documento", resposta != null ? resposta.idInstanciaDocumento() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .map(resposta -> Response.status(Response.Status.CREATED)
                        .entity(resposta)
                        .build())
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.documento.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "incluir-documento-dossie-produto",
                                    "dossie_produto_id", id,
                                    "tipo_documento", tipoDocumento,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    @PATCH
    @Path("/{id}/validacao-negocial")
    @WithSpan(value = "simtr-hub.api.dossie-produto.validacao-negocial.registrar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Registra validacao negocial no dossie de produto",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o servico de aplicacao e registra a validacao negocial no simtr-dossie-produto v1."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verificacao do dossie aplicada com sucesso."
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisicao invalida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Nao autorizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Canal ou usuario sem permissao.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Recurso nao localizado para os parametros informados.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflito ao processar a requisicao.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "503",
                    description = "Servico de terceiros indisponivel no momento.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<Response> registrarValidacaoNegocialDossieProduto(
            @PathParam("id")
            @NotNull(message = "O identificador do dossie produto deve ser informado.")
            @Min(value = 1, message = "O identificador do dossie produto deve ser maior que zero.")
            Long id,
            @NotNull(message = "O corpo da requisicao deve ser informado.")
            @Valid ValidacaoNegocialDossieProdutoRequest requisicao) {

        Integer quantidadeVerificacoes = quantidadeVerificacoesValidacao(requisicao);
        Integer quantidadeRespostasFormulario = quantidadeRespostasFormularioValidacao(requisicao);

        Span span = Span.current();
        span.setAttribute("http.route", "/simtr-hub/v1/dossie-produto/{id}/validacao-negocial");
        span.setAttribute("simtr_hub.api", "dossie-produto-v1");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.validacao.verificacoes.quantidade", quantidadeVerificacoes);
        setIntAttribute(span, "dossie_produto.validacao.respostas_formulario.quantidade", quantidadeRespostasFormulario);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.validacao-negocial.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", "registrar-validacao-negocial-dossie-produto",
                        "dossie_produto_id", id,
                        "validacao_verificacoes_quantidade", quantidadeVerificacoes,
                        "validacao_respostas_formulario_quantidade", quantidadeRespostasFormulario
                )
        );

        return registrarValidacaoNegocial.executar(
                        ValidacaoNegocialDossieProdutoRestMapper.paraComando(id, requisicao))
                .onFailure(FalhaRegistroValidacaoNegocialDossieProduto.class)
                .transform(erro -> ValidacaoNegocialDossieProdutoRestMapper.paraExcecaoRest(
                        (FalhaRegistroValidacaoNegocialDossieProduto) erro))
                .invoke(resposta -> ObservabilityLog.info(
                        LOG,
                        "simtr-hub.dossie-produto.validacao-negocial.resposta.enviada",
                        ObservabilityLog.fields(
                                "camada", CAMADA,
                                "componente", COMPONENTE,
                                "operacao", "registrar-validacao-negocial-dossie-produto",
                                "dossie_produto_id", id,
                                "resultado", "sucesso"
                        )
                ))
                .replaceWith(Response.ok().build())
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.validacao-negocial.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "registrar-validacao-negocial-dossie-produto",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    @POST
    @Path("/{id}/workflow")
    @Consumes(MediaType.WILDCARD)
    @WithSpan(value = "simtr-hub.api.dossie-produto.workflow.avancar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Inicia ou avança o workflow do dossiê de produto",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o serviço de aplicação e inicia ou avança o fluxo do dossiê de produto no simtr-dossie-produto v1."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Fluxo iniciado ou avançado com sucesso.",
                    content = @Content(schema = @Schema(implementation = DossieProdutoCriadoDto.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Não autorizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Canal ou usuário sem permissão.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Parâmetro não localizado para o avanço de fluxo no Dossiê de Produto.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflito ao processar a requisição.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<Response> iniciarOuAvancarWorkflowDossieProduto(
            @PathParam("id")
            @NotNull(message = "O identificador do dossie produto deve ser informado.")
            @Min(value = 1, message = "O identificador do dossie produto deve ser maior que zero.")
            Long id) {

        Span span = Span.current();
        span.setAttribute("http.route", "/simtr-hub/v1/dossie-produto/{id}/workflow");
        span.setAttribute("simtr_hub.api", "dossie-produto-v1");
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.workflow.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                        "dossie_produto_id", id
                )
        );

        return iniciarOuAvancarWorkflow.executar(new IdentificadorDossieProduto(id))
                .onFailure(FalhaWorkflowDossieProduto.class)
                .transform(erro -> WorkflowDossieProdutoRestMapper.paraExcecaoRest(
                        (FalhaWorkflowDossieProduto) erro))
                .map(WorkflowDossieProdutoRestMapper::paraResposta)
                .invoke(resposta -> {
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.workflow.id_resposta", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.dossie-produto.workflow.resposta.enviada",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .map(resposta -> Response.ok(resposta).build())
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.workflow.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private static Long processo(CriacaoDossieProdutoRequest requisicao) {
        return requisicao != null ? requisicao.processo() : null;
    }

    private static Long chaveCorrelacaoCanal(CriacaoDossieProdutoRequest requisicao) {
        return requisicao != null ? requisicao.chaveCorrelacaoCanal() : null;
    }

    private static Integer quantidadeClientes(CriacaoDossieProdutoRequest requisicao) {
        if (requisicao == null || requisicao.clientes() == null) {
            return null;
        }
        return requisicao.clientes().size();
    }

    private static Integer quantidadeVinculosFormulario(List<DossieProdutoFormularioDto> requisicao) {
        if (requisicao == null) {
            return null;
        }
        return requisicao.size();
    }

    private static Integer quantidadeRespostasFormulario(List<DossieProdutoFormularioDto> requisicao) {
        if (requisicao == null) {
            return null;
        }
        return requisicao.stream()
                .filter(Objects::nonNull)
                .map(DossieProdutoFormularioDto::vinculoDossie)
                .filter(Objects::nonNull)
                .map(vinculo -> vinculo.respostasFormulario())
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private static String tipoDocumento(InclusaoDocumentoDossieProdutoRequest requisicao) {
        return requisicao != null ? requisicao.tipoDocumento() : null;
    }

    private static Integer quantidadeAtributosDocumento(
            InclusaoDocumentoDossieProdutoRequest requisicao) {
        if (requisicao == null || requisicao.atributos() == null) {
            return null;
        }
        return requisicao.atributos().size();
    }

    private static Integer quantidadePropriedadesDocumento(
            InclusaoDocumentoDossieProdutoRequest requisicao) {
        if (requisicao == null || requisicao.propriedades() == null) {
            return null;
        }
        return requisicao.propriedades().size();
    }

    private static Integer quantidadeVerificacoesValidacao(ValidacaoNegocialDossieProdutoRequest requisicao) {
        if (requisicao == null || requisicao.verificacoes() == null) {
            return null;
        }
        return requisicao.verificacoes().size();
    }

    private static Integer quantidadeRespostasFormularioValidacao(ValidacaoNegocialDossieProdutoRequest requisicao) {
        if (requisicao == null || requisicao.respostasFormulario() == null) {
            return null;
        }
        return requisicao.respostasFormulario().size();
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

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}

package br.gov.caixa.simtr.hub;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteRelacionadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.CriacaoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoAtributoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoPropriedadeDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoProdutoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialParecerApontamentoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialProdutoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialVerificacaoDto;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistApontamentoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.CampoFormularioDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ChecklistReferenciaDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.DocumentoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.FaseDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.FuncaoDocumentalDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.GarantiaDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.MacroprocessoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.OpcaoDisponivelDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProdutoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.RelacionamentoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.TipoDocumentoDto;

import java.util.List;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static ProcessoDto processoDto() {
        ChecklistReferenciaDto checklist = new ChecklistReferenciaDto(700L, 1);
        OpcaoDisponivelDto opcao = new OpcaoDisponivelDto("SIM", "Sim", true);
        CampoFormularioDto campoFormulario = new CampoFormularioDto(
                600L,
                "Campo formulario",
                true,
                true,
                "cliente.tipo == PF",
                12,
                1,
                "TEXTO",
                "99999999999",
                "Informe o valor",
                1,
                50,
                "Orientacao de preenchimento",
                false,
                List.of(opcao)
        );
        TipoDocumentoDto tipoDocumento = new TipoDocumentoDto(
                "RG",
                "Registro Geral",
                true,
                true,
                true,
                checklist
        );
        FuncaoDocumentalDto funcaoDocumental = new FuncaoDocumentalDto(
                "Identificacao",
                List.of(tipoDocumento),
                checklist
        );
        DocumentoDto documento = new DocumentoDto(funcaoDocumental, tipoDocumento, true);
        GarantiaDto garantia = new GarantiaDto(
                10L,
                "Garantia fidejussoria",
                true,
                List.of(campoFormulario),
                List.of(documento),
                checklist
        );
        ProdutoDto produto = new ProdutoDto(
                100L,
                200L,
                "Produto Habitacional",
                List.of(campoFormulario),
                List.of(documento),
                List.of(garantia),
                checklist
        );
        RelacionamentoDto relacionamento = new RelacionamentoDto(
                300L,
                "Titular",
                "PF",
                true,
                true,
                false,
                true,
                List.of(campoFormulario),
                List.of(documento)
        );
        FaseDto fase = new FaseDto(
                400L,
                "Contratacao",
                true,
                "01/01/2026 10:00:00",
                1,
                "Orientacao da fase",
                List.of(produto),
                List.of(garantia),
                List.of(campoFormulario),
                List.of(documento),
                List.of(checklist)
        );

        return new ProcessoDto(
                1000016487L,
                "Concessao Habitacional",
                true,
                "01/01/2026 10:00:00",
                false,
                new MacroprocessoDto(900L, "Macro Habitacional", true, "01/01/2026 09:00:00"),
                List.of(relacionamento),
                List.of(produto),
                List.of(fase),
                List.of(documento),
                checklist
        );
    }

    public static ChecklistDto checklistDto() {
        return new ChecklistDto(
                "Checklist Habitacional",
                1000012583L,
                1,
                "01/01/2026 10:00:00",
                "02/01/2026 10:00:00",
                true,
                "Orientacao",
                List.of(new ChecklistApontamentoDto(
                        10L,
                        "Apontamento",
                        "Descricao",
                        "Orientacao operador",
                        true,
                        1
                ))
        );
    }

    public static CriacaoDossieProdutoRequest criacaoDossieProdutoRequest() {
        return new CriacaoDossieProdutoRequest(
                100L,
                200L,
                300L,
                List.of(new DossieProdutoClienteDto(
                        "12345678901",
                        "12345678000190",
                        1L,
                        new DossieProdutoClienteRelacionadoDto("98765432100", null),
                        1
                ))
        );
    }

    public static List<DossieProdutoFormularioDto> formularioDto() {
        return List.of(new DossieProdutoFormularioDto(
                new DossieProdutoVinculoDossieDto(
                        10L,
                        new DossieProdutoVinculoClienteDto("12345678901", null, 1L),
                        new DossieProdutoVinculoProdutoDto(100, 200),
                        new DossieProdutoVinculoGarantiaDto(
                                300,
                                400,
                                500,
                                List.of(new DossieProdutoClienteAvalistaDto(null, "12345678000190"))
                        ),
                        List.of(new DossieProdutoRespostaFormularioDto(
                                600L,
                                "resposta",
                                List.of("opcao-1"),
                                true
                        ))
                )
        ));
    }

    public static DossieProdutoDocumentoInclusaoDto documentoInclusaoDto() {
        return new DossieProdutoDocumentoInclusaoDto(
                null,
                "container/documento.pdf",
                "GED123",
                "OBJECT_STORE",
                "RG",
                new DossieProdutoDocumentoVinculoDossieDto(
                        new DossieProdutoDocumentoClienteDto("12345678901", null, 1L),
                        700L,
                        new DossieProdutoDocumentoGarantiaDto(
                                300,
                                400,
                                500,
                                List.of(new DossieProdutoDocumentoClienteDto(null, "12345678000190", null))
                        )
                ),
                List.of(new DossieProdutoDocumentoAtributoDto(
                        "numero",
                        "12345",
                        "documento",
                        List.of("opcao-1")
                )),
                List.of(new DossieProdutoDocumentoPropriedadeDto(
                        "origem",
                        "pre-validacao",
                        "documento"
                ))
        );
    }

    public static DossieProdutoDocumentoInclusaoDto documentoInclusaoSemObjetoAtributoDto() {
        return new DossieProdutoDocumentoInclusaoDto(
                null,
                "cli-web-mtr/TESTE_DEFENDER_01_20251104.pdf",
                null,
                null,
                "0001000100030024",
                new DossieProdutoDocumentoVinculoDossieDto(
                        new DossieProdutoDocumentoClienteDto("51081563672", null, 1000009995L),
                        null,
                        null
                ),
                List.of(
                        new DossieProdutoDocumentoAtributoDto("numero_recibo", "0012", null, null),
                        new DossieProdutoDocumentoAtributoDto("nome", "MARIANA RODRIGO FERREIRA GOMES", null, null),
                        new DossieProdutoDocumentoAtributoDto("cpf", "00072169125", null, null)
                ),
                null
        );
    }

    public static DossieProdutoValidacaoNegocialDto validacaoNegocialDto() {
        return new DossieProdutoValidacaoNegocialDto(
                List.of(new DossieProdutoValidacaoNegocialVerificacaoDto(
                        1122928L,
                        6592L,
                        2,
                        true,
                        List.of(
                                new DossieProdutoValidacaoNegocialParecerApontamentoDto(
                                        1000012877L,
                                        "APROVADO",
                                        "apontamento aprovado",
                                        false,
                                        1.0
                                ),
                                new DossieProdutoValidacaoNegocialParecerApontamentoDto(
                                        1000011696L,
                                        "APROVADO",
                                        null,
                                        true,
                                        0.7
                                ),
                                new DossieProdutoValidacaoNegocialParecerApontamentoDto(
                                        1000011695L,
                                        "INCONCLUSIVO",
                                        "necessita revisao",
                                        true,
                                        0.3
                                )
                        ),
                        new DossieProdutoValidacaoNegocialGarantiaDto(
                                300,
                                List.of(new DossieProdutoValidacaoNegocialClienteAvalistaDto(
                                        "12345678901",
                                        null
                                ))
                        ),
                        new DossieProdutoValidacaoNegocialProdutoDto(100, 200),
                        true
                )),
                List.of(
                        new DossieProdutoValidacaoNegocialRespostaFormularioDto(
                                1000011689L,
                                "teste",
                                null
                        ),
                        new DossieProdutoValidacaoNegocialRespostaFormularioDto(
                                1000011699L,
                                null,
                                List.of("2")
                        )
                )
        );
    }

    public static GestaoDocumentoCredencialContainerDto gestaoDocumentoCredencialContainerDto() {
        return new GestaoDocumentoCredencialContainerDto(
                "sv=mock&ss=b&srt=o&sp=rw&se=2026-07-10T18:00:00Z&sig=mock",
                "10/07/2026 18:00:00",
                "https://dossiedigitaldes.blob.core.windows.net",
                "pre-validacao"
        );
    }
}

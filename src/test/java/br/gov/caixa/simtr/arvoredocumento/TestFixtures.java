package br.gov.caixa.simtr.arvoredocumento;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteAvalistaDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteRelacionadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoRespostaFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoClienteDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoProdutoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistApontamentoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.CampoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ChecklistReferenciaDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.DocumentoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.FaseDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.FuncaoDocumentalDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.GarantiaDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.MacroprocessoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.OpcaoDisponivelDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProdutoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.RelacionamentoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.TipoDocumentoDto;

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

    public static DossieProdutoCriacaoDto dossieCriacaoDto() {
        return new DossieProdutoCriacaoDto(
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
}

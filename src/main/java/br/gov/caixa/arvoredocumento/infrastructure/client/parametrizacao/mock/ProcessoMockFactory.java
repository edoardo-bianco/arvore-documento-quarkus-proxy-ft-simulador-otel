package br.gov.caixa.arvoredocumento.infrastructure.client.parametrizacao.mock;

import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.CampoFormularioDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.ChecklistReferenciaDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.DocumentoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.FaseDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.FuncaoDocumentalDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.GarantiaDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.MacroprocessoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.OpcaoDisponivelDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.ProdutoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.RelacionamentoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.TipoDocumentoDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProcessoMockFactory {

    public ProcessoDto criarProcessoMock(Long identificador) {
        ChecklistReferenciaDto checklist = new ChecklistReferenciaDto(1000006317L, 1);

        CampoFormularioDto campoFormulario = new CampoFormularioDto(
                29848450L,
                "Campo mockado",
                true,
                true,
                null,
                12,
                1,
                "TEXTO",
                null,
                "Informe o valor",
                1,
                100,
                "Campo gerado pelo simulador do arvore-documento.",
                false,
                List.of(new OpcaoDisponivelDto("1", "Opção mockada", true))
        );

        TipoDocumentoDto tipoDocumento = new TipoDocumentoDto(
                "MOCK-TIPO-DOC",
                "Tipo de documento mockado",
                true,
                true,
                checklist
        );

        DocumentoDto documento = new DocumentoDto(
                new FuncaoDocumentalDto(
                        "Função documental mockada",
                        List.of(tipoDocumento),
                        checklist
                ),
                tipoDocumento,
                true
        );

        GarantiaDto garantia = new GarantiaDto(
                999L,
                "Garantia mockada",
                false,
                List.of(campoFormulario),
                List.of(documento),
                checklist
        );

        ProdutoDto produto = new ProdutoDto(
                123L,
                456L,
                "Produto mockado",
                List.of(campoFormulario),
                List.of(documento),
                List.of(garantia),
                checklist
        );

        FaseDto fase = new FaseDto(
                7001L,
                "Fase mockada",
                true,
                "07/07/2026 10:00:00",
                1,
                "Orientação mockada para o usuário.",
                List.of(produto),
                List.of(garantia),
                List.of(campoFormulario),
                List.of(documento),
                checklist
        );

        RelacionamentoDto relacionamento = new RelacionamentoDto(
                8001L,
                "Relacionamento mockado",
                "F",
                true,
                true,
                false,
                false,
                List.of(campoFormulario),
                List.of(documento)
        );

        return new ProcessoDto(
                identificador,
                "Processo mockado pelo simulador do arvore-documento",
                true,
                "07/07/2026 10:00:00",
                new MacroprocessoDto(
                        1000L,
                        "Macroprocesso mockado",
                        true,
                        "07/07/2026 10:00:00"
                ),
                List.of(relacionamento),
                List.of(produto),
                List.of(fase),
                List.of(documento),
                checklist
        );
    }
}

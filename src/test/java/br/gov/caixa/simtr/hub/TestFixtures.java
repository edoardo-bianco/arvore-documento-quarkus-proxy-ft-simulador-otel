package br.gov.caixa.simtr.hub;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteRelacionadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.CriacaoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoAtributoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.InclusaoDocumentoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoPropriedadeDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoProdutoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.ValidacaoNegocialDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialParecerApontamentoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialProdutoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialVerificacaoDto;

import java.util.List;

public final class TestFixtures {

    private TestFixtures() {
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

    public static InclusaoDocumentoDossieProdutoRequest documentoInclusaoDto() {
        return new InclusaoDocumentoDossieProdutoRequest(
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

    public static InclusaoDocumentoDossieProdutoRequest documentoInclusaoSemObjetoAtributoDto() {
        return new InclusaoDocumentoDossieProdutoRequest(
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

    public static ValidacaoNegocialDossieProdutoRequest validacaoNegocialRequest() {
        return new ValidacaoNegocialDossieProdutoRequest(
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

}

package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoProdutoDto;

import java.util.ArrayList;
import java.util.List;

final class FormularioDossieProdutoRestMapper {

    private FormularioDossieProdutoRestMapper() {
    }

    static ComandoAtualizacaoFormularioDossieProduto paraComando(
            Long identificadorDossieProduto,
            List<DossieProdutoFormularioDto> request
    ) {
        return new ComandoAtualizacaoFormularioDossieProduto(
                identificadorDossieProduto, formularios(request));
    }

    static DossieProdutoCriadoDto paraResposta(
            ResultadoAtualizacaoFormularioDossieProduto resultado
    ) {
        if (resultado == null) {
            return null;
        }
        return new DossieProdutoCriadoDto(resultado.identificadorDossieProduto());
    }

    static Throwable paraExcecaoRest(FalhaAtualizacaoFormularioDossieProduto falha) {
        int status = falha.status() != null ? falha.status() : 500;
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagensErro(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno());

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(status, erro);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(status, erro);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro);
        };
    }

    private static List<FormularioDossieProduto> formularios(
            List<DossieProdutoFormularioDto> formularios
    ) {
        if (formularios == null) {
            return null;
        }
        List<FormularioDossieProduto> resultado = new ArrayList<>(formularios.size());
        for (DossieProdutoFormularioDto formulario : formularios) {
            resultado.add(formulario != null
                    ? new FormularioDossieProduto(vinculo(formulario.vinculoDossie()))
                    : null);
        }
        return resultado;
    }

    private static VinculoFormularioDossieProduto vinculo(DossieProdutoVinculoDossieDto vinculo) {
        if (vinculo == null) {
            return null;
        }
        return new VinculoFormularioDossieProduto(
                vinculo.fase(),
                cliente(vinculo.cliente()),
                produto(vinculo.produto()),
                garantia(vinculo.garantia()),
                respostas(vinculo.respostasFormulario()));
    }

    private static ClienteFormularioDossieProduto cliente(DossieProdutoVinculoClienteDto cliente) {
        return cliente != null
                ? new ClienteFormularioDossieProduto(
                        cliente.cpf(), cliente.cnpj(), cliente.tipoVinculo())
                : null;
    }

    private static ProdutoFormularioDossieProduto produto(DossieProdutoVinculoProdutoDto produto) {
        return produto != null
                ? new ProdutoFormularioDossieProduto(
                        produto.codigoOperacao(), produto.codigoModalidade())
                : null;
    }

    private static GarantiaFormularioDossieProduto garantia(
            DossieProdutoVinculoGarantiaDto garantia
    ) {
        if (garantia == null) {
            return null;
        }
        return new GarantiaFormularioDossieProduto(
                garantia.codigoBacen(),
                garantia.produtoOperacao(),
                garantia.produtoModalidade(),
                avalistas(garantia.clientesAvalistas()));
    }

    private static List<ClienteAvalistaFormularioDossieProduto> avalistas(
            List<DossieProdutoClienteAvalistaDto> avalistas
    ) {
        if (avalistas == null) {
            return null;
        }
        List<ClienteAvalistaFormularioDossieProduto> resultado =
                new ArrayList<>(avalistas.size());
        for (DossieProdutoClienteAvalistaDto avalista : avalistas) {
            resultado.add(avalista != null
                    ? new ClienteAvalistaFormularioDossieProduto(
                            avalista.cpf(), avalista.cnpj())
                    : null);
        }
        return resultado;
    }

    private static List<RespostaFormularioDossieProduto> respostas(
            List<DossieProdutoRespostaFormularioDto> respostas
    ) {
        if (respostas == null) {
            return null;
        }
        List<RespostaFormularioDossieProduto> resultado = new ArrayList<>(respostas.size());
        for (DossieProdutoRespostaFormularioDto resposta : respostas) {
            resultado.add(resposta != null
                    ? new RespostaFormularioDossieProduto(
                            resposta.campoFormulario(),
                            resposta.resposta(),
                            resposta.opcoesSelecionadas(),
                            resposta.excluir())
                    : null);
        }
        return resultado;
    }

    private static List<ErroMensagemDto> mensagensErro(List<String> mensagens) {
        if (mensagens == null) {
            return null;
        }
        List<ErroMensagemDto> resultado = new ArrayList<>(mensagens.size());
        for (String mensagem : mensagens) {
            resultado.add(mensagem != null ? new ErroMensagemDto(mensagem) : null);
        }
        return resultado;
    }
}

package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ConsultaDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ConsultaDossieProdutoRestMapper {

    private ConsultaDossieProdutoRestMapper() {
    }

    static ConsultaDossieProdutoResponse paraResposta(DossieProdutoConsultado dossie) {
        if (dossie == null) {
            return null;
        }
        return new ConsultaDossieProdutoResponse(
                dossie.id(),
                dossie.chaveCorrelacaoCanal(),
                dossie.instanciaJbpm(),
                dossie.numeroNegocio(),
                dossie.canalCriacao(),
                dossie.unidadeCriacao(),
                dossie.dataCriacao(),
                mapear(dossie.clientes(), ConsultaDossieProdutoRestMapper::cliente),
                processo(dossie.processo()),
                fase(dossie.faseAtual()),
                situacao(dossie.situacaoAtual()),
                mapear(dossie.unidadesTratamento(), Function.identity()),
                mapear(
                        dossie.produtosContratados(),
                        ConsultaDossieProdutoRestMapper::produtoContratado
                )
        );
    }

    static Throwable paraExcecaoRest(FalhaConsultaDossieProduto falha) {
        int status = falha.status() != null ? falha.status() : 500;
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagens(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno()
        );

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(status, erro);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(status, erro);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro);
        };
    }

    private static ConsultaDossieProdutoResponse.Cliente cliente(
            DossieProdutoConsultado.Cliente origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDossieProdutoResponse.Cliente(
                origem.cpf(),
                origem.cnpj(),
                origem.nome(),
                origem.razaoSocial(),
                origem.tipoVinculo(),
                origem.identificadorNegocialVinculo(),
                origem.principal()
        );
    }

    private static ConsultaDossieProdutoResponse.Processo processo(
            DossieProdutoConsultado.Processo origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDossieProdutoResponse.Processo(
                origem.id(),
                origem.nome(),
                origem.identificadorNegocial(),
                origem.macroprocesso(),
                origem.data(),
                origem.tratamentoSeletivo(),
                origem.complementacaoSeletiva()
        );
    }

    private static ConsultaDossieProdutoResponse.Fase fase(
            DossieProdutoConsultado.Fase origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDossieProdutoResponse.Fase(
                origem.id(), origem.nome(), origem.identificadorNegocial(), origem.data()
        );
    }

    private static ConsultaDossieProdutoResponse.Situacao situacao(
            DossieProdutoConsultado.Situacao origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDossieProdutoResponse.Situacao(
                origem.id(), origem.nome(), origem.data(), origem.matricula()
        );
    }

    private static ConsultaDossieProdutoResponse.ProdutoContratado produtoContratado(
            DossieProdutoConsultado.ProdutoContratado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDossieProdutoResponse.ProdutoContratado(
                origem.id(), origem.codigoOperacao(), origem.codigoModalidade(), origem.nome()
        );
    }

    @SuppressWarnings("java:S1168") // Null preserva a ausência da lista recebida da dependência.
    private static List<ErroMensagemDto> mensagens(List<String> mensagens) {
        if (mensagens == null) {
            return null;
        }
        List<ErroMensagemDto> resultado = new ArrayList<>(mensagens.size());
        for (String mensagem : mensagens) {
            resultado.add(mensagem != null ? new ErroMensagemDto(mensagem) : null);
        }
        return resultado;
    }

    @SuppressWarnings("java:S1168") // Null distingue lista ausente de lista vazia no contrato.
    private static <O, D> List<D> mapear(List<O> origens, Function<O, D> conversor) {
        if (origens == null) {
            return null;
        }
        List<D> destinos = new ArrayList<>(origens.size());
        for (O origem : origens) {
            destinos.add(conversor.apply(origem));
        }
        return destinos;
    }
}

package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.consulta.ConsultaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ConsultaDossieProdutoMtrMapper {

    public DossieProdutoConsultado paraDominio(ConsultaDossieProdutoMtrResponse resposta) {
        if (resposta == null) {
            return null;
        }
        return new DossieProdutoConsultado(
                resposta.id(),
                resposta.chaveCorrelacaoCanal(),
                resposta.instanciaJbpm(),
                resposta.numeroNegocio(),
                resposta.canalCriacao(),
                resposta.unidadeCriacao(),
                resposta.dataCriacao(),
                mapear(resposta.clientes(), ConsultaDossieProdutoMtrMapper::cliente),
                processo(resposta.processo()),
                fase(resposta.faseAtual()),
                situacao(resposta.situacaoAtual()),
                mapear(resposta.unidadesTratamento(), Function.identity()),
                mapear(resposta.produtosContratados(),
                        ConsultaDossieProdutoMtrMapper::produtoContratado)
        );
    }

    private static DossieProdutoConsultado.Cliente cliente(
            ConsultaDossieProdutoMtrResponse.Cliente origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DossieProdutoConsultado.Cliente(
                origem.cpf(),
                origem.cnpj(),
                origem.nome(),
                origem.razaoSocial(),
                origem.tipoVinculo(),
                origem.identificadorNegocialVinculo(),
                origem.principal()
        );
    }

    private static DossieProdutoConsultado.Processo processo(
            ConsultaDossieProdutoMtrResponse.Processo origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DossieProdutoConsultado.Processo(
                origem.id(),
                origem.nome(),
                origem.identificadorNegocial(),
                origem.macroprocesso(),
                origem.data(),
                origem.tratamentoSeletivo(),
                origem.complementacaoSeletiva()
        );
    }

    private static DossieProdutoConsultado.Fase fase(
            ConsultaDossieProdutoMtrResponse.Fase origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DossieProdutoConsultado.Fase(
                origem.id(),
                origem.nome(),
                origem.identificadorNegocial(),
                origem.data()
        );
    }

    private static DossieProdutoConsultado.Situacao situacao(
            ConsultaDossieProdutoMtrResponse.Situacao origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DossieProdutoConsultado.Situacao(
                origem.id(), origem.nome(), origem.data(), origem.matricula()
        );
    }

    private static DossieProdutoConsultado.ProdutoContratado produtoContratado(
            ConsultaDossieProdutoMtrResponse.ProdutoContratado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DossieProdutoConsultado.ProdutoContratado(
                origem.id(), origem.codigoOperacao(), origem.codigoModalidade(), origem.nome()
        );
    }

    @SuppressWarnings("java:S1168") // Null distingue lista ausente de lista vazia no contrato MTR.
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

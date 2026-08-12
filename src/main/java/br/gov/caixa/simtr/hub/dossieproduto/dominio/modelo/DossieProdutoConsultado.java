package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record DossieProdutoConsultado(
        Long id,
        Long chaveCorrelacaoCanal,
        Long instanciaJbpm,
        Long numeroNegocio,
        String canalCriacao,
        Integer unidadeCriacao,
        String dataCriacao,
        List<Cliente> clientes,
        Processo processo,
        Fase faseAtual,
        Situacao situacaoAtual,
        List<Integer> unidadesTratamento,
        List<ProdutoContratado> produtosContratados
) {

    public record Cliente(
            String cpf,
            String cnpj,
            String nome,
            String razaoSocial,
            String tipoVinculo,
            Long identificadorNegocialVinculo,
            Boolean principal
    ) {
    }

    public record Processo(
            Integer id,
            String nome,
            Long identificadorNegocial,
            String macroprocesso,
            String data,
            Boolean tratamentoSeletivo,
            Boolean complementacaoSeletiva
    ) {
    }

    public record Fase(
            Integer id,
            String nome,
            Long identificadorNegocial,
            String data
    ) {
    }

    public record Situacao(
            Integer id,
            String nome,
            String data,
            String matricula
    ) {
    }

    public record ProdutoContratado(
            Integer id,
            Integer codigoOperacao,
            Integer codigoModalidade,
            String nome
    ) {
    }
}

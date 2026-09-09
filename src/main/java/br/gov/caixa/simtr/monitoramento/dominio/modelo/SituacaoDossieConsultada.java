package br.gov.caixa.simtr.monitoramento.dominio.modelo;

/**
 * Situacao original traduzida da consulta publica do Hub, sem seus dados de transporte ou cliente.
 *
 * @param id identificador original da situacao, inclusive nulo; nao e codigo funcional do monitoramento
 * @param nome texto original nao vazio; nenhuma normalizacao ou classificacao e realizada
 */
public record SituacaoDossieConsultada(Integer id, String nome) {

    public SituacaoDossieConsultada {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da situacao do dossie obrigatorio.");
        }
    }
}

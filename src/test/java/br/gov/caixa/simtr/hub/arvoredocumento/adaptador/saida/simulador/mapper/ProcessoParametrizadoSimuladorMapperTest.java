package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.dto.ProcessoParametrizadoSimuladorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProcessoParametrizadoSimuladorMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void leContratoProprioEMapeiaArvoreCompletaComNulosEChecklistsHeterogeneos()
            throws Exception {
        String json = """
                {
                  "identificador_negocial": 100,
                  "nome": "Processo simulado",
                  "ativo": true,
                  "ultima_alteracao": null,
                  "indicador_produto_obrigatorio": false,
                  "macroprocesso": {
                    "identificador_negocial": 200,
                    "nome": "Credito",
                    "ativo": null,
                    "ultima_alteracao": "2026-07-14"
                  },
                  "relacionamentos": [null, {
                    "identificador_negocial": 300,
                    "nome": "Proponente",
                    "tipo_pessoa": "F",
                    "principal": true,
                    "obrigatorio": false,
                    "relacionado": false,
                    "sequencia": false,
                    "campos_formulario": [{
                      "identificador_negocial": 400,
                      "label": "Autoriza?",
                      "obrigatorio": true,
                      "ativo": true,
                      "exibicao_condicional": null,
                      "tamanho_apresentacao": 6,
                      "ordem_apresentacao": 1,
                      "tipo": "RADIO",
                      "mascara": null,
                      "placeholder": null,
                      "tamanho_minimo": null,
                      "tamanho_maximo": null,
                      "orientacao_preenchimento": null,
                      "bloquear_edicao": false,
                      "opcoes_disponiveis": [null, {
                        "valor_opcao": "S",
                        "descricao_opcao": "Sim",
                        "ativo": true
                      }]
                    }],
                    "documentos": []
                  }],
                  "produtos": [{
                    "codigo_operacao": 500,
                    "codigo_modalidade": 501,
                    "nome": "Capital de giro",
                    "campos_formulario": null,
                    "documentos": [],
                    "garantias": [{
                      "codigo_bacen": 600,
                      "nome_garantia": "Aval",
                      "fidejussoria": true,
                      "campos_formulario": [],
                      "documentos": null,
                      "checklist": []
                    }],
                    "checklist": [{
                      "identificador_checklist": "700",
                      "versao_checklist": "2"
                    }]
                  }],
                  "fases": [{
                    "identificador_negocial": 800,
                    "nome": "Formalizacao",
                    "ativo": true,
                    "ultima_alteracao": null,
                    "ordem": 1,
                    "orientacao_usuario": null,
                    "produtos": [],
                    "garantias": [],
                    "campos_formulario": [],
                    "documentos": [],
                    "checklist": [{
                      "identificador_checklist": 801,
                      "versao_checklist": 3
                    }]
                  }],
                  "documentos": [{
                    "funcao_documental": null,
                    "tipo_documento": {
                      "codigo_tipologia": "TIP-01",
                      "nome": "Contrato",
                      "permite_reuso": true,
                      "permite_multiplo": false,
                      "ativo": true,
                      "checklist": {
                        "identificador_checklist": 900,
                        "versao_checklist": 4
                      }
                    },
                    "obrigatorio": false
                  }],
                  "checklist": [{
                    "identificador_checklist": 1000,
                    "versao_checklist": 5
                  }]
                }
                """;

        var resposta = OBJECT_MAPPER.readValue(
                json,
                ProcessoParametrizadoSimuladorResponse.class
        );
        var processo = new ProcessoParametrizadoSimuladorMapper().paraDominio(resposta);

        assertEquals(100L, processo.identificadorNegocial());
        assertEquals("Processo simulado", processo.nome());
        assertNull(processo.ultimaAlteracao());
        assertEquals(200L, processo.macroprocesso().identificadorNegocial());
        assertNull(processo.macroprocesso().ativo());
        assertNull(processo.relacionamentos().getFirst());
        assertEquals("F", processo.relacionamentos().get(1).tipoPessoa());
        assertNull(processo.relacionamentos().get(1).camposFormulario().getFirst()
                .opcoesDisponiveis().getFirst());
        assertEquals("S", processo.relacionamentos().get(1).camposFormulario().getFirst()
                .opcoesDisponiveis().get(1).valorOpcao());
        assertNull(processo.produtos().getFirst().camposFormulario());
        assertNull(processo.produtos().getFirst().garantias().getFirst().documentos());
        assertNull(processo.produtos().getFirst().garantias().getFirst().checklist());
        assertEquals(700L, processo.produtos().getFirst().checklist().identificadorChecklist());
        assertEquals(801L, processo.fases().getFirst().checklist().getFirst()
                .identificadorChecklist());
        assertEquals("TIP-01", processo.documentos().getFirst().tipoDocumento()
                .codigoTipologia());
        assertEquals(900L, processo.documentos().getFirst().tipoDocumento().checklist()
                .identificadorChecklist());
        assertEquals(1000L, processo.checklist().identificadorChecklist());
        assertDoesNotThrow(() -> processo.relacionamentos().add(null));
        assertTrue(processo.relacionamentos().getLast() == null);
    }
}

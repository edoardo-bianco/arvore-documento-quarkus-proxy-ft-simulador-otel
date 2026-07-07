# mock 1000009990-consulta-processo-parametrizacao-v2-identificador-negocial

## enpoint
GET {{microservice-BaseUrl-api-manager_usar}}/parametrizacao/v2/patriarca/processo/identificador-negocial/1000009990


## dados do mock corpo do retorno json

{
    "identificador_negocial": 1000009990,
    "nome": "Utilização FGTS - 1º Uso",
    "ativo": true,
    "indicador_produto_obrigatorio": false,
    "macroprocesso": {
        "identificador_negocial": 20210706,
        "nome": "HABITAÇÃO"
    },
    "relacionamentos": [
        {
            "identificador_negocial": 1000009995,
            "nome": "Mutuário",
            "tipo_pessoa": "F",
            "principal": true,
            "obrigatorio": true,
            "relacionado": false,
            "sequencia": false,
            "campos_formulario": [],
            "documentos": [
                {
                    "funcao_documental": {
                        "nome": "COMPROVANTE DE ENDEREÇO - 1º USO FGTS",
                        "tipos_documento": [
                            {
                                "codigo_tipologia": "0001000100019001",
                                "nome": "Conta de Água",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            },
                            {
                                "codigo_tipologia": "0001000100019003",
                                "nome": "Conta de Energia Elétrica",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            }
                        ],
                        "checklist": {
                            "identificador_checklist": 1000010029,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0007000200020149",
                        "nome": "Recibo de Entrega do Imposto de Renda",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000011319,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0001000100030024",
                        "nome": "Declaração de Imposto de Renda PF",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000010007,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0003000500050011",
                        "nome": "Certidao de Matricula do Imovel",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000010018,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0001000100020016",
                        "nome": "Certidao Casamento",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000012583,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "funcao_documental": {
                        "nome": "DOCUMENTO DE IDENTIFICAÇÃO 1º USO FGTS",
                        "tipos_documento": [
                            {
                                "codigo_tipologia": "0001000100020005",
                                "nome": "RG - Documento de Identidade",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            },
                            {
                                "codigo_tipologia": "0001000100020007",
                                "nome": "CNH - Carteira Nacional de Habilitação",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            },
                            {
                                "codigo_tipologia": "0007000200050036",
                                "nome": " Damp - Fgts MO29300 ",
                                "permite_reuso": false,
                                "ativo": true,
                                "permite_multiplo": false
                            }
                        ],
                        "checklist": {
                            "identificador_checklist": 1000011315,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                }
            ]
        },
        {
            "identificador_negocial": 42005203,
            "nome": "Coobrigado/ Cônjuge do Proponente",
            "tipo_pessoa": "F",
            "principal": false,
            "obrigatorio": false,
            "relacionado": true,
            "sequencia": false,
            "campos_formulario": [],
            "documentos": [
                {
                    "funcao_documental": {
                        "nome": "COMPROVANTE DE ENDEREÇO - 1º USO FGTS",
                        "tipos_documento": [
                            {
                                "codigo_tipologia": "0001000100019001",
                                "nome": "Conta de Água",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            },
                            {
                                "codigo_tipologia": "0001000100019003",
                                "nome": "Conta de Energia Elétrica",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            }
                        ],
                        "checklist": {
                            "identificador_checklist": 1000010029,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0007000200020149",
                        "nome": "Recibo de Entrega do Imposto de Renda",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000011319,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0001000100030024",
                        "nome": "Declaração de Imposto de Renda PF",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000010007,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0003000500050011",
                        "nome": "Certidao de Matricula do Imovel",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000010018,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0001000100020016",
                        "nome": "Certidao Casamento",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": {
                            "identificador_checklist": 1000012583,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                },
                {
                    "funcao_documental": {
                        "nome": "DOCUMENTO DE IDENTIFICAÇÃO 1º USO FGTS",
                        "tipos_documento": [
                            {
                                "codigo_tipologia": "0001000100020005",
                                "nome": "RG - Documento de Identidade",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            },
                            {
                                "codigo_tipologia": "0001000100020007",
                                "nome": "CNH - Carteira Nacional de Habilitação",
                                "permite_reuso": true,
                                "ativo": true,
                                "permite_multiplo": false
                            },
                            {
                                "codigo_tipologia": "0007000200050036",
                                "nome": " Damp - Fgts MO29300 ",
                                "permite_reuso": false,
                                "ativo": true,
                                "permite_multiplo": false
                            }
                        ],
                        "checklist": {
                            "identificador_checklist": 1000011315,
                            "versao_checklist": 1
                        }
                    },
                    "obrigatorio": false
                }
            ]
        }
    ],
    "produtos": [],
    "fases": [
        {
            "identificador_negocial": 1000009991,
            "nome": "Recepção dos dados e documentos ",
            "ativo": true,
            "ultima_alteracao": "13/05/2026 09:16:18",
            "ordem": 1,
            "orientacao_usuario": null,
            "produtos": [],
            "garantias": [],
            "campos_formulario": [
                {
                    "identificador_negocial": 1000009996,
                    "label": "DADOS SIBKO - Nº Protocolo",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 12,
                    "ordem_apresentacao": 1,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009997,
                    "label": "DADOS DO MUTUÁRIO - Nome",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 2,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009998,
                    "label": "DADOS DO MUTUÁRIO - CPF",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 3,
                    "tipo": "CPF",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009999,
                    "label": "DADOS DO COOBRIGADO/CÔNJUGE - Nome",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 4,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000010000,
                    "label": "DADOS DO COOBRIGADO/CÔNJUGE - CPF",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 5,
                    "tipo": "CPF",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000010001,
                    "label": "DADOS DO IMÓVEL FINANCIADO - Endereço",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 6,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000010002,
                    "label": "DECLARAÇÕES DO CLIENTE - Mutuário reside em qual cidade e estado?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 7,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009983,
                    "label": "DECLARAÇÕES DO CLIENTE - Mutuário exerce ocupação laboral principal em qual cidade e estado?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 8,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012859,
                    "label": "DECLARAÇÕES DO CLIENTE - Qual a situação profissional atual?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 9,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009984,
                    "label": "DECLARAÇÕES DO CLIENTE - Qual o nome + CNPJ da empresa onde trabalha??",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 10,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009985,
                    "label": "DECLARAÇÕES DO CLIENTE - Mutuário é isento do Imposto de Renda? ",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 4,
                    "ordem_apresentacao": 11,
                    "tipo": "RADIO",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": [
                        {
                            "valor_opcao": "s",
                            "descricao_opcao": "Sim",
                            "ativo": true
                        },
                        {
                            "valor_opcao": "n",
                            "descricao_opcao": "Não",
                            "ativo": true
                        }
                    ]
                },
                {
                    "identificador_negocial": 1000009986,
                    "label": "DECLARAÇÕES DO CLIENTE - Estado civil",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 12,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009987,
                    "label": "DECLARAÇÕES DO CLIENTE - Regime de casamento",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 13,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": null,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009988,
                    "label": "DECLARAÇÕES DO CLIENTE - Data da união estável/casamento",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 14,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000009989,
                    "label": "DECLARAÇÕES DO CLIENTE - Cônjuge é isento do Imposto de Renda?  ",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 4,
                    "ordem_apresentacao": 15,
                    "tipo": "RADIO",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": [
                        {
                            "valor_opcao": "n",
                            "descricao_opcao": "Não",
                            "ativo": true
                        },
                        {
                            "valor_opcao": "s",
                            "descricao_opcao": "Sim",
                            "ativo": true
                        }
                    ]
                },
                {
                    "identificador_negocial": 1000010003,
                    "label": "DECLARAÇÕES DO CLIENTE - Houve alteração na matrícula de imóvel impeditivo? ",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 4,
                    "ordem_apresentacao": 16,
                    "tipo": "RADIO",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": [
                        {
                            "valor_opcao": "n",
                            "descricao_opcao": "Não",
                            "ativo": true
                        },
                        {
                            "valor_opcao": "s",
                            "descricao_opcao": "Sim",
                            "ativo": true
                        }
                    ]
                },
                {
                    "identificador_negocial": 1000012868,
                    "label": "USO DO FGTS - Modalidade de Uso",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 17,
                    "tipo": "TEXTAREA",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012867,
                    "label": "DADOS DO IMÓVEL FINANCIADO - Nº do Contrato",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 18,
                    "tipo": "TEXTAREA",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012866,
                    "label": "DADOS DO IMÓVEL FINANCIADO - Saldo Devedor",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 19,
                    "tipo": "TEXTAREA",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012865,
                    "label": "DECLARAÇÕES DO CLIENTE - Proprietário de outro imóvel ou financiamento?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 20,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012864,
                    "label": "DECLARAÇÕES DO CLIENTE - Proprietário de outro imóvel no município onde reside? ",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 21,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012863,
                    "label": "DECLARAÇÕES DO CLIENTE - Proprietário de outro imóvel no município onde trabalha? ",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 22,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012862,
                    "label": "DECLARAÇÕES DO CLIENTE - Possui imóvel em município limítrofe de onde trabalha/reside? ",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 10,
                    "ordem_apresentacao": 23,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012861,
                    "label": "DECLARAÇÕES DO CLIENTE -  Possui imóvel na mesma região metropolitana de onde trabalha/reside?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 24,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012860,
                    "label": "DECLARAÇÕES DO CLIENTE -  Possui holding patrimonial/familiar?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 25,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012858,
                    "label": "DECLARAÇÕES DO CLIENTE - União Estável",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 6,
                    "ordem_apresentacao": 26,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                },
                {
                    "identificador_negocial": 1000012857,
                    "label": "DECLARAÇÕES DO CLIENTE - Vendeu algum imóvel que ainda consta no IRPF?  ",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 4,
                    "ordem_apresentacao": 27,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": true,
                    "opcoes_disponiveis": []
                }
            ],
            "documentos": [],
            "checklist": []
        },
        {
            "identificador_negocial": 1000009992,
            "nome": "Validação Documental - IA",
            "ativo": true,
            "ultima_alteracao": "13/05/2026 09:16:19",
            "ordem": 2,
            "orientacao_usuario": null,
            "produtos": [],
            "garantias": [],
            "campos_formulario": [
                {
                    "identificador_negocial": 1000011176,
                    "label": "Comando de Finalização Automático?",
                    "obrigatorio": true,
                    "ativo": true,
                    "exibicao_condicional": null,
                    "tamanho_apresentacao": 4,
                    "ordem_apresentacao": 1,
                    "tipo": "RADIO",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": false,
                    "opcoes_disponiveis": [
                        {
                            "valor_opcao": "N",
                            "descricao_opcao": "Não",
                            "ativo": true
                        },
                        {
                            "valor_opcao": "S",
                            "descricao_opcao": "Sim",
                            "ativo": true
                        }
                    ]
                },
                {
                    "identificador_negocial": 1000011177,
                    "label": "Número do DAMP",
                    "obrigatorio": false,
                    "ativo": true,
                    "exibicao_condicional": "[[{\"tipo_regra\":\"FF\",\"campo_resposta\":\"8087\",\"valores\":[\"S\"],\"tipo_expressao\":\"3949\"}]]",
                    "tamanho_apresentacao": 12,
                    "ordem_apresentacao": 2,
                    "tipo": "TEXT",
                    "mascara": null,
                    "placeholder": null,
                    "tamanho_minimo": null,
                    "tamanho_maximo": null,
                    "orientacao_preenchimento": null,
                    "bloquear_edicao": false,
                    "opcoes_disponiveis": []
                }
            ],
            "documentos": [],
            "checklist": [
                {
                    "identificador_checklist": 5000005493,
                    "versao_checklist": 1
                }
            ]
        },
        {
            "identificador_negocial": 1000015907,
            "nome": "Validação Documental - Centralizadora ",
            "ativo": true,
            "ultima_alteracao": "13/05/2026 09:16:19",
            "ordem": 3,
            "orientacao_usuario": null,
            "produtos": [],
            "garantias": [],
            "campos_formulario": [],
            "documentos": [],
            "checklist": [
                {
                    "identificador_checklist": 1000010007,
                    "versao_checklist": 1
                }
            ]
        },
        {
            "identificador_negocial": 1000009993,
            "nome": "Conjunto Probatório FGTS",
            "ativo": true,
            "ultima_alteracao": "13/05/2026 09:16:20",
            "ordem": 4,
            "orientacao_usuario": null,
            "produtos": [],
            "garantias": [],
            "campos_formulario": [],
            "documentos": [
                {
                    "tipo_documento": {
                        "codigo_tipologia": "0007000200020176",
                        "nome": "Serviço de 1 Uso do Fgts",
                        "permite_reuso": true,
                        "permite_multiplo": false,
                        "ativo": true,
                        "checklist": null
                    },
                    "obrigatorio": false
                }
            ],
            "checklist": []
        },
        {
            "identificador_negocial": 1000009994,
            "nome": "Finalização",
            "ativo": true,
            "ultima_alteracao": "13/05/2026 09:16:22",
            "ordem": 5,
            "orientacao_usuario": null,
            "produtos": [],
            "garantias": [],
            "campos_formulario": [],
            "documentos": [],
            "checklist": []
        }
    ],
    "documentos": [],
    "checklist": null
}


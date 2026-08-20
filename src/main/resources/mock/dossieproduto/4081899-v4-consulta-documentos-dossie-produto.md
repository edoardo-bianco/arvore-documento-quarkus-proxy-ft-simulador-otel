# 4081899

## endpoint

```http
GET /simtr-dossie-produto/v4/dossie-produto/4081899/documentos?inclui-armazenamento=true&inclui-assinaturas=true&inclui-atributos=true&inclui-conformidade=true&inclui-outsourcing=true&inclui-propriedades=true
```

## dados do mock corpo do retorno json

```json
{
  "documentos": [
    {
      "id_instancia_documento": 1132220,
      "id_documento": 1924727,
      "codigo_ged": "GED-SIMULADO-0001",
      "data_hora_captura": "08/07/2026 11:13:18",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 3315,
        "nome": "Pesquisa Cadastral - Sipes",
        "codigo_tipologia": "0001000100069003",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "00000000000",
          "nome": "CLIENTE SIMULADO 01",
          "tipo_vinculo": "Vendedor PF",
          "identificador_negocial_vinculo": 90000001,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098870,
          "data_hora_armazenamento": "08/07/2026 11:13:19",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0001",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132221,
      "id_documento": 1924728,
      "codigo_ged": "GED-SIMULADO-0002",
      "data_hora_captura": "08/07/2026 11:13:19",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 1135,
        "nome": "Certidão de Nascimento",
        "codigo_tipologia": "0001000100020008",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "00000000000",
          "nome": "CLIENTE SIMULADO 01",
          "tipo_vinculo": "Vendedor PF",
          "identificador_negocial_vinculo": 90000001,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098871,
          "data_hora_armazenamento": "08/07/2026 11:13:19",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0002",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132178,
      "id_documento": 184067,
      "codigo_ged": null,
      "data_hora_captura": "04/06/2024 13:48:42",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 2,
        "nome": "CNH - Carteira Nacional de Habilitação",
        "codigo_tipologia": "0001000100020007",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "00000000000",
          "nome": "CLIENTE SIMULADO 01",
          "tipo_vinculo": "Vendedor PF",
          "identificador_negocial_vinculo": 90000001,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 151906,
          "data_hora_armazenamento": "04/06/2024 13:48:42",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0003.pdf",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132224,
      "id_documento": 1924731,
      "codigo_ged": "GED-SIMULADO-0004",
      "data_hora_captura": "08/07/2026 11:13:20",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 672,
        "nome": "Ficha Cadastro Pessoa Física",
        "codigo_tipologia": "0001000100060002",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "11111111111",
          "nome": "CLIENTE SIMULADO 02",
          "tipo_vinculo": "Coobrigado",
          "identificador_negocial_vinculo": 90000002,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098874,
          "data_hora_armazenamento": "08/07/2026 11:13:20",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0004",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132225,
      "id_documento": 1924732,
      "codigo_ged": "GED-SIMULADO-0005",
      "data_hora_captura": "08/07/2026 11:13:20",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 4663,
        "nome": "MO30844 - Formulário Cliente Habitação",
        "codigo_tipologia": "0007000200020134",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "11111111111",
          "nome": "CLIENTE SIMULADO 02",
          "tipo_vinculo": "Coobrigado",
          "identificador_negocial_vinculo": 90000002,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098875,
          "data_hora_armazenamento": "08/07/2026 11:13:21",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0005",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132226,
      "id_documento": 1826851,
      "codigo_ged": null,
      "data_hora_captura": "06/11/2024 17:20:47",
      "data_hora_validade": "23/03/2052 17:20:47",
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 1,
        "nome": "RG - Documento de Identidade",
        "codigo_tipologia": "0001000100020005",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "11111111111",
          "nome": "CLIENTE SIMULADO 02",
          "tipo_vinculo": "Coobrigado",
          "identificador_negocial_vinculo": 90000002,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2004918,
          "data_hora_armazenamento": "06/11/2024 17:20:47",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0006.pdf",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132227,
      "id_documento": 173088,
      "codigo_ged": null,
      "data_hora_captura": "04/06/2024 13:32:18",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 2,
        "nome": "CNH - Carteira Nacional de Habilitação",
        "codigo_tipologia": "0001000100020007",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "11111111111",
          "nome": "CLIENTE SIMULADO 02",
          "tipo_vinculo": "Coobrigado",
          "identificador_negocial_vinculo": 90000002,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 140927,
          "data_hora_armazenamento": "04/06/2024 13:32:18",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0007.pdf",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132222,
      "id_documento": 1924729,
      "codigo_ged": "GED-SIMULADO-0008",
      "data_hora_captura": "08/07/2026 11:13:19",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 4031,
        "nome": "Crf - Certificado de Regularidade do Fgts",
        "codigo_tipologia": "0001000200030043",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cnpj": "00000000000000",
          "razao_social": "EMPRESA SIMULADA",
          "tipo_vinculo": "Vendedor PJ",
          "identificador_negocial_vinculo": 90000003,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098872,
          "data_hora_armazenamento": "08/07/2026 11:13:19",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0008",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132223,
      "id_documento": 1924730,
      "codigo_ged": "GED-SIMULADO-0009",
      "data_hora_captura": "08/07/2026 11:13:19",
      "data_hora_validade": "06/08/2026 16:41:17",
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 5375,
        "nome": "Certidão Simplificada da Junta Comercial",
        "codigo_tipologia": "0001000200010042",
        "ativo": false
      },
      "situacao_documento": "Vencido",
      "vinculo_dossie": {
        "cliente": {
          "cnpj": "00000000000000",
          "razao_social": "EMPRESA SIMULADA",
          "tipo_vinculo": "Vendedor PJ",
          "identificador_negocial_vinculo": 90000003,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098873,
          "data_hora_armazenamento": "08/07/2026 11:13:20",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0009",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132179,
      "id_documento": 1852358,
      "codigo_ged": null,
      "data_hora_captura": "28/03/2025 10:13:56",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 1483,
        "nome": "Documento Constitutivo",
        "codigo_tipologia": "0001000200010031",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cnpj": "00000000000000",
          "razao_social": "EMPRESA SIMULADA",
          "tipo_vinculo": "Vendedor PJ",
          "identificador_negocial_vinculo": 90000003,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2023875,
          "data_hora_armazenamento": "28/03/2025 10:13:56",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0010.jpg",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132180,
      "id_documento": 1826869,
      "codigo_ged": null,
      "data_hora_captura": "06/11/2024 17:20:47",
      "data_hora_validade": "23/03/2052 17:20:47",
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 1,
        "nome": "RG - Documento de Identidade",
        "codigo_tipologia": "0001000100020005",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "22222222222",
          "nome": "CLIENTE SIMULADO 03",
          "tipo_vinculo": "Responsável Técnico da obra",
          "identificador_negocial_vinculo": 90000004,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2004934,
          "data_hora_armazenamento": "06/11/2024 17:20:47",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0011.pdf",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132175,
      "id_documento": 1826910,
      "codigo_ged": null,
      "data_hora_captura": "06/11/2024 17:20:47",
      "data_hora_validade": "23/03/2052 17:20:47",
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 1,
        "nome": "RG - Documento de Identidade",
        "codigo_tipologia": "0001000100020005",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "33333333333",
          "nome": "CLIENTE SIMULADO 04",
          "tipo_vinculo": "Proponente",
          "identificador_negocial_vinculo": 90000005,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2004977,
          "data_hora_armazenamento": "06/11/2024 17:20:47",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0012.pdf",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132176,
      "id_documento": 173091,
      "codigo_ged": null,
      "data_hora_captura": "04/06/2024 13:32:19",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 2,
        "nome": "CNH - Carteira Nacional de Habilitação",
        "codigo_tipologia": "0001000100020007",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "33333333333",
          "nome": "CLIENTE SIMULADO 04",
          "tipo_vinculo": "Proponente",
          "identificador_negocial_vinculo": 90000005,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 140930,
          "data_hora_armazenamento": "04/06/2024 13:32:19",
          "tipo_armazenamento": "STORAGE_RECEBIDO",
          "path_storage": "simulador/documentos/documento-0013.pdf",
          "object_store_ged": null,
          "codigo_ged": null,
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    },
    {
      "id_instancia_documento": 1132177,
      "id_documento": 1924682,
      "codigo_ged": "GED-SIMULADO-0014",
      "data_hora_captura": "07/07/2026 19:34:08",
      "data_hora_validade": null,
      "matricula_captura": "SIM0001",
      "tipo_documento": {
        "id": 4663,
        "nome": "MO30844 - Formulário Cliente Habitação",
        "codigo_tipologia": "0007000200020134",
        "ativo": false
      },
      "situacao_documento": "Criado",
      "vinculo_dossie": {
        "cliente": {
          "cpf": "33333333333",
          "nome": "CLIENTE SIMULADO 04",
          "tipo_vinculo": "Proponente",
          "identificador_negocial_vinculo": 90000005,
          "principal": false
        }
      },
      "atributos": [],
      "assinaturas_digitais": [],
      "conformidade": [],
      "propriedades": [],
      "outsourcing": [],
      "armazenamento": [
        {
          "id": 2098824,
          "data_hora_armazenamento": "07/07/2026 19:34:09",
          "tipo_armazenamento": "GED_RECEBIDO",
          "path_storage": null,
          "object_store_ged": "OS_SIMULADOR",
          "codigo_ged": "GED-SIMULADO-0014",
          "data_hora_previsao_exclusao": null,
          "data_hora_exclusao": null
        }
      ]
    }
  ]
}
```

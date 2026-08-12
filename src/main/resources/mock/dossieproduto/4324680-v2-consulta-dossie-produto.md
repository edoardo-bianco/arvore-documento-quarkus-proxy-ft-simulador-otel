# Mock - Consulta de Dossie Produto por Identificador

## endpoint

```http
GET /simtr-dossie-produto/v2/dossie-produto/{id}
```

## dados do mock corpo do retorno json

```json
{
  "id": 4324680,
  "chave_correlacao_canal": 1000012592,
  "instancia_jbpm": null,
  "numero_negocio": null,
  "canal_criacao": "SIMTRAPI",
  "unidade_criacao": 5402,
  "data_criacao": null,
  "clientes": [
    {
      "cpf": "00000000000",
      "cnpj": null,
      "nome": "CLIENTE SIMULADO",
      "razao_social": null,
      "tipo_vinculo": "Proponente",
      "identificador_negocial_vinculo": 40610702,
      "principal": true
    }
  ],
  "processo": {
    "id": 5032,
    "nome": "Concessão Habitacional",
    "identificador_negocial": 1000016487,
    "macroprocesso": "HABITAÇÃO",
    "tratamento_seletivo": true,
    "complementacao_seletiva": true
  },
  "fase_atual": {
    "id": 5033,
    "nome": "Recepção de dados e documentos",
    "identificador_negocial": 1000016488,
    "data": "23/07/2026 10:24:00"
  },
  "situacao_atual": {
    "id": 1,
    "nome": "Rascunho",
    "data": "23/07/2026 10:24:00",
    "matricula": "SIMTRAPI"
  },
  "unidades_tratamento": [],
  "produtos_contratados": [
    {
      "id": null,
      "codigo_operacao": null,
      "codigo_modalidade": null,
      "nome": null
    }
  ]
}
```

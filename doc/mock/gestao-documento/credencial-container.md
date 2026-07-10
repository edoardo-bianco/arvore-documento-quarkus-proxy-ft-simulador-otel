# Mock - Gestao de Documentos - Credencial de container

## objetivo

Copia documental do mock runtime para `POST /simtr-gestao-documento/v1/storage/container/credencial`.

## dados do mock corpo do retorno json

```json
{
  "sas": "sv=mock&ss=b&srt=o&sp=rw&se=2026-07-10T18:00:00Z&sig=mock",
  "validade": "10/07/2026 18:00:00",
  "url_storage": "https://dossiedigitaldes.blob.core.windows.net",
  "nome_container": "pre-validacao"
}
```

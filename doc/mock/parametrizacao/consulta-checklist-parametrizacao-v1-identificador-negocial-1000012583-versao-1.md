# consulta-checklist-parametrizacao-v1-identificador-negocial-1000012583-versao-1

## enpoint
GET {{parametrizacao-BaseUrl}}/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/1000012583/versao/1

## dados do mock corpo do retorno json


{
    "nome": "Utilização FGTS - Certidão de Casamento",
    "identificador_negocial": 1000012583,
    "versao": 1,
    "data_hora_criacao": "25/03/2026 15:36:04",
    "data_hora_ultima_alteracao": "13/05/2026 16:27:41",
    "verificacao_previa": false,
    "orientacao_operador": null,
    "apontamentos": [
        {
            "identificador_negocial": 1000012577,
            "nome": "Certidão está legível e sem rasuras? ",
            "descricao": "<p> Conferência no próprio documento</p>",
            "orientacao_operador": "Documento não está legível ou apresenta rasuras. Gentileza encaminhar um novo documento.",
            "indicador_reanalise": false,
            "sequencia_apresentacao": 1
        },
        {
            "identificador_negocial": 1000012579,
            "nome": "Certidão está em nome do solicitante/mutuário e do cônjuge/companheiro(a)? ",
            "descricao": "<p>Verificar se os nomes que constam na certidão são do solicitante/mutuário e do seu cônjuge/companheiro(a) da jornada. Tolerar erro de até 3 carácteres.</p>",
            "orientacao_operador": "Nomes que constam na declaração não pertencem ao solicitante/mutuário e do seu cônjuge/companheiro(a). Favor verificar e reeenviar documento correto.",
            "indicador_reanalise": false,
            "sequencia_apresentacao": 2
        },
        {
            "identificador_negocial": 1000012578,
            "nome": "Certidão obrigatória? ",
            "descricao": "<p>Verificar se o estado civil informado na jornada é casado. Verificar também na certidão se no campo de anotações (averbações) não consta a averbação de divórcio ou separação. </p><p>O estado civil mais recente na averbação deverá sobrepor o estado civil da certidão. Em geral consta expressões como separação (ou expressões equivalentes como: separação consensual, separação litigiosa, separação judicial), divórcio (ou expressão equivalente como: dissolvida a sociedade conjugal), viúvo (ou expressão equivalente como falecimento). </p><p>Caso o estado civil confirmado seja casado, considerar como ¿V¿ True. Em caso de situação diferente, retornar como ¿F¿ False.</p>",
            "orientacao_operador": "Verificar estado civil na certidão para envio do documento correto.",
            "indicador_reanalise": false,
            "sequencia_apresentacao": 3
        },
        {
            "identificador_negocial": 1000012580,
            "nome": "Estado civil conforme?",
            "descricao": "<p>Verificar se o estado civil informado na jornada é o mesmo da certidão de casamento. Na certidão deve ser analisada a anotação / averbação, e caso conste algum outro estado civil, deve ser considerado o registro mais recente (último registro) como válido.&nbsp;</p>",
            "orientacao_operador": "Estado civil informado na jornada não corresponde ao que consta na certidão enviada. Favor verificar.",
            "indicador_reanalise": false,
            "sequencia_apresentacao": 4
        },
        {
            "identificador_negocial": 1000012581,
            "nome": "Regime de bens conforme?",
            "descricao": "<p>Verificar se o regime de bens informado na jornada é o mesmo da certidão de casamento. Na certidão deve ser analisada a anotação / averbação da certidão, e e caso conste algum outro regime de bens, deve ser considerado o registro mais recente (último registro) como válido.</p>",
            "orientacao_operador": "O regime de bens informado na Jornada não corresponde ao registrado na certidão de casamento. Favor verificar e reenviar com as devidas correções.",
            "indicador_reanalise": false,
            "sequencia_apresentacao": 5
        },
        {
            "identificador_negocial": 1000012582,
            "nome": "Data do casamento conforme?",
            "descricao": "<p> Verificar se a data do casamento está igual a informada na jornada.&nbsp;</p>",
            "orientacao_operador": "A data de casamento diverge da informação cadastrada na Jornada. Favor verificar e reenviar o documento após as devidas correções.",
            "indicador_reanalise": false,
            "sequencia_apresentacao": 6
        }
    ]
}
package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.DossieProdutoResource;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoDocumentoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.InclusaoDocumentoDossieProdutoRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DossieProdutoValidacaoJakartaContractTest {

    @Test
    void aplicaCascataNoTipoDosElementosDasListasRest() throws NoSuchMethodException {
        AnnotatedType formulario = DossieProdutoResource.class
                .getDeclaredMethod("atualizarFormularioDossieProduto", Long.class, List.class)
                .getAnnotatedParameterTypes()[1];

        assertCascataNoElemento("formulario.requisicao", formulario);
        assertCascataNoElemento("formulario.respostasFormulario",
                tipoDoComponente(DossieProdutoVinculoDossieDto.class, "respostasFormulario"));
        assertCascataNoElemento("formulario.clientesAvalistas",
                tipoDoComponente(DossieProdutoVinculoGarantiaDto.class, "clientesAvalistas"));
        assertCascataNoElemento("documento.clienteAvalista",
                tipoDoComponente(DossieProdutoDocumentoGarantiaDto.class, "clienteAvalista"));
        assertCascataNoElemento("documento.atributos",
                tipoDoComponente(InclusaoDocumentoDossieProdutoRequest.class, "atributos"));
        assertCascataNoElemento("documento.propriedades",
                tipoDoComponente(InclusaoDocumentoDossieProdutoRequest.class, "propriedades"));
    }

    private static AnnotatedType tipoDoComponente(Class<?> tipoRecord, String nome) {
        return Arrays.stream(tipoRecord.getRecordComponents())
                .filter(componente -> componente.getName().equals(nome))
                .findFirst()
                .map(RecordComponent::getAnnotatedType)
                .orElseThrow(() -> new AssertionError(
                        "Componente %s nao encontrado em %s".formatted(nome, tipoRecord.getName())));
    }

    private static void assertCascataNoElemento(String contrato, AnnotatedType lista) {
        AnnotatedParameterizedType listaParametrizada = assertInstanceOf(
                AnnotatedParameterizedType.class,
                lista,
                contrato + " deve continuar sendo uma lista parametrizada");

        assertFalse(lista.isAnnotationPresent(Valid.class),
                contrato + " nao deve aplicar @Valid ao container List");
        assertTrue(listaParametrizada.getAnnotatedActualTypeArguments()[0].isAnnotationPresent(Valid.class),
                contrato + " deve aplicar @Valid ao tipo do elemento");
    }
}

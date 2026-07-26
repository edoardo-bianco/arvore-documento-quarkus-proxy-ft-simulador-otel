package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface RepositorioDocumental {

    Uni<ResultadoGravacao> criar(String documentId, ObjectNode documento);

    Uni<ResultadoGravacao> substituir(
            String documentId,
            String versaoEsperada,
            ObjectNode documento);

    Uni<Optional<DocumentoPersistido>> consultar(
            String documentId,
            String correlationId);

    record DocumentoPersistido(ObjectNode documento, String versao) {

        public DocumentoPersistido {
            documento = java.util.Objects.requireNonNull(documento, "documento").deepCopy();
            if (versao == null || versao.isBlank()) {
                throw new IllegalArgumentException("Versão do documento inválida");
            }
        }

        @Override
        public ObjectNode documento() {
            return documento.deepCopy();
        }
    }

    enum ResultadoGravacao {
        GRAVADO,
        CONFLITO
    }
}

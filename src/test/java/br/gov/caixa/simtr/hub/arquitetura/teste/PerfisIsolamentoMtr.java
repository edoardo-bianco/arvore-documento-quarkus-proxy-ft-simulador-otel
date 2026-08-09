package br.gov.caixa.simtr.hub.arquitetura.teste;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public final class PerfisIsolamentoMtr {

    private PerfisIsolamentoMtr() {
    }

    private abstract static class PerfilBase implements QuarkusTestProfile {

        private final String contexto;

        private PerfilBase(String contexto) {
            this.contexto = contexto;
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("simtr.test.isolamento-mtr", contexto);
        }
    }

    public static final class Checklist extends PerfilBase {

        public Checklist() {
            super("checklist");
        }
    }

    public static final class CriacaoDossie extends PerfilBase {

        public CriacaoDossie() {
            super("criacao-dossie");
        }
    }

    public static final class DocumentoDossie extends PerfilBase {

        public DocumentoDossie() {
            super("documento-dossie");
        }
    }

    public static final class FormularioDossie extends PerfilBase {

        public FormularioDossie() {
            super("formulario-dossie");
        }
    }

    public static final class GestaoDocumento extends PerfilBase {

        public GestaoDocumento() {
            super("gestao-documento");
        }
    }
}

package br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao.dto.PreValidacaoSimuladaDto;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.NoSuchElementException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Consulta cenarios imutaveis do demonstrador somente com ativacao explicita. */
@ApplicationScoped
public class PreValidacaoSimuladaAdapter implements ConsultarPreValidacao {

    private static final Map<String, PreValidacaoSimuladaDto> CENARIOS = Map.of(
            "pre-em-analise", new PreValidacaoSimuladaDto("EM_ANALISE_ENVIO_MTR"),
            "pre-conforme", new PreValidacaoSimuladaDto("CONFORME"),
            "pre-nao-conforme", new PreValidacaoSimuladaDto("NAO_CONFORME"));

    private final boolean habilitado;
    private final PreValidacaoSimuladaMapper mapper;

    @Inject
    public PreValidacaoSimuladaAdapter(
            @ConfigProperty(name = "monitoramento.simulador.prevalidacao.habilitado", defaultValue = "false")
            boolean habilitado,
            PreValidacaoSimuladaMapper mapper) {
        this.habilitado = habilitado;
        this.mapper = mapper;
    }

    /** Executa a consulta na assinatura do Uni, inclusive a verificacao da ativacao. */
    @Override
    public Uni<PreValidacaoConsultada> executar(String idDossiePreValidacao) {
        return Uni.createFrom().item(() -> consultar(idDossiePreValidacao));
    }

    private PreValidacaoConsultada consultar(String id) {
        if (!habilitado) {
            throw new IllegalStateException("Simulador de pre-validacao desabilitado.");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Identificador da pre-validacao obrigatorio.");
        }
        var resposta = CENARIOS.get(id);
        if (resposta == null) {
            throw new NoSuchElementException("Pre-validacao nao encontrada no simulador.");
        }
        return mapper.paraModelo(resposta);
    }
}

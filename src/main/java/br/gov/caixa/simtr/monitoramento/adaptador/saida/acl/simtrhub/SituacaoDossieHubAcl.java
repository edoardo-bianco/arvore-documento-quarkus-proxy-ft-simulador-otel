package br.gov.caixa.simtr.monitoramento.adaptador.saida.acl.simtrhub;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarSituacaoDossie;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Consulta o Hub por sua porta publica e preserva a situacao original, sem classificar conclusao. */
@ApplicationScoped
public class SituacaoDossieHubAcl implements ConsultarSituacaoDossie {

    private final ConsultarDossieProduto hub;

    @Inject
    public SituacaoDossieHubAcl(ConsultarDossieProduto hub) {
        this.hub = hub;
    }

    /** Valida e consulta na assinatura; falhas do fornecedor permanecem no fluxo sem retry local. */
    @Override
    public Uni<SituacaoDossieConsultada> executar(String idDossieMtr) {
        return Uni.createFrom().item(() -> identificador(idDossieMtr))
                .onItem().transformToUni(hub::executar)
                .onItem().transform(SituacaoDossieHubAcl::situacao);
    }

    private static IdentificadorDossieProduto identificador(String id) {
        if (id == null || !id.matches("\\d+")) {
            throw identificadorInvalido();
        }
        long valor;
        try {
            valor = Long.parseLong(id);
        } catch (NumberFormatException _) {
            throw identificadorInvalido();
        }
        if (valor <= 0) {
            throw identificadorInvalido();
        }
        return new IdentificadorDossieProduto(valor);
    }

    private static IllegalArgumentException identificadorInvalido() {
        return new IllegalArgumentException("Identificador MTR invalido para consulta.");
    }

    private static SituacaoDossieConsultada situacao(DossieProdutoConsultado resposta) {
        if (resposta == null || resposta.situacaoAtual() == null) {
            throw new IllegalStateException("Resposta do Hub sem situacao do dossie.");
        }
        var original = resposta.situacaoAtual();
        return new SituacaoDossieConsultada(original.id(), original.nome());
    }
}

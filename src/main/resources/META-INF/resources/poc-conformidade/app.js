"use strict";

const CAMINHO_API = "/simtr-hub/v1/conformidade/analises";
const INTERVALO_POLLING_MS = 1500;
const PARECERES = Object.freeze([
  ["CONFORME", "Conforme"],
  ["INCONFORME", "Inconforme"],
  ["INCONCLUSIVO", "Inconclusivo"],
  ["NAO_ANALISADO", "Não analisado"]
]);
const MENSAGENS_HTTP = Object.freeze({
  400: "Os dados enviados são inválidos. Confira os campos e tente novamente.",
  404: "A análise informada não foi encontrada.",
  409: "A análise não está no estado esperado para esta operação.",
  422: "A revisão não corresponde ao resultado preliminar recebido.",
  503: "O serviço de análise está temporariamente indisponível. Tente novamente mais tarde."
});

const elementos = {
  formInicio: document.querySelector("#form-inicio"),
  formRevisao: document.querySelector("#form-revisao"),
  painelInicio: document.querySelector("#painel-inicio"),
  painelAcompanhamento: document.querySelector("#painel-acompanhamento"),
  painelRevisao: document.querySelector("#painel-revisao"),
  painelResultado: document.querySelector("#painel-resultado"),
  indicadorProcessamento: document.querySelector("#indicador-processamento"),
  estadoAnalise: document.querySelector("#estado-analise"),
  mensagemErro: document.querySelector("#mensagem-erro"),
  mensagemStatus: document.querySelector("#mensagem-status"),
  contadorTexto: document.querySelector("#contador-texto"),
  textoDocumento: document.querySelector("#texto-documento"),
  botaoIniciar: document.querySelector("#botao-iniciar"),
  botaoRevisar: document.querySelector("#botao-revisar"),
  botaoNovaAnalise: document.querySelector("#botao-nova-analise"),
  listaApontamentos: document.querySelector("#lista-apontamentos"),
  listaResultado: document.querySelector("#lista-resultado"),
  resumoPreliminar: document.querySelector("#resumo-preliminar"),
  resumoFinal: document.querySelector("#resumo-final"),
  observacaoRevisao: document.querySelector("#observacao-revisao"),
  identidades: {
    correlationId: document.querySelector("#correlation-id"),
    instanceId: document.querySelector("#instance-id"),
    identificadorDocumento: document.querySelector("#identificador-documento-atual"),
    identificadorChecklist: document.querySelector("#identificador-checklist-atual"),
    versaoChecklist: document.querySelector("#versao-checklist-atual")
  }
};

let timerPolling;
let geracaoConsulta = 0;
let analiseAtual;

elementos.formInicio.addEventListener("submit", iniciarAnalise);
elementos.formRevisao.addEventListener("submit", enviarRevisao);
elementos.botaoNovaAnalise.addEventListener("click", reiniciarTela);
elementos.textoDocumento.addEventListener("input", atualizarContadorTexto);
window.addEventListener("beforeunload", cancelarPolling);

atualizarContadorTexto();

async function iniciarAnalise(evento) {
  evento.preventDefault();
  limparMensagens();

  if (!elementos.formInicio.checkValidity()) {
    elementos.formInicio.reportValidity();
    return;
  }

  definirOcupado(elementos.botaoIniciar, true, "Iniciando…");
  geracaoConsulta += 1;
  const geracao = geracaoConsulta;

  const dados = new FormData(elementos.formInicio);
  const solicitacao = {
    identificadorDocumento: dados.get("identificadorDocumento").trim(),
    texto: dados.get("texto").trim(),
    identificadorChecklist: Number(dados.get("identificadorChecklist")),
    versaoChecklist: Number(dados.get("versaoChecklist"))
  };

  try {
    const resposta = await requisitar(CAMINHO_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(solicitacao)
    });

    if (geracao !== geracaoConsulta) {
      return;
    }

    analiseAtual = resposta;
    mostrarAcompanhamento();
    atualizarIdentidades(resposta);
    mostrarProcessamento("Análise aceita. Aguardando o primeiro resultado.");
    agendarConsulta(geracao);
  } catch (erro) {
    mostrarErro(mensagemPublica(erro));
  } finally {
    definirOcupado(elementos.botaoIniciar, false, "Iniciar análise");
  }
}

async function consultarAnalise(geracao) {
  if (!analiseAtual?.instanceId || geracao !== geracaoConsulta) {
    return;
  }

  try {
    const resposta = await requisitar(
      `${CAMINHO_API}/${encodeURIComponent(analiseAtual.instanceId)}`,
      { method: "GET", headers: { Accept: "application/json" } }
    );

    if (geracao !== geracaoConsulta) {
      return;
    }

    analiseAtual = resposta;
    atualizarIdentidades(resposta);
    atualizarTelaPorStatus(resposta, geracao);
  } catch (erro) {
    cancelarPolling();
    mostrarEstado("Falha na consulta", "falhou");
    mostrarErro(mensagemPublica(erro));
  }
}

function atualizarTelaPorStatus(resposta, geracao) {
  switch (resposta.status) {
    case "EM_PROCESSAMENTO":
      mostrarProcessamento("A análise continua em processamento.");
      agendarConsulta(geracao);
      break;
    case "AGUARDANDO_REVISAO":
      cancelarPolling();
      mostrarRevisao(resposta.resultadoPreliminar);
      break;
    case "CONCLUIDA":
      cancelarPolling();
      mostrarResultado(resposta.resultadoFinal);
      break;
    case "FALHOU":
      cancelarPolling();
      mostrarFalha(resposta.mensagemErro);
      break;
    default:
      cancelarPolling();
      mostrarEstado("Estado desconhecido", "falhou");
      mostrarErro("A análise retornou um estado que esta página não reconhece.");
  }
}

async function enviarRevisao(evento) {
  evento.preventDefault();
  limparMensagens();

  if (!elementos.formRevisao.checkValidity()) {
    elementos.formRevisao.reportValidity();
    return;
  }

  const apontamentosOriginais = analiseAtual?.resultadoPreliminar?.apontamentos;
  if (!Array.isArray(apontamentosOriginais) || apontamentosOriginais.length === 0) {
    mostrarErro("O resultado preliminar não possui apontamentos revisáveis.");
    return;
  }

  const revisao = {
    observacao: elementos.observacaoRevisao.value.trim() || null,
    apontamentos: apontamentosOriginais.map((apontamento, indice) => ({
      identificadorApontamento: apontamento.identificadorApontamento,
      nomeApontamento: apontamento.nomeApontamento,
      parecer: campoRevisao("parecer", indice).value,
      justificativa: campoRevisao("justificativa", indice).value.trim(),
      evidencia: campoRevisao("evidencia", indice).value.trim() || null,
      confianca: apontamento.confianca
    }))
  };

  definirOcupado(elementos.botaoRevisar, true, "Enviando…");
  try {
    await requisitar(
      `${CAMINHO_API}/${encodeURIComponent(analiseAtual.instanceId)}/revisao`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(revisao)
      }
    );

    elementos.painelRevisao.hidden = true;
    mostrarProcessamento("Revisão aceita. Aguardando a conclusão da análise.");
    agendarConsulta(geracaoConsulta);
  } catch (erro) {
    mostrarErro(mensagemPublica(erro));
  } finally {
    definirOcupado(elementos.botaoRevisar, false, "Enviar revisão");
  }
}

function mostrarAcompanhamento() {
  elementos.painelInicio.hidden = true;
  elementos.painelAcompanhamento.hidden = false;
  elementos.painelAcompanhamento.scrollIntoView({ block: "start" });
}

function mostrarProcessamento(mensagem) {
  elementos.painelRevisao.hidden = true;
  elementos.painelResultado.hidden = true;
  elementos.indicadorProcessamento.hidden = false;
  mostrarEstado("Em processamento", "processamento");
  mostrarStatus(mensagem);
  atualizarEtapas("processamento");
}

function mostrarRevisao(resultado) {
  elementos.indicadorProcessamento.hidden = true;
  elementos.painelResultado.hidden = true;

  if (!resultado || !Array.isArray(resultado.apontamentos)) {
    mostrarFalha("O resultado preliminar está indisponível.");
    return;
  }

  elementos.resumoPreliminar.textContent = resultado.resumo || "Resultado preliminar disponível.";
  elementos.listaApontamentos.replaceChildren(
    ...resultado.apontamentos.map(criarApontamentoRevisao)
  );
  elementos.observacaoRevisao.value = "";
  elementos.painelRevisao.hidden = false;
  mostrarEstado("Aguardando revisão", "aguardando");
  mostrarStatus("A análise precisa da sua revisão para continuar.");
  atualizarEtapas("revisao");

  const primeiroCampo = elementos.listaApontamentos.querySelector("select");
  primeiroCampo?.focus();
}

function mostrarResultado(resultado) {
  elementos.indicadorProcessamento.hidden = true;
  elementos.painelRevisao.hidden = true;

  elementos.resumoFinal.textContent = resultado?.resumo || "Análise concluída.";
  elementos.listaResultado.replaceChildren(
    ...(resultado?.apontamentos || []).map(criarApontamentoResultado)
  );
  elementos.painelResultado.hidden = false;
  mostrarEstado("Concluída", "concluida");
  mostrarStatus("Análise concluída com revisão humana registrada.");
  atualizarEtapas("conclusao");
  elementos.botaoNovaAnalise.focus();
}

function mostrarFalha(mensagem) {
  elementos.indicadorProcessamento.hidden = true;
  elementos.painelRevisao.hidden = true;
  elementos.painelResultado.hidden = true;
  mostrarEstado("Falhou", "falhou");
  mostrarErro(mensagem || "A análise não pôde ser concluída.");
  atualizarEtapas("conclusao");
}

function criarApontamentoRevisao(apontamento, indice) {
  const artigo = criarElemento("article", "apontamento");
  artigo.setAttribute("aria-labelledby", `nome-apontamento-${indice}`);

  const cabecalho = criarElemento("div", "apontamento-cabecalho");
  const identidade = document.createElement("div");
  const identificador = criarElemento(
    "p",
    "apontamento-identificador",
    `Apontamento ${apontamento.identificadorApontamento}`
  );
  const nome = criarElemento("h4", "apontamento-nome", apontamento.nomeApontamento);
  nome.id = `nome-apontamento-${indice}`;
  identidade.append(identificador, nome);
  const confianca = criarElemento(
    "p",
    "apontamento-confianca",
    `Confiança: ${formatarConfianca(apontamento.confianca)}`
  );
  cabecalho.append(identidade, confianca);

  const campos = criarElemento("div", "apontamento-campos");
  campos.append(
    criarCampoParecer(apontamento, indice),
    criarCampoTexto("justificativa", "Justificativa", apontamento.justificativa, indice, true),
    criarCampoTexto("evidencia", "Evidência", apontamento.evidencia, indice, false, "campo-evidencia")
  );
  artigo.append(cabecalho, campos);
  return artigo;
}

function criarCampoParecer(apontamento, indice) {
  const wrapper = criarElemento("div", "campo");
  const id = `parecer-${indice}`;
  const rotulo = criarElemento("label", "", "Parecer");
  rotulo.htmlFor = id;
  const select = document.createElement("select");
  select.id = id;
  select.name = id;
  select.required = true;

  PARECERES.forEach(([valor, texto]) => {
    const option = document.createElement("option");
    option.value = valor;
    option.textContent = texto;
    option.selected = valor === apontamento.parecer;
    select.append(option);
  });

  wrapper.append(rotulo, select);
  return wrapper;
}

function criarCampoTexto(nome, rotuloTexto, valor, indice, obrigatorio, classeAdicional = "") {
  const wrapper = criarElemento("div", `campo ${classeAdicional}`.trim());
  const id = `${nome}-${indice}`;
  const rotulo = criarElemento("label", "", rotuloTexto);
  rotulo.htmlFor = id;
  const textarea = document.createElement("textarea");
  textarea.id = id;
  textarea.name = id;
  textarea.rows = 4;
  textarea.required = obrigatorio;
  textarea.value = valor || "";
  wrapper.append(rotulo, textarea);
  return wrapper;
}

function criarApontamentoResultado(apontamento, indice) {
  const artigo = criarElemento("article", "apontamento");
  const nome = criarElemento("h4", "apontamento-nome", apontamento.nomeApontamento);
  nome.id = `nome-resultado-${indice}`;
  artigo.setAttribute("aria-labelledby", nome.id);
  artigo.append(
    criarElemento(
      "p",
      "apontamento-identificador",
      `Apontamento ${apontamento.identificadorApontamento} · Confiança ${formatarConfianca(apontamento.confianca)}`
    ),
    nome,
    criarListaResultado(apontamento)
  );
  return artigo;
}

function criarListaResultado(apontamento) {
  const lista = document.createElement("dl");
  lista.append(
    criarTermoResultado("Parecer", formatarParecer(apontamento.parecer)),
    criarTermoResultado("Justificativa", apontamento.justificativa),
    criarTermoResultado("Evidência", apontamento.evidencia || "Não informada")
  );
  return lista;
}

function criarTermoResultado(termo, valor) {
  const grupo = criarElemento("div", "resultado-campo");
  grupo.append(criarElemento("dt", "", termo), criarElemento("dd", "", valor || "—"));
  return grupo;
}

function atualizarIdentidades(resposta) {
  Object.entries(elementos.identidades).forEach(([campo, elemento]) => {
    elemento.textContent = resposta?.[campo] ?? "—";
  });
}

function mostrarEstado(texto, estado) {
  elementos.estadoAnalise.textContent = texto;
  elementos.estadoAnalise.dataset.estado = estado;
}

function atualizarEtapas(etapaAtual) {
  const ordem = ["inicio", "processamento", "revisao", "conclusao"];
  const indiceAtual = ordem.indexOf(etapaAtual);
  document.querySelectorAll("[data-etapa]").forEach((etapa) => {
    const indice = ordem.indexOf(etapa.dataset.etapa);
    etapa.classList.toggle("etapa-atual", indice === indiceAtual);
    etapa.classList.toggle("etapa-concluida", indice < indiceAtual);
  });
}

function agendarConsulta(geracao) {
  cancelarPolling();
  timerPolling = window.setTimeout(() => consultarAnalise(geracao), INTERVALO_POLLING_MS);
}

function cancelarPolling() {
  if (timerPolling !== undefined) {
    window.clearTimeout(timerPolling);
    timerPolling = undefined;
  }
}

function reiniciarTela() {
  cancelarPolling();
  geracaoConsulta += 1;
  analiseAtual = undefined;
  elementos.formInicio.reset();
  elementos.formRevisao.reset();
  elementos.listaApontamentos.replaceChildren();
  elementos.listaResultado.replaceChildren();
  elementos.painelInicio.hidden = false;
  elementos.painelAcompanhamento.hidden = true;
  elementos.painelRevisao.hidden = true;
  elementos.painelResultado.hidden = true;
  elementos.indicadorProcessamento.hidden = true;
  atualizarIdentidades();
  atualizarEtapas("inicio");
  limparMensagens();
  atualizarContadorTexto();
  document.querySelector("#identificador-documento").focus();
}

async function requisitar(caminho, opcoes) {
  let resposta;
  try {
    resposta = await window.fetch(caminho, opcoes);
  } catch {
    throw new Error("Não foi possível conectar ao serviço de análise.");
  }

  if (!resposta.ok) {
    const erro = new Error(MENSAGENS_HTTP[resposta.status] || "O serviço retornou uma resposta inesperada.");
    erro.statusHttp = resposta.status;
    throw erro;
  }

  if (resposta.status === 202 && opcoes.method === "PUT") {
    return undefined;
  }

  return resposta.json();
}

function mensagemPublica(erro) {
  if (erro?.statusHttp && MENSAGENS_HTTP[erro.statusHttp]) {
    return MENSAGENS_HTTP[erro.statusHttp];
  }
  return erro?.message || "Não foi possível concluir a operação.";
}

function mostrarErro(mensagem) {
  elementos.mensagemErro.textContent = mensagem;
  elementos.mensagemErro.hidden = false;
  elementos.mensagemErro.focus({ preventScroll: false });
}

function mostrarStatus(mensagem) {
  elementos.mensagemStatus.textContent = mensagem;
  elementos.mensagemStatus.hidden = false;
}

function limparMensagens() {
  elementos.mensagemErro.hidden = true;
  elementos.mensagemErro.textContent = "";
  elementos.mensagemStatus.hidden = true;
  elementos.mensagemStatus.textContent = "";
}

function atualizarContadorTexto() {
  elementos.contadorTexto.textContent = `${elementos.textoDocumento.value.length.toLocaleString("pt-BR")} / 20.000`;
}

function definirOcupado(botao, ocupado, texto) {
  botao.disabled = ocupado;
  botao.textContent = texto;
  botao.setAttribute("aria-busy", String(ocupado));
}

function campoRevisao(nome, indice) {
  return document.querySelector(`#${nome}-${indice}`);
}

function criarElemento(tag, classe, texto) {
  const elemento = document.createElement(tag);
  if (classe) {
    elemento.className = classe;
  }
  if (texto !== undefined && texto !== null) {
    elemento.textContent = texto;
  }
  return elemento;
}

function formatarConfianca(valor) {
  return typeof valor === "number"
    ? valor.toLocaleString("pt-BR", { style: "percent", maximumFractionDigits: 1 })
    : "—";
}

function formatarParecer(parecer) {
  return PARECERES.find(([valor]) => valor === parecer)?.[1] || parecer || "—";
}

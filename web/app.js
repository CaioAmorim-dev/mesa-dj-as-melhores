const $ = (selector) => document.querySelector(selector);
const tracksElement = $("#tracks");
const musicSelect = $("#music-select");
const dialog = $("#add-dialog");
const toast = $("#toast");
let state = null;
let toastTimer;

async function api(path, params = {}, method = "POST") {
    const query = new URLSearchParams(params).toString();
    const response = await fetch(`${path}${query ? `?${query}` : ""}`, { method });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || "Não foi possível completar a ação.");
    return data;
}

function notify(message, error = false) {
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.className = `${error ? "error " : ""}show`;
    toastTimer = setTimeout(() => toast.className = "", 2800);
}

function meter() {
    return Array.from({ length: 18 }, (_, i) =>
        `<span style="--height:${10 + ((i * 11) % 22)}px"></span>`).join("");
}

function trackCard(track) {
    const playing = track.state === "TOCANDO";
    return `
        <article class="track ${playing ? "playing" : ""}" data-name="${escapeHtml(track.name)}">
            <div class="track-top">
                <div>
                    <div class="track-name">${escapeHtml(track.name)}</div>
                    <div class="track-pattern" title="${escapeHtml(track.pattern)}">${escapeHtml(track.pattern)}</div>
                </div>
                <span class="state">${track.state}</span>
            </div>
            <div class="meter">${meter()}</div>
            <div class="track-bottom">
                <span class="track-meta">${track.bpm} BPM · ${track.grid} · ${track.stepMs}ms</span>
                <div class="track-actions">
                    <button data-action="play" title="Tocar" aria-label="Tocar ${escapeHtml(track.name)}">▶</button>
                    <button data-action="pause" title="Pausar" aria-label="Pausar ${escapeHtml(track.name)}">Ⅱ</button>
                    <button data-action="solo" title="Solo" aria-label="Solo ${escapeHtml(track.name)}">S</button>
                    <button data-action="stop" class="danger" title="Encerrar" aria-label="Encerrar ${escapeHtml(track.name)}">■</button>
                </div>
            </div>
        </article>`;
}

function render(nextState) {
    state = nextState;
    const music = state.currentMusic;
    if (music) {
        $("#music-title").textContent = music.title;
        $("#music-meta").textContent = `${music.key} · ${music.bpm} BPM original`;
    }

    const catalogChanged = musicSelect.options.length !== state.catalog.length;
    if (catalogChanged) {
        musicSelect.innerHTML = state.catalog.map(item =>
            `<option value="${escapeHtml(item.id)}">${escapeHtml(item.title)}</option>`).join("");
    }
    if (music) musicSelect.value = music.id;

    $("#track-count").textContent = state.tracks.length;
    tracksElement.innerHTML = state.tracks.map(trackCard).join("");

    const commonBpm = state.tracks[0]?.bpm || music?.bpm || 120;
    if (document.activeElement !== $("#master-bpm")) $("#master-bpm").value = commonBpm;
    if (document.activeElement !== $("#track-bpm")) $("#track-bpm").value = music?.bpm || 120;

    const extraSelect = $("#extra-select");
    extraSelect.innerHTML = `<option value="">Faixa personalizada</option>` + state.extras.map(name =>
        `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`).join("");
}

function escapeHtml(value) {
    return String(value).replace(/[&<>'"]/g, char => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    })[char]);
}

async function act(path, params, message) {
    try {
        render(await api(path, params));
        if (message) notify(message);
    } catch (error) {
        notify(error.message, true);
    }
}

tracksElement.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const name = button.closest(".track").dataset.name;
    const action = button.dataset.action;
    act(`/api/track/${action}`, { name }, `${name}: ${action.toUpperCase()}`);
});

musicSelect.addEventListener("change", () =>
    act("/api/music", { id: musicSelect.value }, "Música carregada em silêncio."));
$("#play-all").addEventListener("click", () => act("/api/all/play", {}, "Todas as faixas estão tocando."));
$("#pause-all").addEventListener("click", () => act("/api/all/pause", {}, "Mesa em silêncio."));
$("#sync").addEventListener("click", () => act("/api/sync", {}, "Faixas sincronizadas."));
$("#apply-bpm").addEventListener("click", () =>
    act("/api/bpm", { target: "all", value: $("#master-bpm").value }, "BPM mestre atualizado."));

$("#open-add").addEventListener("click", () => dialog.showModal());
$("#close-dialog").addEventListener("click", () => dialog.close());
$("#extra-select").addEventListener("change", (event) => {
    if (event.target.value) $("#track-name").value = event.target.value;
});
$("#add-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await act("/api/track/add", {
        name: $("#track-name").value,
        sound: $("#track-sound").value,
        bpm: $("#track-bpm").value
    }, "Nova faixa criada e tocando.");
    dialog.close();
    event.target.reset();
});

$("#shutdown").addEventListener("click", async () => {
    if (!confirm("Deseja desligar a mesa e encerrar todas as threads?")) return;
    try {
        await api("/api/shutdown");
        document.body.innerHTML = `<main class="app-shell"><section class="hero panel"><div class="now-playing"><p class="eyebrow">SESSÃO ENCERRADA</p><h2>Mesa desligada</h2><p class="muted">Todas as threads e o sintetizador MIDI foram finalizados.</p></div></section></main>`;
    } catch (error) {
        notify(error.message, true);
    }
});

async function refresh() {
    try {
        render(await api("/api/state", {}, "GET"));
    } catch (error) {
        notify("Servidor desconectado.", true);
    }
}

refresh();
setInterval(refresh, 1500);

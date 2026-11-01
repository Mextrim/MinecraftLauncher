// State
let config = {};
let allVersions = [];
let isDownloading = false;
let isGameRunning = false;

const THEMES = {
  emerald: { name: 'Изумруд', label: 'Изумрудная (Emerald)' },
  diamond: { name: 'Алмаз', label: 'Алмазная (Diamond)' },
  nether: { name: 'Незерит', label: 'Незерит & Лава' },
  amethyst: { name: 'Аметист', label: 'Аметистовая (Amethyst)' },
  redstone: { name: 'Редстоун', label: 'Редстоун (Redstone)' }
};

// DOM Elements
const tabs = document.querySelectorAll('.nav-tab');
const tabContents = document.querySelectorAll('.tab-content');

// Window Controls
document.getElementById('btn-minimize').addEventListener('click', () => window.api.minimize());
document.getElementById('btn-maximize').addEventListener('click', () => window.api.maximize());
document.getElementById('btn-close').addEventListener('click', () => window.api.close());

// Tab Switching
tabs.forEach(tab => {
  tab.addEventListener('click', () => {
    tabs.forEach(t => t.classList.remove('active'));
    tabContents.forEach(c => c.classList.remove('active'));

    tab.classList.add('active');
    const target = document.getElementById(tab.dataset.tab);
    if (target) target.classList.add('active');
  });
});

// UI Elements
const inputNickname = document.getElementById('input-nickname');
const selectVersion = document.getElementById('select-version');
const checkSnapshots = document.getElementById('check-snapshots');
const btnRefreshVersions = document.getElementById('btn-refresh-versions');
const labelStatus = document.getElementById('label-status');
const labelPercent = document.getElementById('label-percent');
const progressBar = document.getElementById('progress-bar');
const btnPlay = document.getElementById('btn-play');
const btnPlayText = document.getElementById('btn-play-text');
const btnStopGame = document.getElementById('btn-stop-game');
const gameStatusBadge = document.getElementById('game-status-badge');

// Settings Elements
const sliderRam = document.getElementById('slider-ram');
const labelRamVal = document.getElementById('label-ram-val');
const inputJavaPath = document.getElementById('input-java-path');
const btnBrowseJava = document.getElementById('btn-browse-java');
const btnAutodetectJava = document.getElementById('btn-autodetect-java');
const listDetectedJavas = document.getElementById('list-detected-javas');
const inputGameDir = document.getElementById('input-gamedir');
const btnBrowseGameDir = document.getElementById('btn-browse-gamedir');
const inputWidth = document.getElementById('input-width');
const inputHeight = document.getElementById('input-height');
const checkFullscreen = document.getElementById('check-fullscreen');
const inputJvmArgs = document.getElementById('input-jvm-args');

// Theme Switcher Elements
const btnThemeDropdown = document.getElementById('btn-theme-dropdown');
const themePopupMenu = document.getElementById('theme-popup-menu');
const currentThemeLabel = document.getElementById('current-theme-label');
const themeCards = document.querySelectorAll('.theme-card');

// Console Elements
const consoleOutput = document.getElementById('console-output');
const consoleStatusPill = document.getElementById('console-status-pill');
const btnClearLogs = document.getElementById('btn-clear-logs');
const btnCopyLogs = document.getElementById('btn-copy-logs');
const btnKillGame = document.getElementById('btn-kill-game');

// Log appender
function appendLog(line) {
  const time = new Date().toLocaleTimeString();
  consoleOutput.textContent += `[${time}] ${line}\n`;
  consoleOutput.parentElement.scrollTop = consoleOutput.parentElement.scrollHeight;
}

// RAM Slider logic
function updateRamDisplay(val) {
  const gb = (val / 1024).toFixed(1);
  labelRamVal.textContent = `${val} МБ (${gb} ГБ)`;
}

sliderRam.addEventListener('input', (e) => {
  const val = parseInt(e.target.value, 10);
  updateRamDisplay(val);
  config.ramMb = val;
  window.api.saveConfig(config);
});

// ================= THEMES ENGINE =================
function applyTheme(themeId) {
  const theme = THEMES[themeId] ? themeId : 'emerald';
  document.documentElement.setAttribute('data-theme', theme);
  currentThemeLabel.textContent = THEMES[theme].name;

  // Update gallery cards active state
  themeCards.forEach(card => {
    if (card.dataset.themeId === theme) {
      card.classList.add('active');
    } else {
      card.classList.remove('active');
    }
  });

  config.theme = theme;
  window.api.saveConfig(config);
}

// Header theme dropdown toggle
btnThemeDropdown.addEventListener('click', (e) => {
  e.stopPropagation();
  themePopupMenu.classList.toggle('hidden');
});

// Close theme dropdown on outside click
document.addEventListener('click', () => {
  if (!themePopupMenu.classList.contains('hidden')) {
    themePopupMenu.classList.add('hidden');
  }
});

// Theme selection from dropdown
document.querySelectorAll('.theme-popup-item').forEach(item => {
  item.addEventListener('click', (e) => {
    e.stopPropagation();
    const id = item.dataset.themeId;
    applyTheme(id);
    themePopupMenu.classList.add('hidden');
    labelStatus.textContent = `Тема изменена: ${THEMES[id].label}`;
  });
});

// Theme selection from settings gallery
themeCards.forEach(card => {
  card.addEventListener('click', () => {
    const id = card.dataset.themeId;
    applyTheme(id);
    labelStatus.textContent = `Тема изменена: ${THEMES[id].label}`;
  });
});

// ================= INITIALIZATION =================
async function init() {
  appendLog('[UI] Инициализация Electron + Rust Launcher...');

  // 1. Load config
  config = await window.api.loadConfig();
  inputNickname.value = config.nickname || 'Player';
  checkSnapshots.checked = !!config.showSnapshots;
  sliderRam.value = config.ramMb || 4096;
  updateRamDisplay(sliderRam.value);
  inputJavaPath.value = config.javaPath || '';
  inputGameDir.value = config.gameDir || '';
  inputWidth.value = config.windowWidth || 1280;
  inputHeight.value = config.windowHeight || 720;
  checkFullscreen.checked = !!config.fullscreen;
  inputJvmArgs.value = config.customJvmArgs || '-XX:+UseG1GC';

  // Apply theme
  applyTheme(config.theme || 'emerald');

  // 2. Fetch Java & Versions concurrently
  detectJava();
  loadVersions();
}

// Save inputs on change
inputNickname.addEventListener('input', () => {
  config.nickname = inputNickname.value.trim() || 'Player';
  window.api.saveConfig(config);
});

selectVersion.addEventListener('change', () => {
  config.selectedVersion = selectVersion.value;
  window.api.saveConfig(config);
});

checkSnapshots.addEventListener('change', () => {
  config.showSnapshots = checkSnapshots.checked;
  window.api.saveConfig(config);
  renderVersions();
});

inputJavaPath.addEventListener('change', () => {
  config.javaPath = inputJavaPath.value.trim();
  window.api.saveConfig(config);
});

inputGameDir.addEventListener('change', () => {
  config.gameDir = inputGameDir.value.trim();
  window.api.saveConfig(config);
});

inputWidth.addEventListener('change', () => {
  config.windowWidth = parseInt(inputWidth.value, 10) || 1280;
  window.api.saveConfig(config);
});

inputHeight.addEventListener('change', () => {
  config.windowHeight = parseInt(inputHeight.value, 10) || 720;
  window.api.saveConfig(config);
});

checkFullscreen.addEventListener('change', () => {
  config.fullscreen = checkFullscreen.checked;
  window.api.saveConfig(config);
});

inputJvmArgs.addEventListener('change', () => {
  config.customJvmArgs = inputJvmArgs.value.trim();
  window.api.saveConfig(config);
});

// Java Detection
async function detectJava() {
  listDetectedJavas.innerHTML = '<li class="loading-item">Поиск Java через Rust Core...</li>';
  try {
    const javas = await window.api.detectJava();
    listDetectedJavas.innerHTML = '';
    if (!javas || javas.length === 0) {
      listDetectedJavas.innerHTML = '<li class="loading-item">Java не найдена. Укажите путь вручную.</li>';
      return;
    }

    if (!inputJavaPath.value && javas.length > 0) {
      inputJavaPath.value = javas[0].path;
      config.javaPath = javas[0].path;
      window.api.saveConfig(config);
    }

    javas.forEach(j => {
      const li = document.createElement('li');
      li.textContent = `${j.name} (${j.path})`;
      if (inputJavaPath.value.toLowerCase() === j.path.toLowerCase()) {
        li.classList.add('selected');
      }
      li.addEventListener('click', () => {
        document.querySelectorAll('#list-detected-javas li').forEach(el => el.classList.remove('selected'));
        li.classList.add('selected');
        inputJavaPath.value = j.path;
        config.javaPath = j.path;
        window.api.saveConfig(config);
        labelStatus.textContent = `Выбрана Java: ${j.name}`;
      });
      listDetectedJavas.appendChild(li);
    });
  } catch (e) {
    listDetectedJavas.innerHTML = `<li class="loading-item">Ошибка поиска: ${e.message}</li>`;
  }
}

btnAutodetectJava.addEventListener('click', detectJava);

btnBrowseJava.addEventListener('click', async () => {
  const filePath = await window.api.pickFile({
    title: 'Выберите javaw.exe или java.exe',
    filters: [{ name: 'Java Executable', extensions: ['exe'] }]
  });
  if (filePath) {
    inputJavaPath.value = filePath;
    config.javaPath = filePath;
    window.api.saveConfig(config);
  }
});

btnBrowseGameDir.addEventListener('click', async () => {
  const folderPath = await window.api.pickFolder(inputGameDir.value);
  if (folderPath) {
    inputGameDir.value = folderPath;
    config.gameDir = folderPath;
    window.api.saveConfig(config);
  }
});

let currentModLoader = 'all';

// Modloader descriptions
const MOD_DESCRIPTIONS = {
  optifine: '✨ Оптимизация OptiFine HD: Двукратный прирост FPS, поддержка реалистичных шейдеров BSL/Complementary, зум на C, динамический свет от факелов.',
  fabric: '🧵 Модлоадер Fabric: Сверхбыстрый и легковесный движок модов. Полная поддержка модов Sodium, Iris Shaders, Lithium и современных модификаций.',
  forge: '🔨 Модлоадер Minecraft Forge: Классическая богатая база модов. Поддержка глобальных модов Create, Twilight Forest, Tinkers Construct.',
  forge_optifine: '⚔️ Сборка Forge + OptiFine: Мощнейший гибрид — запускайте любимые моды Forge вместе с шейдерами и FPS-бустом OptiFine в одном клиенте!',
  vanilla: '⚡ Чистая официальная версия Mojang без модификаций. Максимальная стабильность.'
};

function updateModDescription(loader) {
  const box = document.getElementById('mod-description-box');
  if (box) {
    box.textContent = MOD_DESCRIPTIONS[loader] || MOD_DESCRIPTIONS.vanilla;
  }
}

// Versions Fetching
async function loadVersions() {
  selectVersion.innerHTML = '<option value="">Загрузка версий с Mojang...</option>';
  try {
    const manifest = await window.api.getManifest();
    const vanillaList = manifest.versions || [];

    // Inject modded builds for popular versions: OptiFine, Fabric, Forge, Forge + OptiFine
    const popularBases = ['1.21.4', '1.20.4', '1.16.5', '1.12.2'];
    const modded = [];
    popularBases.forEach(baseVer => {
      const match = vanillaList.find(v => v.id === baseVer);
      const url = match ? match.url : '';
      modded.push(
        { id: `${baseVer} (OptiFine)`, baseVersion: baseVer, modLoader: 'optifine', type: 'release', url, badge: '✨ OPTIFINE' },
        { id: `${baseVer} (Fabric)`, baseVersion: baseVer, modLoader: 'fabric', type: 'release', url, badge: '🧵 FABRIC' },
        { id: `${baseVer} (Forge)`, baseVersion: baseVer, modLoader: 'forge', type: 'release', url, badge: '🔨 FORGE' },
        { id: `${baseVer} (Forge + OptiFine)`, baseVersion: baseVer, modLoader: 'forge_optifine', type: 'release', url, badge: '⚔️ FORGE+OPTIFINE' }
      );
    });

    allVersions = [...modded, ...vanillaList];
    renderVersions(manifest.latest ? manifest.latest.release : '');
  } catch (e) {
    appendLog(`[Error] Не удалось загрузить манифест: ${e.message}`);
    labelStatus.textContent = `Ошибка загрузки версий: ${e.message}`;
    selectVersion.innerHTML = '<option value="">Ошибка загрузки версий</option>';
  }
}

function renderVersions(defaultRelease) {
  selectVersion.innerHTML = '';
  let filtered = config.showSnapshots
    ? allVersions
    : allVersions.filter(v => v.type === 'release');

  if (currentModLoader !== 'all') {
    filtered = filtered.filter(v => (v.modLoader || 'vanilla') === currentModLoader);
  }

  filtered.forEach(v => {
    const opt = document.createElement('option');
    opt.value = v.id;
    const badge = v.badge || (v.type === 'release' ? 'РЕЛИЗ' : v.type.toUpperCase());
    opt.textContent = `${v.id}  [${badge}]`;
    opt.dataset.url = v.url || '';
    opt.dataset.baseVersion = v.baseVersion || v.id;
    opt.dataset.modLoader = v.modLoader || 'vanilla';
    selectVersion.appendChild(opt);
  });

  const target = config.selectedVersion || defaultRelease || (filtered[0] ? filtered[0].id : '');
  if (target && Array.from(selectVersion.options).some(o => o.value === target)) {
    selectVersion.value = target;
  } else if (filtered[0]) {
    selectVersion.value = filtered[0].id;
  }

  const selectedOpt = selectVersion.selectedOptions[0];
  const loader = selectedOpt?.dataset.modLoader || 'vanilla';
  updateModDescription(loader);
  labelStatus.textContent = 'Готов к игре';
}

selectVersion.addEventListener('change', () => {
  const selectedOpt = selectVersion.selectedOptions[0];
  const loader = selectedOpt?.dataset.modLoader || 'vanilla';
  updateModDescription(loader);
});

// Modloader pill filters
document.querySelectorAll('.modloader-pill-row .chip-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.modloader-pill-row .chip-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentModLoader = btn.dataset.loader;
    renderVersions();
  });
});

// Quick Folders
const btnOpenMods = document.getElementById('btn-open-mods');
if (btnOpenMods) {
  btnOpenMods.addEventListener('click', () => {
    window.api.openFolderSub('mods');
    labelStatus.textContent = 'Открыта папка: .minecraft/mods';
  });
}

const btnOpenShaders = document.getElementById('btn-open-shaders');
if (btnOpenShaders) {
  btnOpenShaders.addEventListener('click', () => {
    window.api.openFolderSub('shaderpacks');
    labelStatus.textContent = 'Открыта папка: .minecraft/shaderpacks';
  });
}

btnRefreshVersions.addEventListener('click', loadVersions);

// Download progress listener
window.api.onDownloadProgress((data) => {
  labelStatus.textContent = data.status || 'Загрузка...';
  if (typeof data.percent === 'number') {
    progressBar.style.width = `${Math.min(100, Math.max(0, data.percent))}%`;
    labelPercent.textContent = `${Math.round(data.percent)}%`;
  }
});

// Game logs listener
window.api.onGameLog((line) => {
  appendLog(line);
});

// Game exited listener
window.api.onGameExited((code) => {
  isGameRunning = false;
  setGameRunningState(false);
  labelStatus.textContent = 'Minecraft закрыт';
  appendLog(`[RustCore] Процесс Minecraft завершился с кодом ${code}`);
});

function setGameRunningState(running) {
  if (running) {
    gameStatusBadge.classList.remove('hidden');
    consoleStatusPill.classList.remove('hidden');
    btnStopGame.classList.remove('hidden');
    btnKillGame.classList.remove('hidden');
    btnPlayText.textContent = 'В ИГРЕ';
    btnPlay.disabled = true;
  } else {
    gameStatusBadge.classList.add('hidden');
    consoleStatusPill.classList.add('hidden');
    btnStopGame.classList.add('hidden');
    btnKillGame.classList.add('hidden');
    btnPlayText.textContent = 'ИГРАТЬ';
    btnPlay.disabled = false;
  }
}

// Play Button
btnPlay.addEventListener('click', async () => {
  const versionId = selectVersion.value;
  if (!versionId) {
    alert('Пожалуйста, выберите версию игры!');
    return;
  }

  const selectedOpt = selectVersion.options[selectVersion.selectedIndex];
  const url = selectedOpt ? selectedOpt.dataset.url : '';

  try {
    isDownloading = true;
    btnPlay.disabled = true;
    btnPlayText.textContent = 'ЗАГРУЗКА...';
    progressBar.style.width = '0%';
    labelPercent.textContent = '0%';
    labelStatus.textContent = `Подготовка Minecraft ${versionId}...`;

    appendLog(`================== Запуск Minecraft ${versionId} ==================`);

    // 1. Download
    await window.api.startDownload({
      versionId,
      url,
      gameDir: config.gameDir || ''
    });

    labelStatus.textContent = 'Файлы готовы. Запуск JVM...';
    progressBar.style.width = '100%';
    labelPercent.textContent = '100%';

    // 2. Launch
    isDownloading = false;
    isGameRunning = true;
    setGameRunningState(true);
    labelStatus.textContent = `Minecraft ${versionId} запущен!`;

    await window.api.launchGame({
      versionId,
      username: config.nickname || 'Player',
      ramMb: config.ramMb || 4096,
      javaPath: config.javaPath || 'java',
      gameDir: config.gameDir || '',
      width: config.windowWidth || 1280,
      height: config.windowHeight || 720,
      fullscreen: !!config.fullscreen
    });

  } catch (err) {
    isDownloading = false;
    isGameRunning = false;
    setGameRunningState(false);
    labelStatus.textContent = `Ошибка: ${err.message}`;
    appendLog(`[Error] ${err.message}`);
    alert(`Не удалось запустить игру:\n${err.message}`);
  }
});

// Stop / Kill Game
async function stopGame() {
  await window.api.killGame();
  appendLog('[RustCore] Остановка процесса запрошена пользователем.');
}
btnStopGame.addEventListener('click', stopGame);
btnKillGame.addEventListener('click', stopGame);

// Console buttons
btnClearLogs.addEventListener('click', () => {
  consoleOutput.textContent = '';
});

btnCopyLogs.addEventListener('click', () => {
  navigator.clipboard.writeText(consoleOutput.textContent);
  labelStatus.textContent = 'Логи скопированы в буфер обмена!';
});

// Start
init();

const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const https = require('https');
const { spawn, execSync } = require('child_process');

// Global safety handlers to prevent crash popups
process.on('uncaughtException', (err) => {
  console.error('[Main UncaughtException]', err);
});

process.on('unhandledRejection', (reason) => {
  console.error('[Main UnhandledRejection]', reason);
});

let mainWindow;
let activeRustProcess = null;

function getRustCorePath() {
  const exeDir = path.dirname(app.getPath('exe'));
  const candidates = [
    // Next to executable
    path.join(exeDir, 'bin', 'rust-core.exe'),
    path.join(exeDir, 'rust-core.exe'),
    // In resources directory (standard for extraResources)
    path.join(process.resourcesPath, 'bin', 'rust-core.exe'),
    path.join(process.resourcesPath, 'rust-core.exe'),
    path.join(process.resourcesPath, 'app.asar.unpacked', 'bin', 'rust-core.exe'),
    // In dev tree
    path.join(__dirname.replace(/[/\\]app\.asar[/\\]?$/, ''), 'bin', 'rust-core.exe'),
    path.join(process.cwd(), 'bin', 'rust-core.exe'),
    path.join(process.cwd(), 'electron-launcher', 'bin', 'rust-core.exe'),
    path.join(__dirname, 'bin', 'rust-core.exe')
  ];

  for (const candidate of candidates) {
    // Windows cannot spawn binaries inside an ASAR archive file
    if (candidate.includes('app.asar') && !candidate.includes('app.asar.unpacked')) {
      continue;
    }
    try {
      if (fs.existsSync(candidate)) {
        return candidate;
      }
    } catch (e) {}
  }

  return path.join(process.resourcesPath, 'bin', 'rust-core.exe');
}

// Direct Node.js fallback for manifest if rust-core fails or is blocked
function fetchManifestFallback() {
  return new Promise((resolve, reject) => {
    https.get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', {
      headers: { 'User-Agent': 'MinecraftLauncher/2.0' }
    }, (res) => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(e);
        }
      });
    }).on('error', reject);
  });
}

// Direct Node.js fallback for java detection
function detectJavaFallback() {
  const list = [];
  const checked = new Set();

  function addJava(p, name) {
    const norm = path.normalize(p).toLowerCase();
    if (checked.has(norm)) return;
    checked.add(norm);
    if (fs.existsSync(p)) {
      list.push({ path: p, name: name || 'Java Runtime' });
    }
  }

  // Check JAVA_HOME
  if (process.env.JAVA_HOME) {
    addJava(path.join(process.env.JAVA_HOME, 'bin', 'javaw.exe'), 'JAVA_HOME');
    addJava(path.join(process.env.JAVA_HOME, 'bin', 'java.exe'), 'JAVA_HOME');
  }

  // Check where.exe java
  try {
    const whereOut = execSync('where.exe javaw java 2>nul', { encoding: 'utf8' });
    for (const line of whereOut.split('\n')) {
      const trimmed = line.trim();
      if (trimmed && fs.existsSync(trimmed)) {
        addJava(trimmed, 'Системная Java (' + path.basename(path.dirname(path.dirname(trimmed))) + ')');
      }
    }
  } catch (e) {}

  // Common Java install directories on Windows
  const roots = [
    'C:\\Program Files\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Microsoft',
    'C:\\Program Files\\BellSoft',
    'C:\\Program Files\\Zulu'
  ];

  for (const root of roots) {
    if (fs.existsSync(root)) {
      try {
        const dirs = fs.readdirSync(root);
        for (const d of dirs) {
          const javaw = path.join(root, d, 'bin', 'javaw.exe');
          if (fs.existsSync(javaw)) {
            addJava(javaw, d);
          }
        }
      } catch (e) {}
    }
  }

  return list;
}

function getConfigPath() {
  const appData = process.env.APPDATA || (process.platform === 'darwin' ? process.env.HOME + '/Library/Application Support' : '/var/local');
  const mcDir = path.join(appData, '.minecraft');
  if (!fs.existsSync(mcDir)) {
    fs.mkdirSync(mcDir, { recursive: true });
  }
  return path.join(mcDir, 'electron_rust_config.json');
}

function loadConfig() {
  try {
    const p = getConfigPath();
    if (fs.existsSync(p)) {
      return JSON.parse(fs.readFileSync(p, 'utf8'));
    }
  } catch (e) {
    console.error('Failed to load config:', e);
  }
  return {
    nickname: 'Player',
    selectedVersion: '',
    showSnapshots: false,
    ramMb: 4096,
    javaPath: '',
    gameDir: path.join(process.env.APPDATA || '', '.minecraft'),
    windowWidth: 1280,
    windowHeight: 720,
    fullscreen: false,
    customJvmArgs: '-XX:+UseG1GC',
    theme: 'emerald'
  };
}

function saveConfig(cfg) {
  try {
    fs.writeFileSync(getConfigPath(), JSON.stringify(cfg, null, 2), 'utf8');
  } catch (e) {
    console.error('Failed to save config:', e);
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1020,
    height: 680,
    minWidth: 880,
    minHeight: 600,
    frame: false,
    backgroundColor: '#0F1015',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  mainWindow.loadFile('index.html');

  mainWindow.on('closed', () => {
    mainWindow = null;
    if (activeRustProcess) {
      try { activeRustProcess.kill(); } catch (e) {}
    }
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// Window controls
ipcMain.on('window-minimize', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
  if (mainWindow) {
    if (mainWindow.isMaximized()) mainWindow.unmaximize();
    else mainWindow.maximize();
  }
});

ipcMain.on('window-close', () => {
  if (mainWindow) mainWindow.close();
});

// Config
ipcMain.handle('load-config', () => {
  return loadConfig();
});

ipcMain.handle('save-config', (event, cfg) => {
  saveConfig(cfg);
  return true;
});

// Rust Bridge: get-manifest (with automatic graceful fallback)
ipcMain.handle('get-manifest', async () => {
  return new Promise((resolve) => {
    const bin = getRustCorePath();
    if (!fs.existsSync(bin)) {
      console.warn('rust-core.exe not found at:', bin, '- using direct Node.js fallback');
      fetchManifestFallback().then(resolve).catch(err => {
        resolve({ latest: { release: '1.21.4', snapshot: '24w46a' }, versions: [] });
      });
      return;
    }

    try {
      const proc = spawn(bin, ['get-manifest']);
      let stdout = '';
      let stderr = '';

      proc.on('error', (err) => {
        console.warn('rust-core spawn error:', err.message, '- falling back to direct fetch');
        fetchManifestFallback().then(resolve).catch(() => {
          resolve({ latest: { release: '1.21.4', snapshot: '24w46a' }, versions: [] });
        });
      });

      proc.stdout.on('data', (d) => { stdout += d.toString(); });
      proc.stderr.on('data', (d) => { stderr += d.toString(); });

      proc.on('close', (code) => {
        if (code === 0 && stdout) {
          try {
            resolve(JSON.parse(stdout));
            return;
          } catch (e) {}
        }
        // If rust-core returned non-zero, fallback to node https
        fetchManifestFallback().then(resolve).catch(() => {
          resolve({ latest: { release: '1.21.4', snapshot: '24w46a' }, versions: [] });
        });
      });
    } catch (e) {
      fetchManifestFallback().then(resolve).catch(() => {
        resolve({ latest: { release: '1.21.4', snapshot: '24w46a' }, versions: [] });
      });
    }
  });
});

// Rust Bridge: detect-java (with automatic graceful fallback)
ipcMain.handle('detect-java', async () => {
  return new Promise((resolve) => {
    const bin = getRustCorePath();
    if (!fs.existsSync(bin)) {
      resolve(detectJavaFallback());
      return;
    }

    try {
      const proc = spawn(bin, ['detect-java']);
      let stdout = '';

      proc.on('error', () => {
        resolve(detectJavaFallback());
      });

      proc.stdout.on('data', (d) => { stdout += d.toString(); });
      proc.on('close', (code) => {
        if (code === 0 && stdout) {
          try {
            const parsed = JSON.parse(stdout);
            if (Array.isArray(parsed) && parsed.length > 0) {
              resolve(parsed);
              return;
            }
          } catch (e) {}
        }
        resolve(detectJavaFallback());
      });
    } catch (e) {
      resolve(detectJavaFallback());
    }
  });
});

// Rust Bridge: download
ipcMain.handle('start-download', async (event, params) => {
  return new Promise((resolve, reject) => {
    const bin = getRustCorePath();
    const args = ['download', '--version', params.versionId, '--game-dir', params.gameDir];
    if (params.url) {
      args.push('--url', params.url);
    }

    try {
      const proc = spawn(bin, args);
      let errorMsg = '';

      proc.on('error', (err) => {
        reject(new Error('Не удалось запустить rust-core: ' + err.message));
      });

      proc.stdout.on('data', (chunk) => {
        const lines = chunk.toString().split('\n');
        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed) continue;
          try {
            const parsed = JSON.parse(trimmed);
            if (parsed.type === 'progress') {
              mainWindow?.webContents.send('download-progress', parsed);
            }
          } catch (e) {}
        }
      });

      proc.stderr.on('data', (d) => { errorMsg += d.toString(); });

      proc.on('close', (code) => {
        if (code === 0) resolve(true);
        else reject(new Error(errorMsg || 'Загрузка завершилась с кодом ' + code));
      });
    } catch (e) {
      reject(e);
    }
  });
});

// Rust Bridge: launch
ipcMain.handle('launch-game', async (event, params) => {
  const bin = getRustCorePath();
  const args = [
    'launch',
    '--version', params.versionId,
    '--username', params.username || 'Player',
    '--ram', String(params.ramMb || 4096),
    '--java', params.javaPath || 'java',
    '--game-dir', params.gameDir || path.join(process.env.APPDATA || '', '.minecraft'),
    '--width', String(params.width || 1280),
    '--height', String(params.height || 720)
  ];
  if (params.fullscreen) {
    args.push('--fullscreen');
  }

  try {
    activeRustProcess = spawn(bin, args);

    activeRustProcess.on('error', (err) => {
      mainWindow?.webContents.send('game-log', '[ERROR] Ошибка запуска rust-core: ' + err.message);
      mainWindow?.webContents.send('game-exited', -1);
      activeRustProcess = null;
    });

    activeRustProcess.stdout.on('data', (chunk) => {
      const lines = chunk.toString().split('\n');
      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed) continue;
        try {
          const parsed = JSON.parse(trimmed);
          if (parsed.type === 'log') {
            mainWindow?.webContents.send('game-log', parsed.line);
          }
        } catch (e) {
          mainWindow?.webContents.send('game-log', trimmed);
        }
      }
    });

    activeRustProcess.stderr.on('data', (chunk) => {
      mainWindow?.webContents.send('game-log', '[ERR] ' + chunk.toString());
    });

    activeRustProcess.on('close', (code) => {
      mainWindow?.webContents.send('game-exited', code);
      activeRustProcess = null;
    });

    return true;
  } catch (e) {
    mainWindow?.webContents.send('game-log', '[ERROR] Не удалось вызвать rust-core: ' + e.message);
    return false;
  }
});

// Kill Game
ipcMain.handle('kill-game', () => {
  if (activeRustProcess) {
    try {
      activeRustProcess.kill();
      activeRustProcess = null;
      return true;
    } catch (e) {
      return false;
    }
  }
  return false;
});

// Dialogs
ipcMain.handle('pick-file', async (event, options) => {
  const res = await dialog.showOpenDialog(mainWindow, options);
  if (!res.canceled && res.filePaths.length > 0) {
    return res.filePaths[0];
  }
  return null;
});

ipcMain.handle('pick-folder', async (event, defaultPath) => {
  const res = await dialog.showOpenDialog(mainWindow, {
    defaultPath,
    properties: ['openDirectory']
  });
  if (!res.canceled && res.filePaths.length > 0) {
    return res.filePaths[0];
  }
  return null;
});

ipcMain.handle('open-folder-sub', async (event, sub) => {
  const cfg = loadConfig();
  const mcDir = cfg.gameDir || path.join(process.env.APPDATA || '', '.minecraft');
  const target = path.join(mcDir, sub || '');
  if (!fs.existsSync(target)) {
    fs.mkdirSync(target, { recursive: true });
  }
  require('electron').shell.openPath(target);
  return true;
});

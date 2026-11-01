const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),

  loadConfig: () => ipcRenderer.invoke('load-config'),
  saveConfig: (cfg) => ipcRenderer.invoke('save-config', cfg),

  getManifest: () => ipcRenderer.invoke('get-manifest'),
  detectJava: () => ipcRenderer.invoke('detect-java'),

  startDownload: (params) => ipcRenderer.invoke('start-download', params),
  launchGame: (params) => ipcRenderer.invoke('launch-game', params),
  killGame: () => ipcRenderer.invoke('kill-game'),

  pickFile: (options) => ipcRenderer.invoke('pick-file', options),
  pickFolder: (defaultPath) => ipcRenderer.invoke('pick-folder', defaultPath),
  openFolderSub: (sub) => ipcRenderer.invoke('open-folder-sub', sub),

  onDownloadProgress: (cb) => {
    ipcRenderer.on('download-progress', (event, data) => cb(data));
  },
  onGameLog: (cb) => {
    ipcRenderer.on('game-log', (event, line) => cb(line));
  },
  onGameExited: (cb) => {
    ipcRenderer.on('game-exited', (event, code) => cb(code));
  }
});

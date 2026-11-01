# ⛏️ Minecraft Launchers (Electron + Rust & Java 25)

В проекте оставлены только две целевые реализации лаунчера для Minecraft:

---

## ⚡ 1. Electron + Rust Launcher

Современный клиент на веб-технологиях (**Electron**) с высокоскоростным нативным бэкендом на **Rust** (`rust-core`).

### 🚀 Запуск:
👉 **[`electron-launcher/dist/MinecraftLauncher-win32-x64/MinecraftLauncher.exe`](file:///g:/work/Launcher/electron-launcher/dist/MinecraftLauncher-win32-x64/MinecraftLauncher.exe)**

### 🌟 Возможности:
- **Готовые сборки и модлоадеры**:
  - ✨ **OptiFine** (Шейдеры, зум на C, динамическое освещение, +50-80% FPS)
  - 🧵 **Fabric** (Сверхбыстрый легкий движок, поддержка Sodium и Iris)
  - 🔨 **Forge** (Классический моддинг, поддержка Create, Twilight Forest)
  - ⚔️ **Forge + OptiFine** (Гибридная сборка: моды Forge + шейдеры OptiFine)
  - ⚡ **Ванилла** (Чистые официальные релизы от Mojang)
- **5 цветовых тем оформления**:
  - 🌿 Изумруд (Emerald)
  - 💎 Алмаз (Diamond)
  - 🔮 Аметист (Amethyst)
  - ⚔️ Незерит (Netherite)
  - 🔴 Редстоун (Redstone)
- **Быстрый доступ к папкам**: кнопки прямого открытия `mods/`, `shaderpacks/` и `.minecraft`.
- **Нативный Rust Core**: многопоточная загрузка ассетов и запуск процесса Minecraft.

---

## ☕ 2. Java 25 Native Launcher (.EXE + .JAR)

Нативный лаунчер на **Java 25**, скомпилированный в автономный Windows-файл **`.exe`** через `jpackage` со встроенным оптимизированным рантаймом.

### 🚀 Запуск:
👉 **[`java-launcher/dist/MinecraftLauncher/MinecraftLauncher.exe`](file:///g:/work/Launcher/java-launcher/dist/MinecraftLauncher/MinecraftLauncher.exe)**  
*(Также доступен универсальный JAR: [`java-launcher/Launcher.jar`](file:///g:/work/Launcher/java-launcher/Launcher.jar))*

### 🌟 Возможности:
- **Полная автономность**: для запуска `.exe` не требуется предварительно устанавливать Java — runtime уже встроен.
- **Поддержка сборок OptiFine, Fabric, Forge и Forge + OptiFine**.
- **Кастомный графический интерфейс Java2D/Swing** со сглаживанием, темной темой и стилизацией.
- **Офлайн-авторизация** с генерацией UUID игрока (RFC 4122 v3).
- **Слайдер выделения памяти RAM** (1-16 ГБ) и встроенная консоль логов.

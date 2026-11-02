using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Windows.Forms;
using Microsoft.Win32;

namespace MexLauncher
{
    static class Program
    {
        private const string GITHUB_JAR_URL = "https://github.com/Mextrim/MinecraftLauncher/releases/latest/download/Launcher.jar";
        private const string GITHUB_FALLBACK_URL = "https://github.com/Mextrim/MinecraftLauncher/releases/download/v2.5.0/Launcher.jar";
        private const string JRE17_URL = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jre/hotspot/normal/eclipse";

        [STAThread]
        static void Main(string[] args)
        {
            try
            {
                // Enable modern TLS 1.2 for GitHub API and releases
                ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072 | (SecurityProtocolType)768 | SecurityProtocolType.Tls;
            }
            catch { }

            string appDir = AppDomain.CurrentDomain.BaseDirectory;
            string jarPath = Path.Combine(appDir, "MexLauncher.jar");
            if (!File.Exists(jarPath))
            {
                jarPath = Path.Combine(appDir, "Launcher.jar");
            }
            if (!File.Exists(jarPath))
            {
                jarPath = Path.Combine(appDir, "app", "MexLauncher.jar");
            }
            if (!File.Exists(jarPath))
            {
                jarPath = Path.Combine(appDir, "app", "Launcher.jar");
            }

            // 1. If JAR is missing, automatically download it from GitHub
            if (!File.Exists(jarPath))
            {
                string targetDownloadPath = Path.Combine(appDir, "MexLauncher.jar");
                bool ok = DownloadWithProgress(
                    "Загрузка Mex Launcher с GitHub",
                    "Подключение к GitHub и загрузка компонентов лаунчера...",
                    GITHUB_JAR_URL,
                    GITHUB_FALLBACK_URL,
                    targetDownloadPath
                );

                if (ok && File.Exists(targetDownloadPath))
                {
                    jarPath = targetDownloadPath;
                }
                else
                {
                    MessageBox.Show(
                        "Не удалось загрузить компоненты лаунчера с GitHub.\n" +
                        "Проверьте подключение к Интернету или скачайте Launcher.jar вручную со страницы:\n" +
                        "https://github.com/Mextrim/MinecraftLauncher/releases",
                        "Mex Launcher — Ошибка загрузки",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }
            }

            // 2. Find Java or offer to auto-download JRE 17
            string javaExe = FindJava(appDir);
            if (string.IsNullOrEmpty(javaExe))
            {
                DialogResult res = MessageBox.Show(
                    "На вашем компьютере не найдена Java (требуется Java 17 для сборки с модами YOLO).\n\n" +
                    "Хотите, чтобы Mex Launcher автоматически скачал и настроил чистую Java 17 (Adoptium OpenJDK)?",
                    "Mex Launcher — Требуется Java",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Question
                );

                if (res == DialogResult.Yes)
                {
                    string jreZip = Path.Combine(appDir, "jre17_temp.zip");
                    string runtimeDir = Path.Combine(appDir, "runtime");
                    bool ok = DownloadWithProgress(
                        "Загрузка Java 17",
                        "Загрузка нативной среды Adoptium Java 17 для Minecraft...",
                        JRE17_URL,
                        null,
                        jreZip
                    );

                    if (ok && File.Exists(jreZip))
                    {
                        try
                        {
                            ExtractZipSimple(jreZip, runtimeDir);
                            File.Delete(jreZip);
                            javaExe = FindJava(appDir);
                        }
                        catch (Exception ex)
                        {
                            MessageBox.Show("Ошибка распаковки Java:\n" + ex.Message, "Ошибка", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        }
                    }
                }

                if (string.IsNullOrEmpty(javaExe))
                {
                    try { Process.Start("https://adoptium.net/temurin/releases/"); } catch { }
                    return;
                }
            }

            // 3. Launch silently without console window
            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = javaExe;

            string arguments = "-Dminecraft.launcher.brand=MexLauncher -jar \"" + jarPath + "\"";
            if (args != null && args.Length > 0)
            {
                arguments += " " + string.Join(" ", args);
            }

            psi.Arguments = arguments;
            psi.WorkingDirectory = appDir;
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;

            try
            {
                Process proc = Process.Start(psi);
                if (proc == null)
                {
                    MessageBox.Show("Не удалось запустить процесс Java.", "Ошибка запуска", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Ошибка запуска:\n" + ex.Message, "Ошибка запуска Mex Launcher", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        // GUI download window with smooth dark styling
        static bool DownloadWithProgress(string title, string info, string primaryUrl, string fallbackUrl, string destinationPath)
        {
            bool success = false;

            using (Form form = new Form())
            {
                form.Text = title;
                form.Size = new Size(460, 160);
                form.StartPosition = FormStartPosition.CenterScreen;
                form.FormBorderStyle = FormBorderStyle.FixedDialog;
                form.MaximizeBox = false;
                form.MinimizeBox = false;
                form.BackColor = Color.FromArgb(19, 23, 36);
                form.ForeColor = Color.White;

                Label lblInfo = new Label();
                lblInfo.Text = info;
                lblInfo.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
                lblInfo.ForeColor = Color.White;
                lblInfo.Location = new Point(20, 16);
                lblInfo.Size = new Size(400, 24);
                form.Controls.Add(lblInfo);

                ProgressBar bar = new ProgressBar();
                bar.Location = new Point(20, 48);
                bar.Size = new Size(400, 24);
                bar.Minimum = 0;
                bar.Maximum = 100;
                form.Controls.Add(bar);

                Label lblStatus = new Label();
                lblStatus.Text = "Подключение к серверу...";
                lblStatus.Font = new Font("Segoe UI", 8.5f, FontStyle.Regular);
                lblStatus.ForeColor = Color.FromArgb(148, 163, 184);
                lblStatus.Location = new Point(20, 80);
                lblStatus.Size = new Size(400, 20);
                form.Controls.Add(lblStatus);

                BackgroundWorker worker = new BackgroundWorker();
                worker.WorkerReportsProgress = true;

                worker.DoWork += (s, ev) =>
                {
                    string[] urls = string.IsNullOrEmpty(fallbackUrl) ? new string[] { primaryUrl } : new string[] { primaryUrl, fallbackUrl };
                    Exception lastEx = null;

                    foreach (var url in urls)
                    {
                        try
                        {
                            HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
                            req.UserAgent = "MexLauncher-Bootstrap";
                            req.Timeout = 30000;
                            req.AllowAutoRedirect = true;

                            using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
                            {
                                long total = resp.ContentLength;
                                using (Stream inStream = resp.GetResponseStream())
                                using (FileStream outStream = new FileStream(destinationPath, FileMode.Create, FileAccess.Write, FileShare.None))
                                {
                                    byte[] buffer = new byte[32768];
                                    long received = 0;
                                    int read;

                                    while ((read = inStream.Read(buffer, 0, buffer.Length)) > 0)
                                    {
                                        outStream.Write(buffer, 0, read);
                                        received += read;
                                        if (total > 0)
                                        {
                                            int pct = (int)((received * 100) / total);
                                            worker.ReportProgress(pct, string.Format("Загружено {0:F1} МБ из {1:F1} МБ ({2}%)", received / 1048576.0, total / 1048576.0, pct));
                                        }
                                        else
                                        {
                                            worker.ReportProgress(50, string.Format("Загружено {0:F1} МБ...", received / 1048576.0));
                                        }
                                    }
                                }
                            }
                            ev.Result = true;
                            return;
                        }
                        catch (Exception ex)
                        {
                            lastEx = ex;
                        }
                    }

                    if (lastEx != null) throw lastEx;
                };

                worker.ProgressChanged += (s, ev) =>
                {
                    bar.Value = Math.Min(100, Math.Max(0, ev.ProgressPercentage));
                    if (ev.UserState != null) lblStatus.Text = ev.UserState.ToString();
                };

                worker.RunWorkerCompleted += (s, ev) =>
                {
                    if (ev.Error == null && ev.Result is bool && (bool)ev.Result)
                    {
                        success = true;
                    }
                    form.Close();
                };

                form.Shown += (s, ev) => worker.RunWorkerAsync();
                form.ShowDialog();
            }

            return success;
        }

        static void ExtractZipSimple(string zipPath, string targetDir)
        {
            if (!Directory.Exists(targetDir)) Directory.CreateDirectory(targetDir);
            // Use shell script / powershell / tar for fast native unpacking in Windows
            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = "tar.exe";
            psi.Arguments = "-xf \"" + zipPath + "\" -C \"" + targetDir + "\"";
            psi.CreateNoWindow = true;
            psi.UseShellExecute = false;

            try
            {
                using (Process p = Process.Start(psi))
                {
                    p.WaitForExit();
                }
            }
            catch
            {
                // Fallback to powershell Expand-Archive
                ProcessStartInfo psiPs = new ProcessStartInfo();
                psiPs.FileName = "powershell.exe";
                psiPs.Arguments = "-NoProfile -Command \"Expand-Archive -Path '" + zipPath + "' -DestinationPath '" + targetDir + "' -Force\"";
                psiPs.CreateNoWindow = true;
                psiPs.UseShellExecute = false;
                using (Process p2 = Process.Start(psiPs))
                {
                    p2.WaitForExit();
                }
            }
        }

        static string FindJava(string baseDir)
        {
            // 1. Check local runtime/bin/javaw.exe
            string[] localPaths = new string[] {
                Path.Combine(baseDir, "runtime", "jre-17", "bin", "javaw.exe"),
                Path.Combine(baseDir, "runtime", "bin", "javaw.exe"),
                Path.Combine(baseDir, "jre", "bin", "javaw.exe"),
                Path.Combine(baseDir, "dist", "MinecraftLauncher", "runtime", "bin", "javaw.exe"),
                Path.Combine(baseDir, "runtime", "jre-17", "bin", "java.exe"),
                Path.Combine(baseDir, "runtime", "bin", "java.exe")
            };
            foreach (var lp in localPaths)
            {
                if (File.Exists(lp)) return lp;
            }

            // 2. Check JAVA_HOME
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                javaHome = javaHome.TrimEnd('\\', '/');
                string p = Path.Combine(javaHome, "bin", "javaw.exe");
                if (File.Exists(p)) return p;
                string pj = Path.Combine(javaHome, "bin", "java.exe");
                if (File.Exists(pj)) return pj;
            }

            // 3. Check Windows Registry 64-bit & 32-bit for JDK/JRE
            string[] registryRoots = new string[] {
                @"SOFTWARE\JavaSoft\JDK",
                @"SOFTWARE\JavaSoft\Java Development Kit",
                @"SOFTWARE\JavaSoft\Java Runtime Environment",
                @"SOFTWARE\Eclipse Adoptium\JDK",
                @"SOFTWARE\Eclipse Foundation\JDK",
                @"SOFTWARE\Microsoft\JDK",
                @"SOFTWARE\BellSoft\Liberica",
                @"SOFTWARE\Azul Systems\Zulu"
            };

            foreach (var view in new RegistryView[] { RegistryView.Registry64, RegistryView.Registry32 })
            {
                foreach (var regRoot in registryRoots)
                {
                    try
                    {
                        using (var hklm = RegistryKey.OpenBaseKey(RegistryHive.LocalMachine, view))
                        using (var key = hklm.OpenSubKey(regRoot))
                        {
                            if (key != null)
                            {
                                foreach (var sub in key.GetSubKeyNames())
                                {
                                    using (var subKey = key.OpenSubKey(sub))
                                    {
                                        if (subKey != null)
                                        {
                                            var dir = subKey.GetValue("JavaHome") as string;
                                            if (!string.IsNullOrEmpty(dir))
                                            {
                                                dir = dir.TrimEnd('\\', '/');
                                                string jw = Path.Combine(dir, "bin", "javaw.exe");
                                                if (File.Exists(jw)) return jw;
                                                string jwa = Path.Combine(dir, "bin", "java.exe");
                                                if (File.Exists(jwa)) return jwa;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch { }
                }
            }

            // 4. Check Program Files standard Java directories
            string[] pfRoots = new string[] {
                Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles),
                Environment.GetEnvironmentVariable("ProgramW6432"),
                Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86)
            };

            foreach (var pf in pfRoots)
            {
                if (string.IsNullOrEmpty(pf) || !Directory.Exists(pf)) continue;
                string[] subDirs = new string[] { "Java", "Eclipse Adoptium", "Microsoft", "BellSoft", "Zulu" };
                foreach (var sd in subDirs)
                {
                    string target = Path.Combine(pf, sd);
                    if (Directory.Exists(target))
                    {
                        try
                        {
                            foreach (var jdkDir in Directory.GetDirectories(target))
                            {
                                string jw = Path.Combine(jdkDir, "bin", "javaw.exe");
                                if (File.Exists(jw)) return jw;
                                string jwa = Path.Combine(jdkDir, "bin", "java.exe");
                                if (File.Exists(jwa)) return jwa;
                            }
                        }
                        catch { }
                    }
                }
            }

            // 5. Search PATH
            string pathEnv = Environment.GetEnvironmentVariable("PATH") ?? "";
            foreach (var dir in pathEnv.Split(Path.PathSeparator))
            {
                try
                {
                    if (!string.IsNullOrWhiteSpace(dir))
                    {
                        string jw = Path.Combine(dir.Trim(), "javaw.exe");
                        if (File.Exists(jw)) return jw;
                        string jwa = Path.Combine(dir.Trim(), "java.exe");
                        if (File.Exists(jwa)) return jwa;
                    }
                }
                catch { }
            }

            return null;
        }
    }
}

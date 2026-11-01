using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;
using Microsoft.Win32;

namespace MinecraftLauncher
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            string appDir = AppDomain.CurrentDomain.BaseDirectory;
            string jarPath = Path.Combine(appDir, "Launcher.jar");
            if (!File.Exists(jarPath))
            {
                jarPath = Path.Combine(appDir, "app", "Launcher.jar");
            }
            if (!File.Exists(jarPath))
            {
                MessageBox.Show(
                    "Не найден исполняемый файл Launcher.jar!\n\nУбедитесь, что Launcher.jar находится в папке с приложением.", 
                    "Minecraft Launcher", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            string javaExe = FindJava(appDir);
            if (string.IsNullOrEmpty(javaExe))
            {
                DialogResult res = MessageBox.Show(
                    "На вашем компьютере не найдена Java (требуется Java 17 или новее).\n\n" +
                    "Хотите открыть страницу загрузки официальной версии Java (Adoptium Temurin OpenJDK)?",
                    "Minecraft Launcher — Требуется Java",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Question
                );
                if (res == DialogResult.Yes)
                {
                    try { Process.Start("https://adoptium.net/temurin/releases/"); } catch { }
                }
                return;
            }

            // Launch javaw.exe silently without console window
            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = javaExe;
            
            string arguments = "-jar \"" + jarPath + "\"";
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
                    MessageBox.Show("Не удалось запустить процесс Java.", "Ошибка", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Ошибка запуска Java:\n" + ex.Message, "Ошибка запуска", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        static string FindJava(string baseDir)
        {
            // 1. Check local runtime/bin/javaw.exe or dist/MinecraftLauncher/runtime/bin/javaw.exe
            string[] localPaths = new string[] {
                Path.Combine(baseDir, "runtime", "bin", "javaw.exe"),
                Path.Combine(baseDir, "dist", "MinecraftLauncher", "runtime", "bin", "javaw.exe"),
                Path.Combine(baseDir, "jre", "bin", "javaw.exe"),
                Path.Combine(baseDir, "runtime", "bin", "java.exe"),
                Path.Combine(baseDir, "dist", "MinecraftLauncher", "runtime", "bin", "java.exe")
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

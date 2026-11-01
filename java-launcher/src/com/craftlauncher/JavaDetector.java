package com.craftlauncher;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class JavaDetector {

    public static class JavaItem {
        public final String name;
        public final String path;
        public final int version;

        public JavaItem(String name, String path, int version) {
            this.name = name;
            this.path = path;
            this.version = version;
        }

        @Override
        public String toString() {
            return name + " (" + path + ")";
        }
    }

    public static List<JavaItem> detect() {
        List<JavaItem> list = new ArrayList<>();
        Set<String> checked = new HashSet<>();

        java.util.function.BiConsumer<String, String> tryAdd = (pathStr, label) -> {
            if (pathStr == null || pathStr.isBlank()) return;
            File f = new File(pathStr);
            if (!f.exists()) return;
            String norm = f.getAbsolutePath().toLowerCase();
            if (checked.add(norm)) {
                int ver = parseVersion(f.getAbsolutePath());
                String title = label.isBlank() ? "Java " + ver + " (" + f.getParentFile().getParentFile().getName() + ")" : label + " (Java " + ver + ")";
                list.add(new JavaItem(title, f.getAbsolutePath(), ver));
            }
        };

        // 1. JAVA_HOME
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            tryAdd.accept(Paths.get(javaHome, "bin", "javaw.exe").toString(), "JAVA_HOME");
            tryAdd.accept(Paths.get(javaHome, "bin", "java.exe").toString(), "JAVA_HOME");
        }

        // 2. PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String seg : pathEnv.split(";")) {
                if (!seg.isBlank() && Files.isDirectory(Paths.get(seg))) {
                    tryAdd.accept(Paths.get(seg, "javaw.exe").toString(), "PATH");
                    tryAdd.accept(Paths.get(seg, "java.exe").toString(), "PATH");
                }
            }
        }

        // 3. Standard roots
        String[] roots = {
            "C:\\Program Files\\Java",
            "C:\\Program Files\\Microsoft",
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files\\BellSoft",
            "C:\\Program Files\\Amazon Corretto",
            "C:\\Program Files\\Zulu",
            "C:\\Program Files (x86)\\Java"
        };

        for (String root : roots) {
            File r = new File(root);
            if (r.isDirectory()) {
                File[] subs = r.listFiles();
                if (subs != null) {
                    for (File sub : subs) {
                        if (sub.isDirectory()) {
                            tryAdd.accept(new File(sub, "bin\\javaw.exe").getAbsolutePath(), "");
                            tryAdd.accept(new File(sub, "bin\\java.exe").getAbsolutePath(), "");
                        }
                    }
                }
            }
        }

        list.sort((a, b) -> Integer.compare(b.version, a.version));
        return list;
    }

    private static int parseVersion(String javaPath) {
        try {
            Process p = new ProcessBuilder(javaPath, "-version")
                .redirectErrorStream(true)
                .start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("version")) {
                        int quote1 = line.indexOf('"');
                        if (quote1 != -1) {
                            int quote2 = line.indexOf('"', quote1 + 1);
                            if (quote2 != -1) {
                                String ver = line.substring(quote1 + 1, quote2);
                                String[] parts = ver.split("\\.");
                                if (parts.length > 0) {
                                    if ("1".equals(parts[0]) && parts.length > 1) {
                                        return Integer.parseInt(parts[1]);
                                    }
                                    return Integer.parseInt(parts[0]);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }
}

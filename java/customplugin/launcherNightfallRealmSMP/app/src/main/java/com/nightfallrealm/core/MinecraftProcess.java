package com.nightfallrealm.core;

import com.google.gson.*;
import com.nightfallrealm.Constants;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MinecraftProcess {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private volatile boolean cancelled = false;

    private final File libDir = new File(Constants.GAME_DIR, "libraries");
    private final File versionsDir = new File(Constants.GAME_DIR, "versions");
    private final File assetsDir = new File(Constants.GAME_DIR, "assets");
    private final File nativesDir = new File(Constants.GAME_DIR, "natives");
    private final File modsGameDir = new File(Constants.GAME_DIR, "mods");
    private final File rpGameDir = new File(Constants.GAME_DIR, "resourcepacks");

    private String fabricLoaderVersion;
    private boolean fabricInstalled;
    private JsonObject fabricProfile;

    public boolean isFabricInstalled() { return fabricInstalled; }
    public String getFabricLoaderVersion() { return fabricLoaderVersion; }

    public CompletableFuture<Void> prepareGame(Consumer<String> statusCallback,
                                                Consumer<Double> progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                prepareDirectories();
                statusCallback.accept("Загрузка манифеста версий...");
                JsonObject versionManifest = fetchJson(Constants.VERSION_MANIFEST_URL).getAsJsonObject();
                JsonObject versionInfo = resolveVersion(versionManifest);
                statusCallback.accept("Загрузка библиотек...");
                downloadLibraries(versionInfo, progressCallback);
                statusCallback.accept("Загрузка клиента...");
                downloadClient(versionInfo, progressCallback);
                statusCallback.accept("Загрузка ассетов...");
                downloadAssets(versionInfo, progressCallback);
                statusCallback.accept("Загрузка Fabric...");
                downloadFabric(statusCallback, progressCallback);
                statusCallback.accept("Настройка Minecraft...");
                createDefaultOptions();
                statusCallback.accept("Готово к запуску!");
            } catch (Exception e) {
                throw new RuntimeException("Ошибка подготовки игры: " + e.getMessage(), e);
            }
        }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
    }

    public Process launch(String username, Consumer<String> statusCallback) throws Exception {
        if (cancelled) throw new IllegalStateException("Запуск отменён");

        String javaPath = findJava21();
        if (javaPath == null) {
            statusCallback.accept("Java 21 не найдена. Скачивание...");
            javaPath = downloadJava21(s -> {});
        }

        versionsDir.mkdirs();

        JsonObject versionManifest = fetchJson(Constants.VERSION_MANIFEST_URL).getAsJsonObject();
        JsonObject versionInfo = resolveVersion(versionManifest);

        String versionName = versionInfo.get("id").getAsString();
        String mainClass = versionInfo.get("mainClass").getAsString();

        List<String> classpathParts = new ArrayList<>();
        JsonArray libraries = versionInfo.getAsJsonArray("libraries");

        if (libraries != null) {
            Set<String> extractedNatives = new HashSet<>();
            for (int i = 0; i < libraries.size(); i++) {
                JsonObject lib = libraries.get(i).getAsJsonObject();
                JsonObject downloads = lib.getAsJsonObject("downloads");
                if (downloads == null) continue;

                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (artifact == null) continue;

                String path = artifact.get("path").getAsString();
                File libFile = new File(libDir, path);
                if (libFile.exists()) {
                    classpathParts.add(libFile.getAbsolutePath());
                }

                if (downloads.has("classifiers")) {
                    JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                    String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                    String classifier = null;
                    if (osName.contains("win")) classifier = "natives-windows";
                    else if (osName.contains("mac")) classifier = "natives-macos";
                    else classifier = "natives-linux";

                    if (classifiers.has(classifier)) {
                        JsonObject nativeArtifact = classifiers.getAsJsonObject(classifier);
                        String nativePath = nativeArtifact.get("path").getAsString();
                        File nativeFile = new File(libDir, nativePath);
                        if (nativeFile.exists() && !extractedNatives.contains(nativeFile.getName())) {
                            extractedNatives.add(nativeFile.getName());
                            extractNatives(nativeFile);
                        }
                    }
                }
            }
        }

        if (fabricInstalled && fabricProfile != null) {
            mainClass = Constants.FABRIC_MAIN_CLASS;
            JsonArray fabricLibs = fabricProfile.getAsJsonArray("libraries");
            if (fabricLibs != null) {
                for (int i = 0; i < fabricLibs.size(); i++) {
                    JsonObject lib = fabricLibs.get(i).getAsJsonObject();
                    String name = lib.get("name").getAsString();
                    String url = lib.has("url") ? lib.get("url").getAsString() : Constants.FABRIC_MAVEN_URL;
                    File jar = resolveFabricLib(name, url);
                    if (jar != null && jar.exists()) {
                        classpathParts.add(jar.getAbsolutePath());
                    }
                }
            }
        }

        String clientPath = getClientPath(versionName);
        if (new File(clientPath).exists()) {
            classpathParts.add(clientPath);
        }

        String classpath = classpathParts.stream()
                .collect(Collectors.joining(File.pathSeparator));

        List<String> args = new ArrayList<>();
        args.add(javaPath);
        args.add("-Xms" + Constants.MIN_RAM_MB + "m");
        args.add("-Xmx" + Constants.RECOMMENDED_RAM_MB + "m");
        args.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        args.add("-cp");
        args.add(classpath);
        args.add(mainClass);
        args.add("--username");
        args.add(username);
        args.add("--version");
        args.add(fabricInstalled ? "fabric-loader-" + fabricLoaderVersion + "-" + versionName : versionName);
        args.add("--gameDir");
        args.add(Constants.GAME_DIR.getAbsolutePath());
        args.add("--assetsDir");
        args.add(assetsDir.getAbsolutePath());
        args.add("--assetIndex");
        args.add(getAssetIndex(versionInfo));
        args.add("--uuid");
        args.add(UUID.randomUUID().toString().replace("-", ""));
        args.add("--accessToken");
        args.add("nightfall-offline-token");
        args.add("--userType");
        args.add("mojang");
        args.add("--versionType");
        args.add("release");
        args.add("--server");
        args.add(Constants.SERVER_IP);
        args.add("--port");
        args.add(String.valueOf(Constants.SERVER_PORT));
        args.add("--quickPlayMultiplayer");
        args.add(Constants.SERVER_IP + ":" + Constants.SERVER_PORT);

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(Constants.GAME_DIR);
        pb.environment().put("APPDATA", System.getenv("APPDATA"));

        File logFile = new File(Constants.GAME_DIR, "launcher_log.txt");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

        statusCallback.accept("Запуск Minecraft" + (fabricInstalled ? " с Fabric" : "") + "...");
        return pb.start();
    }

    private void downloadFabric(Consumer<String> status, Consumer<Double> progress) {
        try {
            String metaUrl = String.format(Constants.FABRIC_META_URL, Constants.MINECRAFT_VERSION);
            JsonArray loaders = fetchJsonArray(metaUrl);
            fabricLoaderVersion = null;
            for (int i = 0; i < loaders.size(); i++) {
                JsonObject entry = loaders.get(i).getAsJsonObject();
                if (entry.getAsJsonObject("loader").get("stable").getAsBoolean()) {
                    fabricLoaderVersion = entry.getAsJsonObject("loader").get("version").getAsString();
                    break;
                }
            }
            if (fabricLoaderVersion == null) {
                throw new RuntimeException("No stable Fabric loader found for " + Constants.MINECRAFT_VERSION);
            }

            String profileUrl = String.format(Constants.FABRIC_PROFILE_URL,
                    Constants.MINECRAFT_VERSION, fabricLoaderVersion);
            fabricProfile = fetchJson(profileUrl);

            JsonArray fabricLibs = fabricProfile.getAsJsonArray("libraries");
            if (fabricLibs != null) {
                int total = fabricLibs.size();
                for (int i = 0; i < fabricLibs.size(); i++) {
                    if (cancelled) return;
                    JsonObject lib = fabricLibs.get(i).getAsJsonObject();
                    String name = lib.get("name").getAsString();
                    String url = lib.has("url") ? lib.get("url").getAsString() : Constants.FABRIC_MAVEN_URL;
                    downloadFabricLib(name, url);
                    progress.accept((double) (i + 1) / total);
                }
            }

            fabricInstalled = true;
            status.accept("Fabric " + fabricLoaderVersion + " загружен");
        } catch (Exception e) {
            fabricInstalled = false;
            fabricProfile = null;
            status.accept("Fabric не загружен: " + e.getMessage());
        }
    }

    private void downloadFabricLib(String mavenCoords, String baseUrl) {
        String[] parts = mavenCoords.split(":");
        if (parts.length < 3) return;
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String jarName = artifact + "-" + version + ".jar";
        String path = group + "/" + artifact + "/" + version + "/" + jarName;
        String url = baseUrl + path;

        File target = new File(libDir, path);
        if (target.exists()) return;

        target.getParentFile().mkdirs();
        downloadUrlToFile(url, target);
    }

    private File resolveFabricLib(String mavenCoords, String baseUrl) {
        String[] parts = mavenCoords.split(":");
        if (parts.length < 3) return null;
        String artifact = parts[1];
        String version = parts[2];
        String group = parts[0].replace('.', '/');
        String jarName = artifact + "-" + version + ".jar";
        String path = group + "/" + artifact + "/" + version + "/" + jarName;
        File f = new File(libDir, path);
        return f.exists() ? f : null;
    }

    public void cancel() {
        cancelled = true;
    }

    private void createDefaultOptions() {
        File optionsFile = new File(Constants.GAME_DIR, "options.txt");
        if (optionsFile.exists()) return;
        try {
            String content = "lang:ru_ru\nmusic:0.0\nnote_block:0.0\nguiScale:2\n";
            Files.writeString(optionsFile.toPath(), content);
        } catch (Exception ignored) {}
    }

    private void prepareDirectories() {
        libDir.mkdirs();
        versionsDir.mkdirs();
        assetsDir.mkdirs();
        nativesDir.mkdirs();
        modsGameDir.mkdirs();
        rpGameDir.mkdirs();
        Constants.MODS_DIR.mkdirs();
        Constants.RESOURCE_PACKS_DIR.mkdirs();
        Constants.LAUNCHER_DIR.mkdirs();
    }

    private JsonObject resolveVersion(JsonObject manifest) {
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (int i = 0; i < versions.size(); i++) {
            JsonObject v = versions.get(i).getAsJsonObject();
            if (Constants.MINECRAFT_VERSION.equals(v.get("id").getAsString())) {
                String url = v.get("url").getAsString();
                return fetchJson(url).getAsJsonObject();
            }
        }
        throw new RuntimeException("Версия " + Constants.MINECRAFT_VERSION + " не найдена в манифесте");
    }

    private String getClientPath(String versionName) {
        return new File(versionsDir, versionName + "/" + versionName + ".jar").getAbsolutePath();
    }

    private String getAssetIndex(JsonObject versionInfo) {
        JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
        return assetIndex.get("id").getAsString();
    }

    private void downloadLibraries(JsonObject versionInfo, Consumer<Double> progress) {
        JsonArray libraries = versionInfo.getAsJsonArray("libraries");
        if (libraries == null) return;

        int total = libraries.size();
        int completed = 0;

        for (int i = 0; i < libraries.size(); i++) {
            if (cancelled) return;
            JsonObject lib = libraries.get(i).getAsJsonObject();

            if (isLibAllowed(lib)) {
                JsonObject downloads = lib.getAsJsonObject("downloads");
                if (downloads != null) {
                    if (downloads.has("artifact")) {
                        JsonObject artifact = downloads.getAsJsonObject("artifact");
                        downloadFile(artifact, libDir);
                    }
                    if (downloads.has("classifiers")) {
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        for (String key : classifiers.keySet()) {
                            JsonObject classifierArtifact = classifiers.getAsJsonObject(key);
                            downloadFile(classifierArtifact, libDir);
                        }
                    }
                }
            }
            completed++;
            progress.accept((double) completed / total);
        }
    }

    private boolean isLibAllowed(JsonObject lib) {
        if (!lib.has("rules")) return true;
        boolean allowed = false;
        JsonArray rules = lib.getAsJsonArray("rules");
        for (int i = 0; i < rules.size(); i++) {
            JsonObject rule = rules.get(i).getAsJsonObject();
            String action = rule.get("action").getAsString();
            boolean matches = true;
            if (rule.has("os")) {
                JsonObject os = rule.getAsJsonObject("os");
                String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                if (os.has("name")) {
                    String requiredOs = os.get("name").getAsString();
                    if (requiredOs.equals("windows") && !osName.contains("win")) matches = false;
                    else if (requiredOs.equals("osx") && !osName.contains("mac")) matches = false;
                    else if (requiredOs.equals("linux") && !osName.contains("nux")) matches = false;
                }
            }
            if (matches) {
                allowed = action.equals("allow");
            }
        }
        return allowed;
    }

    private void downloadClient(JsonObject versionInfo, Consumer<Double> progress) {
        JsonObject downloads = versionInfo.getAsJsonObject("downloads");
        if (downloads == null) return;
        JsonObject client = downloads.getAsJsonObject("client");
        if (client == null) return;

        String versionName = versionInfo.get("id").getAsString();
        File versionDir = new File(versionsDir, versionName);
        versionDir.mkdirs();

        JsonObject artifact = new JsonObject();
        artifact.addProperty("path", versionName + "/" + versionName + ".jar");
        artifact.addProperty("url", client.get("url").getAsString());
        if (client.has("sha1")) artifact.addProperty("sha1", client.get("sha1").getAsString());
        if (client.has("size")) artifact.addProperty("size", client.get("size").getAsLong());

        downloadFile(artifact, versionsDir);
        progress.accept(1.0);
    }

    private void downloadAssets(JsonObject versionInfo, Consumer<Double> progress) {
        JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
        if (assetIndex == null) return;

        String indexUrl = assetIndex.get("url").getAsString();
        File indexesDir = new File(assetsDir, "indexes");
        indexesDir.mkdirs();

        String indexFileName = assetIndex.get("id").getAsString() + ".json";
        File indexFile = new File(indexesDir, indexFileName);

        if (!indexFile.exists()) {
            downloadUrlToFile(indexUrl, indexFile);
        }

        try {
            String content = new String(Files.readAllBytes(indexFile.toPath()));
            JsonObject index = GSON.fromJson(content, JsonObject.class);
            JsonObject objects = index.getAsJsonObject("objects");
            if (objects == null) return;

            File objectsDir = new File(assetsDir, "objects");
            objectsDir.mkdirs();

            List<String> keys = new ArrayList<>(objects.keySet());
            int total = keys.size();

            for (int i = 0; i < keys.size(); i++) {
                if (cancelled) return;
                String key = keys.get(i);
                JsonObject obj = objects.getAsJsonObject(key);
                String hash = obj.get("hash").getAsString();
                String prefix = hash.substring(0, 2);
                File objFile = new File(objectsDir, prefix + "/" + hash);

                if (!objFile.exists()) {
                    String assetUrl = Constants.ASSETS_BASE_URL + "/" + prefix + "/" + hash;
                    objFile.getParentFile().mkdirs();
                    try {
                        downloadUrlToFile(assetUrl, objFile);
                    } catch (Exception e) {
                        // skip failed assets
                    }
                }
                progress.accept((double) i / total);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки ассетов: " + e.getMessage(), e);
        }
    }

    private void downloadFile(JsonObject artifact, File baseDir) {
        if (artifact == null) return;
        String path = artifact.get("path").getAsString();
        String url = artifact.get("url").getAsString();
        File target = new File(baseDir, path);

        String sha1 = artifact.has("sha1") ? artifact.get("sha1").getAsString() : null;
        long size = artifact.has("size") ? artifact.get("size").getAsLong() : -1;

        if (target.exists()) {
            if (sha1 != null) {
                try {
                    String fileHash = sha1Hash(target);
                    if (fileHash.equals(sha1)) return;
                } catch (Exception e) {
                    // re-download
                }
            } else if (size > 0 && target.length() == size) {
                return;
            } else {
                return; // no validation, assume OK
            }
        }

        target.getParentFile().mkdirs();
        downloadUrlToFile(url, target);
    }

    private void downloadUrlToFile(String urlStr, File target) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("User-Agent", Constants.DOWNLOAD_USER_AGENT);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                int code = conn.getResponseCode();
                if (code == 200) {
                    try (ReadableByteChannel in = Channels.newChannel(conn.getInputStream());
                         FileChannel out = FileChannel.open(target.toPath(),
                                 StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                                 StandardOpenOption.TRUNCATE_EXISTING)) {
                        out.transferFrom(in, 0, Long.MAX_VALUE);
                    }
                    return;
                }
            } catch (Exception e) {
                if (attempt == 2) throw new RuntimeException(
                        "Ошибка скачивания: " + urlStr + " - " + e.getMessage(), e);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private JsonObject fetchJson(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", Constants.DOWNLOAD_USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки JSON: " + urlStr + " - " + e.getMessage(), e);
        }
    }

    private JsonArray fetchJsonArray(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", Constants.DOWNLOAD_USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                return GSON.fromJson(reader, JsonArray.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки JSON: " + urlStr + " - " + e.getMessage(), e);
        }
    }

    private void extractNatives(File nativeJar) {
        try {
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(nativeJar)) {
                Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (name.contains("META-INF")) continue;
                    File outFile = new File(nativesDir, name);
                    outFile.getParentFile().mkdirs();
                    try (InputStream is = jar.getInputStream(entry);
                         FileOutputStream os = new FileOutputStream(outFile)) {
                        is.transferTo(os);
                    }
                }
            }
        } catch (Exception e) {
            // ignore extraction errors
        }
    }

    public static String findJava21() {
        File bundledJava = findJavaInDir(Constants.JAVA_DIR);
        if (bundledJava != null) return bundledJava.getAbsolutePath();

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java.exe";

        File javaExe = new File(javaBin);
        if (javaExe.exists()) {
            String version = getJavaVersion(javaExe.getAbsolutePath());
            if (version != null && isVersion21OrHigher(version)) {
                return javaExe.getAbsolutePath();
            }
        }

        String[] commonPaths = {
                "C:/Program Files/Java/jdk-21/bin/java.exe",
                "C:/Program Files/Java/jdk-22/bin/java.exe",
                "C:/Program Files/Java/jdk-23/bin/java.exe",
                "C:/Program Files/Eclipse Adoptium/jdk-21/bin/java.exe",
                "C:/Program Files/Eclipse Adoptium/jdk-22/bin/java.exe",
                "C:/Program Files/Java/jre-21/bin/java.exe",
                "C:/Program Files/Java/jre-22/bin/java.exe",
                System.getenv("JAVA_HOME") + "/bin/java.exe",
                System.getenv("JAVA_HOME_21") + "/bin/java.exe",
        };

        for (String path : commonPaths) {
            File f = new File(path);
            if (f.exists()) {
                String version = getJavaVersion(f.getAbsolutePath());
                if (version != null && isVersion21OrHigher(version)) {
                    return f.getAbsolutePath();
                }
            }
        }

        try {
            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String dir : pathEnv.split(File.pathSeparator)) {
                    File candidate = new File(dir, "java.exe");
                    if (candidate.exists()) {
                        String version = getJavaVersion(candidate.getAbsolutePath());
                        if (version != null && isVersion21OrHigher(version)) {
                            return candidate.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }

    private static String getJavaVersion(String javaPath) {
        try {
            Process p = new ProcessBuilder(javaPath, "-version")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains("\"")) {
                    int start = line.indexOf("\"") + 1;
                    int end = line.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        return line.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static boolean isVersion21OrHigher(String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length > 0) {
                int major = Integer.parseInt(parts[0]);
                return major >= 21;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private String sha1Hash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static File findJavaInDir(File dir) {
        if (!dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File binJava = new File(f, "bin/java.exe");
                if (binJava.exists()) {
                    String version = getJavaVersion(binJava.getAbsolutePath());
                    if (version != null && isVersion21OrHigher(version)) {
                        return binJava;
                    }
                }
                File nested = findJavaInDir(f);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    public static String downloadJava21(Consumer<String> status) throws Exception {
        File javaDir = Constants.JAVA_DIR;
        javaDir.mkdirs();

        File marker = new File(javaDir, ".java_ready");
        if (marker.exists()) {
            MinecraftProcess temp = new MinecraftProcess();
            File found = temp.findJavaInDir(javaDir);
            if (found != null) return found.getAbsolutePath();
        }

        status.accept("Скачивание Java 21...");
        File zipFile = new File(javaDir, "jdk21.zip");
        String urlStr = Constants.JAVA_DOWNLOAD_URL;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("User-Agent", Constants.DOWNLOAD_USER_AGENT);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);
                int code = conn.getResponseCode();
                if (code == 200) {
                    long total = conn.getContentLengthLong();
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(zipFile)) {
                        byte[] buf = new byte[8192];
                        int read;
                        long downloaded = 0;
                        while ((read = in.read(buf)) != -1) {
                            out.write(buf, 0, read);
                            downloaded += read;
                            if (total > 0) {
                                double pct = Math.min(0.95, (double) downloaded / total);
                                status.accept(String.format("Скачивание Java 21... %d%%", (int)(pct * 100)));
                            }
                        }
                    }
                    break;
                }
            } catch (Exception e) {
                if (attempt == 2) throw e;
                Thread.sleep(2000);
            }
        }

        status.accept("Распаковка Java 21...");
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new java.io.BufferedInputStream(new java.io.FileInputStream(zipFile)))) {
            java.util.zip.ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(javaDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int read;
                        while ((read = zis.read(buf)) != -1) {
                            fos.write(buf, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        zipFile.delete();

        MinecraftProcess temp = new MinecraftProcess();
        File found = temp.findJavaInDir(javaDir);
        if (found == null) {
            throw new RuntimeException("Не удалось найти java.exe после распаковки");
        }

        try {
            marker.createNewFile();
        } catch (Exception ignored) {}

        status.accept("Java 21 готова: " + found.getAbsolutePath());
        return found.getAbsolutePath();
    }
}

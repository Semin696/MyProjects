package com.nightfallrealm.core;

import com.nightfallrealm.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ModManager {

    public static List<File> getMods() {
        File modsDir = Constants.MODS_DIR;
        if (!modsDir.exists()) return Collections.emptyList();
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }

    public static List<File> getResourcePacks() {
        File rpDir = Constants.RESOURCE_PACKS_DIR;
        if (!rpDir.exists()) return Collections.emptyList();
        File[] files = rpDir.listFiles((dir, name) ->
                name.endsWith(".zip") || new File(dir, name).isDirectory());
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }

    public static boolean addMod(File sourceJar) {
        return copyFile(sourceJar, Constants.MODS_DIR);
    }

    public static boolean removeMod(String fileName) {
        File target = new File(Constants.MODS_DIR, fileName);
        return target.delete();
    }

    public static boolean addResourcePack(File source) {
        return copyFile(source, Constants.RESOURCE_PACKS_DIR);
    }

    public static boolean removeResourcePack(String fileName) {
        File target = new File(Constants.RESOURCE_PACKS_DIR, fileName);
        if (target.isDirectory()) {
            try {
                Files.walk(target.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return target.delete();
    }

    public static long getTotalModSize() {
        long size = 0;
        for (File f : getMods()) size += f.length();
        return size;
    }

    private static boolean copyFile(File source, File targetDir) {
        if (!targetDir.exists()) targetDir.mkdirs();
        File target = new File(targetDir, source.getName());
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

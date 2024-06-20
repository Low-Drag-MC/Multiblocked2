package com.lowdragmc.mbd2.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;

public class FileUtils {
    public static void loadNBTFiles(File path, String extension, BiConsumer<Path, CompoundTag> consumer) {
        if (path.exists() && path.isDirectory()) {
            // walk through all files under the directory, including subdirectory
            try {
                Files.walkFileTree(path.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(extension)) {
                            try {
                                var tag = NbtIo.read(file.toFile());
                                if (tag != null) {
                                    consumer.accept(file, tag);
                                }
                            } catch (IOException ignored) {}
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }
        }
    }
}

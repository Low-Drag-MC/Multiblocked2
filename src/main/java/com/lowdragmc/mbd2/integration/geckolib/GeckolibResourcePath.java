package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public final class GeckolibResourcePath {

    private GeckolibResourcePath() {
    }

    public static Optional<ResourceLocation> fromAssetFile(File file) {
        var path = file.toPath().toAbsolutePath().normalize();
        var assetsDirectory = projectAssetsDirectory();
        if (!path.startsWith(assetsDirectory) || assetsDirectory.getNameCount() + 1 >= path.getNameCount()) {
            return Optional.empty();
        }
        var relativePath = assetsDirectory.relativize(path);
        var namespace = relativePath.getName(0).toString();
        var resourcePath = relativePath.subpath(1, relativePath.getNameCount()).toString().replace(File.separatorChar, '/');
        try {
            return Optional.of(ResourceLocation.fromNamespaceAndPath(namespace, resourcePath));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static Path projectAssetsDirectory() {
        return LDLib2.getAssetsDir().toPath().toAbsolutePath().normalize();
    }
}

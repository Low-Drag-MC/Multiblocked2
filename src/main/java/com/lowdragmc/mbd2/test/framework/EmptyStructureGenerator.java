package com.lowdragmc.mbd2.test.framework;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Run once to generate the empty structure NBT files used by MBD2 gametests.
 *
 * <pre>{@code
 * gradlew :emptyStructures   // (custom task), or invoke #main directly from IDEA
 * }</pre>
 *
 * The files are written to {@code src/main/resources/data/mbd2/structure/} and should be
 * committed. Re-run only when you need a new size.
 */
public final class EmptyStructureGenerator {

    private EmptyStructureGenerator() {}

    public static void main(String[] args) throws Exception {
        // SharedConstants must be initialized to get a sane DataVersion when running
        // outside the normal Minecraft bootstrap.
        SharedConstants.tryDetectVersion();
        Path outputDir = locateOutputDir();
        Files.createDirectories(outputDir);
        writeEmptyStructure(outputDir.resolve("empty_simple.nbt"), 3, 3, 3);
        writeEmptyStructure(outputDir.resolve("empty_multiblock.nbt"), 11, 11, 11);
        System.out.println("Empty structure NBT files generated in " + outputDir.toAbsolutePath());
    }

    private static Path locateOutputDir() {
        Path cwd = Paths.get("").toAbsolutePath();
        // Try a few common project-relative paths so this works whether the user runs from
        // the project root or from the build directory.
        Path[] candidates = {
                cwd.resolve("src/main/resources/data/mbd2/structure"),
                cwd.getParent() != null ? cwd.getParent().resolve("src/main/resources/data/mbd2/structure") : null,
        };
        for (Path candidate : candidates) {
            if (candidate == null) continue;
            Path parent = candidate.getParent();
            if (parent != null && Files.isDirectory(parent.getParent())) {
                return candidate;
            }
        }
        return candidates[0];
    }

    private static void writeEmptyStructure(Path file, int sizeX, int sizeY, int sizeZ) throws Exception {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        ListTag size = new ListTag();
        size.add(IntTag.valueOf(sizeX));
        size.add(IntTag.valueOf(sizeY));
        size.add(IntTag.valueOf(sizeZ));
        tag.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag airState = new CompoundTag();
        airState.putString("Name", "minecraft:air");
        palette.add(airState);
        tag.put("palette", palette);

        // explicit air block at every position so the framework knows what to place
        ListTag blocks = new ListTag();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    CompoundTag block = new CompoundTag();
                    ListTag pos = new ListTag();
                    pos.add(IntTag.valueOf(x));
                    pos.add(IntTag.valueOf(y));
                    pos.add(IntTag.valueOf(z));
                    block.put("pos", pos);
                    block.putInt("state", 0);
                    blocks.add(block);
                }
            }
        }
        tag.put("blocks", blocks);
        tag.put("entities", new ListTag());

        try (var out = Files.newOutputStream(file)) {
            NbtIo.writeCompressed(tag, out);
        }
        System.out.println("  wrote " + file + " (" + sizeX + "x" + sizeY + "x" + sizeZ + ")");
    }
}

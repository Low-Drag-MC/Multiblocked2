package com.lowdragmc.mbd2.common.gui.editor.multiblopck;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Optional;

public final class MultiblockAreaSelection {
    private MultiblockAreaSelection() {
    }

    public static Optional<Result> pick(BlockPos from, BlockPos to, BlockPos hit, Direction face) {
        var min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        var max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));
        if (hit.getX() < min.getX() || hit.getX() > max.getX()
                || hit.getY() < min.getY() || hit.getY() > max.getY()
                || hit.getZ() < min.getZ() || hit.getZ() > max.getZ()) {
            return Optional.empty();
        }
        return Optional.of(new Result(hit.subtract(min), face));
    }

    public record Result(BlockPos offset, Direction face) {
    }
}

package com.lowdragmc.mbd2.api.pattern.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One captured cell in a {@link PatternSnapshot}. {@code beData} is the
 * {@code saveWithFullMetadata} payload of the BlockEntity at this position,
 * captured at the time of snapshotting. It is {@code null} for positions whose
 * predicate set does not include an NBT-restricting predicate, or when no
 * BlockEntity exists at the position.
 */
public record SnapshotEntry(BlockState state, @Nullable CompoundTag beData) {}

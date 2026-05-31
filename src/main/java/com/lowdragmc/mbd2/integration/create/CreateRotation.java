package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Unified rotation-recipe content: a numeric amount whose meaning ({@link Mode#STRESS} or
 * {@link Mode#RPM}) is carried in the value itself, plus an optional {@link ToggleFloat}
 * that, when enabled, asks the consuming/generating machine to override its static torque
 * with the toggle's inner value while this recipe is running.
 */
public class CreateRotation {
    public enum Mode { STRESS, RPM }

    public float value;
    public Mode mode;
    public ToggleFloat torqueOverride;

    public CreateRotation() {
        this(0f, Mode.STRESS, ToggleFloat.ofDisabled());
    }

    public CreateRotation(float value, Mode mode, ToggleFloat torqueOverride) {
        this.value = value;
        this.mode = mode;
        this.torqueOverride = torqueOverride == null ? ToggleFloat.ofDisabled() : torqueOverride;
    }

    public static CreateRotation stress(float value) {
        return new CreateRotation(value, Mode.STRESS, ToggleFloat.ofDisabled());
    }

    public static CreateRotation rpm(float value) {
        return new CreateRotation(value, Mode.RPM, ToggleFloat.ofDisabled());
    }

    public CreateRotation withTorqueOverride(float torque) {
        this.torqueOverride = ToggleFloat.of(true, torque);
        return this;
    }

    public CreateRotation copy() {
        return new CreateRotation(value, mode, ToggleFloat.of(torqueOverride.isEnable(), torqueOverride.getValue()));
    }

    private static final Codec<ToggleFloat> TOGGLE_FLOAT_CODEC = RecordCodecBuilder.create(it -> it.group(
            Codec.BOOL.fieldOf("enable").forGetter(ToggleFloat::isEnable),
            Codec.FLOAT.fieldOf("value").forGetter(ToggleFloat::getValue)
    ).apply(it, ToggleFloat::of));

    public static final Codec<CreateRotation> CODEC = RecordCodecBuilder.create(it -> it.group(
            Codec.FLOAT.fieldOf("value").forGetter(c -> c.value),
            Codec.STRING.xmap(Mode::valueOf, Enum::name).fieldOf("mode").forGetter(c -> c.mode),
            TOGGLE_FLOAT_CODEC.optionalFieldOf("torque_override", ToggleFloat.ofDisabled())
                    .forGetter(c -> c.torqueOverride)
    ).apply(it, CreateRotation::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, ToggleFloat> TOGGLE_FLOAT_STREAM = StreamCodec.composite(
            ByteBufCodecs.BOOL, ToggleFloat::isEnable,
            ByteBufCodecs.FLOAT, ToggleFloat::getValue,
            ToggleFloat::of);

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateRotation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, c -> c.value,
            ByteBufCodecs.STRING_UTF8.map(Mode::valueOf, Enum::name), c -> c.mode,
            TOGGLE_FLOAT_STREAM, c -> c.torqueOverride,
            CreateRotation::new);

    /**
     * NBT round-trip helper for tests and external callers that prefer raw tags over the
     * Codec/StreamCodec APIs. Delegates to the {@link #CODEC} via NbtOps.
     */
    public CompoundTag toNbt(HolderLookup.Provider provider) {
        var op = provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        return (CompoundTag) CODEC.encodeStart(op, this).getOrThrow();
    }

    public static CreateRotation fromNbt(HolderLookup.Provider provider, CompoundTag tag) {
        var op = provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        return CODEC.parse(op, tag).getOrThrow();
    }
}

package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.utils.ShapeUtils;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.client.MachineSound;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.*;
import com.lowdragmc.mbd2.integration.geckolib.GeckolibRenderer;
import dev.latvian.mods.rhino.util.HideFromJS;
import lombok.*;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;

@Accessors(fluent = true)
@Getter
public class MachineState implements IConfigurable, IPersistedSerializable, Comparable<MachineState> {
    protected final String name;
    @NonNull
    protected List<MachineState> children;
    @Nullable
    protected MachineState parent;

    @Configurable(name = "config.machine_state.renderer", subConfigurable = true, tips =
            {"config.machine_state.renderer.tooltip.0", "config.machine_state.renderer.tooltip.1"})
    protected final ToggleRenderer renderer;

    @Configurable(name = "config.machine_state.shape", subConfigurable = true, tips =
            {"config.machine_state.shape.tooltip.0", "config.machine_state.shape.tooltip.1",
                    "config.machine_state.shape.tooltip.2", "config.machine_state.shape.tooltip.3",
                    "config.require_restart"})
    protected final ToggleShape shape;

    @Configurable(name = "config.machine_state.light", subConfigurable = true, tips =
            {"config.machine_state.light.tooltip.0", "config.machine_state.light.tooltip.1"})
    protected final ToggleLightValue lightLevel;

    @Configurable(name = "config.machine_state.rendering_box", subConfigurable = true, tips =
            {"config.machine_state.rendering_box.tooltip.0", "config.machine_state.rendering_box.tooltip.1",
                    "config.machine_state.rendering_box.tooltip.2"})
    protected final ToggleAABB renderingBox;
    @Configurable(name = "config.machine_state.machine_sound", subConfigurable = true, tips = {
            "config.machine_state.machine_sound.tooltip.0", "config.machine_state.machine_sound.tooltip.1",
            "config.machine_state.machine_sound.tooltip.2",
    })
    protected final ToggleMachineSound machineSound = new ToggleMachineSound();
    // runtime
    @Nullable
    private StateMachine<?> stateMachine;


    private final Map<Direction, VoxelShape> shapeCache = new EnumMap<>(Direction.class);
    private final Map<Direction, AABB> renderingBoxCache = new EnumMap<>(Direction.class);

    public MachineState(String name, @NonNull List<MachineState> children,
                        @Nullable IRenderer renderer,
                        @Nullable VoxelShape shape,
                        @Nullable Integer lightLevel,
                        @Nullable AABB renderingBox) {
        this.name = name;
        this.children = children;
        this.renderer = renderer == null ? new ToggleRenderer() : new ToggleRenderer(renderer);
        this.shape = shape == null ? new ToggleShape() : new ToggleShape(shape);
        this.lightLevel = lightLevel == null ? new ToggleLightValue() : new ToggleLightValue(lightLevel);
        this.renderingBox = renderingBox == null ? new ToggleAABB() : new ToggleAABB(renderingBox);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        tag.putString("name", name);
        var childrenList = new ListTag();
        for (var child : children) {
            childrenList.add(child.serializeNBT());
        }
        tag.put("children", childrenList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(tag);
        var childrenList = tag.getList("children", 10);
        children = new ArrayList<>();
        for (int i = 0; i < childrenList.size(); i++) {
            var child = childrenList.getCompound(i);
            children.add(createFromTag(child));
        }
        if (this.stateMachine != null) {
            this.stateMachine.initStateMachine();
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public MachineState addChild(String name) {
        return addChild(newBuilder().name(name).build());
    }

    protected MachineState addChild(MachineState state) {
        children = new ArrayList<>(children);
        children.add(state);
        if (this.stateMachine != null) {
            state.parent = this;
            state.init(this.stateMachine);
        }
        return state;
    }

    public void removeChild(MachineState state) {
        children = this.children.stream().filter(s -> s != state).toList();
        if (this.stateMachine != null) {
            this.stateMachine.initStateMachine();
        }
        state.onRemoved();
    }

    private void onRemoved() {
        this.stateMachine = null;
        this.parent = null;
        this.children.forEach(MachineState::onRemoved);
    }

    protected void init(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
        stateMachine.addState(this);
        for (MachineState child : children) {
            child.parent = this;
            child.init(stateMachine);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public IRenderer getRealRenderer() {
        return getRenderer();
    }

    @OnlyIn(Dist.CLIENT)
    public IRenderer getRenderer() {
        if (!renderer.isEnable() || renderer.getValue() == null) {
            if (parent != null) {
                return parent.getRenderer();
            } else {
                return IRenderer.EMPTY;
            }
        }
        return renderer.getValue();
    }

    public VoxelShape getShape(@Nullable Direction direction) {
        if (!shape.isEnable() || shape.getValue() == null) {
            if (parent != null) {
                return parent.getShape(direction);
            } else {
                return Shapes.block();
            }
        }
        var value = shape.getValue();
        if (value.isEmpty() || value == Shapes.block() || direction == Direction.NORTH || direction == null) return value;
        return this.shapeCache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(value, dir));
    }

    public int getLightLevel() {
        if (!lightLevel.isEnable() || lightLevel.getValue() == null) {
            if (parent != null) {
                return parent.getLightLevel();
            } else {
                return 0;
            }
        }
        return lightLevel.getValue();
    }

    @Nullable
    public AABB getRenderingBox(@Nullable Direction direction) {
        if (!renderingBox.isEnable() || renderingBox.getValue() == null) {
            if (parent != null) {
                return parent.getRenderingBox(direction);
            } else {
                return null;
            }
        }
        var value = renderingBox.getValue();
        return (direction == Direction.NORTH || direction == null) ? value : this.renderingBoxCache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(value, dir));
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public MachineSound createMachineSound(BlockPos pos, BooleanSupplier predicate) {
        if (!machineSound.isEnable()) {
            if (parent != null) {
                return parent.createMachineSound(pos, predicate);
            } else {
                return null;
            }
        }
        return machineSound.createMachineSound(pos, predicate);
    }

    public int getDepth() {
        if (parent == null) {
            return 0;
        }
        return parent.getDepth() + 1;
    }

    @Override
    public int compareTo(@NotNull MachineState o) {
        return Integer.compare(this.getDepth(), o.getDepth());
    }

    protected MachineState createFromTag(CompoundTag tag) {
        var name = tag.getString("name");
        var state = newBuilder().name(name).build();
        state.deserializeNBT(tag);
        return state;
    }

    protected Builder<? extends MachineState> newBuilder() {
        return MachineState.builder();
    }

    public static Builder<? extends MachineState> builder() {
        return new Builder<>();
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder<T extends MachineState> {
        protected String name;
        protected List<MachineState> children = new ArrayList<>();
        @Nullable
        protected IRenderer renderer;
        @Nullable
        protected VoxelShape shape;
        @Nullable
        protected Integer lightLevel;
        @Nullable
        protected AABB renderingBox;

        protected Builder() {
        }

        public Builder<T> child(MachineState child) {
            children.add(child);
            return this;
        }

        public Builder<T> modelRenderer(ResourceLocation modelPath) {
            return renderer(new IModelRenderer(modelPath));
        }

        @HideFromJS
        public Builder<T> geckolibRenderer(ResourceLocation modelPath, ResourceLocation texturePath, ResourceLocation animationPath) {
            if (MBD2.isGeckolibLoaded()) {
                return renderer(new GeckolibRenderer(modelPath, texturePath, animationPath));
            }
            return this;
        }

        public T build() {
            return (T) new MachineState(name, children, renderer, shape, lightLevel, renderingBox);
        }
    }
}

package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.ShapeUtils;
import com.lowdragmc.mbd2.client.MachineSound;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.*;
import lombok.*;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.function.Consumers;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Accessors(fluent = true)
@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@KJSBindings
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MachineState implements ITreeNode<MachineState, Void>, IConfigurable, IPersistedSerializable, Comparable<MachineState> {
    @EqualsAndHashCode.Include
    public final String name;
    protected int dimension;
    @Nullable
    protected MachineState parent;
    @NonNull
    protected final List<MachineState> children = new ArrayList<>();

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
    @Configurable(name = "config.machine_state.is_global_visible", tips =
            "config.machine_state.is_global_visible.tooltip")
    @Setter
    protected boolean isGlobalVisible = false;
    @Configurable(name = "config.machine_state.rendering_radius", tips =
            "config.machine_state.rendering_radius.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    @Setter
    protected int renderingRadius = 64;
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

    public MachineState(String name,
                        @Nullable MachineState parent,
                        @Nullable IRenderer renderer,
                        @Nullable VoxelShape shape,
                        @Nullable Integer lightLevel,
                        @Nullable AABB renderingBox) {
        this.name = name;
        this.parent = parent;
        this.dimension = parent == null ? 0 : parent.dimension + 1;
        this.renderer = renderer == null ? new ToggleRenderer() : new ToggleRenderer(renderer);
        this.shape = shape == null ? new ToggleShape() : new ToggleShape(shape);
        this.lightLevel = lightLevel == null ? new ToggleLightValue() : new ToggleLightValue(lightLevel);
        this.renderingBox = renderingBox == null ? new ToggleAABB() : new ToggleAABB(renderingBox);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = IPersistedSerializable.super.serializeNBT(provider);
        tag.putString("name", name);
        var childrenList = new ListTag();
        for (var child : children) {
            childrenList.add(child.serializeNBT(provider));
        }
        tag.put("children", childrenList);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(provider, tag);
        var childrenList = tag.getList("children", 10);
        children.clear();
        for (int i = 0; i < childrenList.size(); i++) {
            var childTag = childrenList.getCompound(i);
            var childState = newBuilder(childTag.getString("name")).build(this);
            childState.deserializeNBT(provider, childTag);
            children.add(childState);
        }
        if (this.stateMachine != null) {
            this.stateMachine.initStateMachine();
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public MachineState addChild(String name) {
        return addChild(newBuilder(name).build(this));
    }

    public MachineState addChild(MachineState state) {
        return addChildAt(state, children.size());
    }

    public MachineState addChildAt(MachineState state, int index) {
        children.add(index, state);
        if (this.stateMachine != null) {
            state.init(this.stateMachine);
        }
        state.parent = this;
        state.setDimension(dimension + 1);
        return state;
    }

    public int getChildSiblingIndex(MachineState state) {
        return children.indexOf(state);
    }

    public int getSiblingIndex(MachineState state) {
        return parent == null ? -1 : parent.getChildSiblingIndex(state);
    }

    public void removeChild(MachineState state) {
        if (children.remove(state)) {
            if (this.stateMachine != null) {
                this.stateMachine.initStateMachine();
            }
            state.onRemoved();
            state.setDimension(0);
            state.parent = null;
        }
    }

    private void onRemoved() {
        this.stateMachine = null;
        new ArrayList<>(this.children).forEach(MachineState::onRemoved);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void init(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
        stateMachine.addState(this);
        for (MachineState child : children) {
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
    public int compareTo(MachineState o) {
        return Integer.compare(this.getDepth(), o.getDepth());
    }

    protected Builder<? extends MachineState> newBuilder(String name) {
        return new Builder<>(name);
    }

    public static Builder<? extends MachineState> baseBuilder() {
        return new Builder<>("base");
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
        children.forEach(child -> child.setDimension(dimension + 1));
    }

    @Override
    public MachineState getKey() {
        return this;
    }

    @Override
    public @Nullable Void getContent() {
        return null;
    }

    @Override
    public @Nullable MachineState getParent() {
        return parent;
    }

    @Override
    public List<MachineState> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return name;
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder<T extends MachineState> {
        protected final String name;
        protected List<Builder<T>> childrenBuilders = new ArrayList<>();
        @Nullable
        protected IRenderer renderer;
        @Nullable
        protected VoxelShape shape;
        @Nullable
        protected Integer lightLevel;
        @Nullable
        protected AABB renderingBox;

        protected Builder(String name) {
            this.name = name;
        }

        public Builder<T> child(String name, Consumer<Builder<T>> builderConsumer) {
            var childBuilder = new Builder<T>(name);
            builderConsumer.accept(childBuilder);
            childrenBuilders.add(childBuilder);
            return this;
        }

        public Builder<T> child(String name) {
            return child(name, Consumers.nop());
        }

        public Builder<T> modelRenderer(ResourceLocation modelPath) {
            return renderer(new IModelRenderer(modelPath));
        }

//        @HideFromJS
//        public Builder<T> geckolibRenderer(ResourceLocation modelPath, ResourceLocation texturePath, ResourceLocation animationPath) {
//            if (MBD2.isGeckolibLoaded()) {
//                return renderer(new GeckolibRenderer(modelPath, texturePath, animationPath));
//            }
//            return this;
//        }

        public T build() {
            return build(null);
        }

        public T build(@Nullable MachineState parent) {
            var state = (T) new MachineState(name, parent, renderer, shape, lightLevel, renderingBox);
            for (Builder<T> childrenBuilder : childrenBuilders) {
                state.addChild(childrenBuilder.build(state));
            }
            return state;
        }
    }
}

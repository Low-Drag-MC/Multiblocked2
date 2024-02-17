package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.utils.ShapeUtils;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleInteger;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleShape;
import lombok.*;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.*;

@Accessors(fluent = true)
@Builder
public class MachineState implements IConfigurable, IPersistedSerializable {
    @Getter
    private final String name;
    @Singular
    @Getter
    @NonNull
    private List<MachineState> children;
    @Nullable
    @Getter
    private MachineState parent;

    @Configurable(name = "config.machine_state.renderer", subConfigurable = true, tips =
            {"config.machine_state.renderer.tooltip.0", "config.machine_state.renderer.tooltip.1"})
    @Builder.Default
    private ToggleRenderer renderer = new ToggleRenderer();

    @Configurable(name = "config.machine_state.shape", subConfigurable = true, tips =
            {"config.machine_state.shape.tooltip.0", "config.machine_state.shape.tooltip.1",
                    "config.machine_state.shape.tooltip.2", "config.machine_state.shape.tooltip.3"})
    @Builder.Default
    private ToggleShape shape = new ToggleShape();

    @Configurable(name = "config.machine_state.light", subConfigurable = true, tips =
            {"config.machine_state.light.tooltip.0", "config.machine_state.light.tooltip.1"})
    @Builder.Default
    private ToggleInteger lightLevel = new ToggleInteger();

    // runtime
    @Getter
    @Nullable
    private StateMachine stateMachine = null;


    private final Map<Direction, VoxelShape> cache = new EnumMap<>(Direction.class);

    public static MachineState fromTag(CompoundTag tag) {
        var name = tag.getString("name");
        var state = MachineState.builder().name(name).build();
        state.deserializeNBT(tag);
        return state;
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
            children.add(MachineState.fromTag(child));
        }
        if (this.stateMachine != null) {
            this.stateMachine.initStateMachine();
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public void addChild(MachineState state) {
        children = new ArrayList<>(children);
        children.add(state);
        if (this.stateMachine != null) {
            state.parent = this;
            state.init(this.stateMachine);
        }
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

    public VoxelShape getShape(Direction direction) {
        if (!shape.isEnable() || shape.getValue() == null) {
            if (parent != null) {
                return parent.getShape(direction);
            } else {
                return Shapes.block();
            }
        }
        var value = shape.getValue();
        if (value.isEmpty() || value == Shapes.block() || direction == Direction.NORTH) return value;
        return this.cache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(value, dir));
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
}

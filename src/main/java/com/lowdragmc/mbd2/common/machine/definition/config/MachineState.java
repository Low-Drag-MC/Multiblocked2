package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.utils.ShapeUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.*;

@Accessors(fluent = true)
@Builder
public class MachineState {
    @Getter
    private final String name;
    @Singular
    @Getter
    @NonNull
    private List<MachineState> children;
    @Nullable
    @Getter
    private MachineState parent;

    @Nullable
    @Builder.Default
    private IRenderer renderer = null;
    @Nullable
    @Builder.Default
    private VoxelShape shape = null;
    @Nullable
    @Builder.Default
    private Integer lightLevel = null;

    private final Map<Direction, VoxelShape> cache = new EnumMap<>(Direction.class);

    public boolean isRoot() {
        return parent == null;
    }

    protected void init(StateMachine stateMachine) {
        stateMachine.addState(this);
        for (MachineState child : children) {
            child.parent = this;
            child.init(stateMachine);
        }
    }
    public IRenderer getRenderer() {
        if (renderer == null) {
            if (parent != null) {
                return parent.getRenderer();
            } else {
                return IRenderer.EMPTY;
            }
        }
        return renderer;
    }

    public VoxelShape getShape(Direction direction) {
        if (shape == null) {
            if (parent != null) {
                return parent.getShape(direction);
            } else {
                return Shapes.block();
            }
        }
        if (shape.isEmpty() || shape == Shapes.block() || direction == Direction.NORTH) return shape;
        return this.cache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(shape, dir));
    }

    public int getLightLevel() {
        if (lightLevel == null) {
            if (parent != null) {
                return parent.getLightLevel();
            } else {
                return 0;
            }
        }
        return lightLevel;
    }
}

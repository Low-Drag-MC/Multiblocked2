package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

public class ToggleShape extends ToggleObject<VoxelShape> implements INBTSerializable<CompoundTag> {
    public static final AABB BLOCK = new AABB(0, 0, 0, 1, 1, 1);

    @Configurable(name = "ToggleShape.shapes")
    private final List<AABB> aabbs = new ArrayList<>();

    // run-time
    private VoxelShape value;

    public ToggleShape(VoxelShape value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleShape(VoxelShape value) {
        this(value, true);
    }

    public ToggleShape(boolean enable) {
        this(Shapes.block(), enable);
    }

    public ToggleShape() {
        this(false);
    }

    public VoxelShape getValue() {
        if (value == null) {
            value = aabbs.stream().map(Shapes::create).reduce(Shapes.empty(), Shapes::or);
        }
        return value;
    }

    @Override
    public void setValue(VoxelShape value) {
        this.value = null;
        this.aabbs.clear();
        this.aabbs.addAll(value.toAabbs());
    }

    @ConfigSetter(field = "aabbs")
    private void onShapeChanged(List<AABB> aabbs) {
        value = null;
    }
}

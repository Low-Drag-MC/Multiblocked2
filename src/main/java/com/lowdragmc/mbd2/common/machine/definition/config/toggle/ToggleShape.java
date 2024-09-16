package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.configurator.AABBConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class ToggleShape extends ToggleObject<VoxelShape> implements ITagSerializable<CompoundTag> {
    public static final AABB BLOCK = new AABB(0, 0, 0, 1, 1, 1);
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

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var shapeGroup = new ArrayConfiguratorGroup<>("shapes", false, () -> new ArrayList<>(aabbs),
                (getter, setter) -> new AABBConfigurator("cube", getter, setter, BLOCK, true), true);
        shapeGroup.setAddDefault(() -> new AABB(0, 0, 0, 1, 1, 1));
        shapeGroup.setOnAdd(aabbs::add);
        shapeGroup.setOnRemove(aabbs::remove);
        shapeGroup.setOnUpdate(list -> {
            aabbs.clear();
            aabbs.addAll(list);
            value = null;
        });
        father.addConfigurators(shapeGroup);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putBoolean("enable", enable);
        var shape = new ListTag();
        for (AABB aabb : aabbs) {
            var aabbTag = new CompoundTag();
            aabbTag.putDouble("minX", aabb.minX);
            aabbTag.putDouble("minY", aabb.minY);
            aabbTag.putDouble("minZ", aabb.minZ);
            aabbTag.putDouble("maxX", aabb.maxX);
            aabbTag.putDouble("maxY", aabb.maxY);
            aabbTag.putDouble("maxZ", aabb.maxZ);
            shape.add(aabbTag);
        }
        tag.put("value", shape);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        enable = compoundTag.getBoolean("enable");
        this.aabbs.clear();
        var shape = compoundTag.getList("value", Tag.TAG_COMPOUND);
        for (Tag tag : shape) {
            var aabbTag = (CompoundTag) tag;
            var aabb = new AABB(
                    aabbTag.getDouble("minX"),
                    aabbTag.getDouble("minY"),
                    aabbTag.getDouble("minZ"),
                    aabbTag.getDouble("maxX"),
                    aabbTag.getDouble("maxY"),
                    aabbTag.getDouble("maxZ")
            );
            this.aabbs.add(aabb);
        }
    }
}

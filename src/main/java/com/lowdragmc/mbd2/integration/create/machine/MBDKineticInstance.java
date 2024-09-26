package com.lowdragmc.mbd2.integration.create.machine;

import com.jozufozu.flywheel.api.InstanceData;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.render.AllMaterialSpecs;
import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class MBDKineticInstance extends KineticBlockEntityInstance<MBDKineticMachineBlockEntity> {

    protected final List<RotatingData> keys = new ArrayList<>();

    public MBDKineticInstance(MaterialManager modelManager, MBDKineticMachineBlockEntity tile, PartialModel model) {
        super(modelManager, tile);
        var speed = tile.getSpeed();
        var rotationFacing = tile.definition.kineticMachineSettings.getRotationFacing(tile.getMetaMachine().getFrontFacing().orElse(Direction.NORTH));
        var rotatingData = materialManager
                .defaultSolid()
                .material(AllMaterialSpecs.ROTATING)
                .getModel(model, blockState, rotationFacing, CachedBufferer.rotateToFaceVertical(rotationFacing))
                .createInstance();
        setup(rotatingData, speed);
        keys.add(rotatingData);
    }

    @Override
    public void update() {
        for (var rotatingData : keys) {
            updateRotation(rotatingData, blockEntity.getSpeed());
        }
    }

    @Override
    public void updateLight() {
        relight(pos, keys.stream());
    }

    @Override
    public void remove() {
        keys.forEach(InstanceData::delete);
        keys.clear();
    }
}

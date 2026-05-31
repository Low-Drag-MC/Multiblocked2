package com.lowdragmc.mbd2.integration.create.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MBDKineticInstance extends KineticBlockEntityVisual<MBDKineticMachineBlockEntity> {

    protected final List<RotatingInstance> keys = new ArrayList<>();

    public MBDKineticInstance(VisualizationContext modelManager, MBDKineticMachineBlockEntity tile, float partialTick, PartialModel model) {
        super(modelManager, tile, partialTick);
        var speed = tile.getSpeed();
        var rotationFacing = tile.definition.kineticMachineSettings().getRotationFacing(
                tile.getMetaMachine().getFrontFacing().orElse(Direction.NORTH));
        var instance = modelManager.instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model)).createInstance();
        instance.setup(blockEntity, speed)
                .setPosition(getVisualPosition())
                .rotateToFace(rotationFacing)
                .setChanged();
        keys.add(instance);
    }

    @Override
    public void update(float partialTick) {
        for (var rotatingData : keys) {
            rotatingData.setup(blockEntity, blockEntity.getSpeed()).setChanged();
        }
    }

    @Override
    public void updateLight(float partialTick) {
        relight(keys.toArray(FlatLit[]::new));
    }

    @Override
    public void _delete() {
        keys.forEach(AbstractInstance::delete);
        keys.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        keys.forEach(consumer);
    }
}

package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * {@link MachineState} subclass for Create kinetic machines, carrying an extra
 * {@code rotationRenderer} per state. {@link #getRealRenderer()} composes the base renderer
 * with a {@link KineticInstanceRenderer} so the user-configured rotating model spins on the
 * machine block. {@link #getRotationRenderer()} walks the parent chain so child states inherit
 * the rotation renderer from a parent when not overridden.
 */
public class CreateMachineState extends MachineState {

    @Persisted(subPersisted = true)
    @Configurable(name = "config.create_machine_state.rotation_renderer", subConfigurable = true,
            tips = "config.create_machine_state.rotation_renderer.tooltip")
    private final ToggleRenderer rotationRenderer;

    public CreateMachineState(String name,
                              @Nullable MachineState parent,
                              @Nullable IRenderer renderer,
                              @Nullable VoxelShape shape,
                              @Nullable Integer lightLevel,
                              @Nullable AABB renderingBox,
                              @Nullable IRenderer rotationRenderer) {
        super(name, parent, renderer, shape, lightLevel, renderingBox);
        this.rotationRenderer = rotationRenderer == null ? new ToggleRenderer() : new ToggleRenderer(rotationRenderer);
    }

    /**
     * Walk the parent chain until a state with an enabled rotation renderer is found.
     * Returns {@link IRenderer#EMPTY} if no parent in the chain has one set.
     */
    public IRenderer getRotationRenderer() {
        if (!rotationRenderer.isEnable() || rotationRenderer.getValue() == null) {
            if (getParent() instanceof CreateMachineState parent) {
                return parent.getRotationRenderer();
            }
            return IRenderer.EMPTY;
        }
        return rotationRenderer.getValue();
    }

    /**
     * Resolve a Flywheel {@link PartialModel} from the rotation renderer chain.
     * Falls back to {@link AllPartialModels#SHAFT_HALF} when no usable model renderer is configured.
     */
    @OnlyIn(Dist.CLIENT)
    public PartialModel getRotationPartialModel() {
        var rr = getRotationRenderer();
        while (rr instanceof UIResourceRenderer uiResourceRenderer) {
            rr = uiResourceRenderer.getInternalRenderer();
        }
        if (rr instanceof IModelRenderer modelRenderer) {
            return PartialModel.of(modelRenderer.getModelLocation());
        }
        return AllPartialModels.SHAFT_HALF;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IRenderer getRealRenderer() {
        var base = super.getRealRenderer();
        var rr = getRotationRenderer();
        while (rr instanceof UIResourceRenderer uiResourceRenderer) {
            rr = uiResourceRenderer.getInternalRenderer();
        }
        if (rr instanceof IModelRenderer modelRenderer) {
            return new KineticInstanceRenderer(base, PartialModel.of(modelRenderer.getModelLocation()));
        }
        return base;
    }

    @Override
    protected Builder newBuilder(String name) {
        return new Builder(name);
    }

    public static Builder builder() {
        return new Builder("base");
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MachineState.Builder<CreateMachineState> {
        @Nullable
        protected IRenderer rotationRenderer;

        protected Builder(String name) {
            super(name);
        }

        @Override
        public CreateMachineState build(@Nullable MachineState parent) {
            var state = new CreateMachineState(name, parent, renderer, shape, lightLevel, renderingBox, rotationRenderer);
            for (var childBuilder : childrenBuilders) {
                state.addChild(childBuilder.build(state));
            }
            return state;
        }
    }
}

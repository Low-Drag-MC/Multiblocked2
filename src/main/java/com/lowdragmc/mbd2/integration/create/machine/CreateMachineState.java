package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleAABB;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleLightValue;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleShape;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

public class CreateMachineState extends MachineState {

    @Persisted(subPersisted = true)
    private final ToggleRenderer rotationRenderer;

    public CreateMachineState(String name, @NonNull List<MachineState> children, ToggleRenderer renderer, ToggleShape shape, ToggleLightValue lightLevel, ToggleAABB renderingBox, ToggleRenderer rotationRenderer) {
        super(name, children, renderer, shape, lightLevel, renderingBox);
        this.rotationRenderer = rotationRenderer;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        if (name.equals("base")) {
            var configurator = new ConfiguratorGroup("config.create_machine_state.rotation_renderer");
            configurator.setTips("config.create_machine_state.rotation_renderer.tooltip");
            rotationRenderer.buildConfigurator(configurator);
            father.addConfigurators(configurator);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IRenderer getRealRenderer() {
        var baseRenderer = getRenderer();
        var rotationRenderer = getRotationRenderer();
        while (rotationRenderer instanceof UIResourceRenderer uiResourceRenderer) {
            rotationRenderer = uiResourceRenderer.getRenderer();
        }
        if (rotationRenderer instanceof IModelRenderer modelRenderer) {
            return new KineticInstanceRenderer(baseRenderer, modelRenderer.getRotatedModel(Direction.NORTH));
        }
        return baseRenderer;
    }

    public IRenderer getRotationRenderer() {
        if (!rotationRenderer.isEnable() || rotationRenderer.getValue() == null) {
            if (parent instanceof CreateMachineState state) {
                return state.getRotationRenderer();
            } else {
                return IRenderer.EMPTY;
            }
        }
        return rotationRenderer.getValue();
    }

    @Override
    protected MachineState.Builder<? extends CreateMachineState> newBuilder() {
        return builder();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MachineState.Builder<CreateMachineState> {
        private ToggleRenderer rotationRenderer = new ToggleRenderer();

        @Override
        public CreateMachineState build() {
            return new CreateMachineState(name, children, renderer, shape, lightLevel, renderingBox, rotationRenderer);
        }
    }
}

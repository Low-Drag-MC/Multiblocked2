package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@Setter
@KJSBindings
public class CapabilityIO implements IConfigurable {
    @Configurable(name = "config.definition.trait.capability_io.internal", tips = "config.definition.trait.capability_io.internal.tooltip", forceUpdate = true)
    private IO internal = IO.BOTH;
    @Configurable(name = "config.definition.trait.capability_io.front", forceUpdate = true)
    private IO frontIO = IO.BOTH;
    @Configurable(name = "config.definition.trait.capability_io.back", forceUpdate = true)
    private IO backIO = IO.BOTH;
    @Configurable(name = "config.definition.trait.capability_io.left", forceUpdate = true)
    private IO leftIO = IO.BOTH;
    @Configurable(name = "config.definition.trait.capability_io.right", forceUpdate = true)
    private IO rightIO = IO.BOTH;
    @Configurable(name = "config.definition.trait.capability_io.top", forceUpdate = true)
    private IO topIO = IO.BOTH;
    @Configurable(name = "config.definition.trait.capability_io.bottom", forceUpdate = true)
    private IO bottomIO = IO.BOTH;

    public BulkIOState getAllIOState() {
        return BulkIOState.combineIO(List.of(internal, frontIO, backIO, leftIO, rightIO, topIO, bottomIO));
    }

    public void setAllIO(IO io) {
        internal = io;
        frontIO = io;
        backIO = io;
        leftIO = io;
        rightIO = io;
        topIO = io;
        bottomIO = io;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void buildConfigurator(ConfiguratorGroup father) {
        BulkIOConfigurators.add(father, "config.definition.trait.io.all",
                "config.definition.trait.io.all.capability.tooltip", this::getAllIOState, this::setAllIO);
        ConfiguratorParser.createConfigurators(father, this);
    }

    public IO getIO(Direction front, @Nullable Direction side) {
        if (front.getAxis() == Direction.Axis.Y) {
            if (side == front) {
                return frontIO;
            } else if (side == front.getOpposite()) {
                return backIO;
            } else {
                return internal;
            }
        }
        if (side == null) {
            return internal;
        }
        if (side == Direction.UP) {
            return topIO;
        } else if (side == Direction.DOWN) {
            return bottomIO;
        } else if (side == front) {
            return frontIO;
        } else if (side == front.getOpposite()) {
            return backIO;
        } else if (side == front.getClockWise()) {
            return rightIO;
        } else if (side == front.getCounterClockWise()) {
            return leftIO;
        }
        return IO.NONE;
    }
}

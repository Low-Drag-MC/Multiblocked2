package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.phys.AABB;


@Setter
@Getter
public class AutoWorldIO implements IToggleConfigurable {
    public boolean enable;
    @Configurable(name = "config.definition.trait.auto_world_io.range", tips = "config.definition.trait.auto_world_io.range.tooltip")
    @DefaultValue(numberValue = {-1, -1, -1, 2, 2, 2})
    @Accessors(chain = true)
    public AABB range = new AABB(-1, -1, -1, 2, 2, 2);
    @Configurable(name = "config.definition.trait.auto_world_io.interval", tips = "config.definition.trait.auto_world_io.interval.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    @Accessors(chain = true)
    public int interval = 20;
    @Configurable(name = "config.definition.trait.auto_world_io.speed", tips = "config.definition.trait.auto_world_io.speed.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    @Accessors(chain = true)
    public int speed = 64;

    // Rotation is memoised per trait instance by RuntimeAutoWorldIO, not here: a cache on this object
    // would be shared by every machine of the type and had no invalidation when the range changed.
}

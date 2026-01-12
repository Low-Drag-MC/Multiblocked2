package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.utils.ShapeUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.Map;

@Setter
@Getter
public class AutoWorldIO implements IToggleConfigurable {
    @Persisted
    public boolean enable;
    @Configurable(name = "config.definition.trait.auto_world_io.range", tips = "config.definition.trait.auto_world_io.range.tooltip")
    @DefaultValue(numberValue = {-1, -1, -1, 2, 2, 2})
    @Accessors(chain = true)
    public AABB range = new AABB(-1, -1, -1, 2, 2, 2);
    @Configurable(name = "config.definition.trait.auto_world_io.interval", tips = "config.definition.trait.auto_world_io.interval.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    @Accessors(chain = true)
    public int interval = 20;
    @Configurable(name = "config.definition.trait.auto_world_io.speed", tips = "config.definition.trait.auto_world_io.speed.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    @Accessors(chain = true)
    public int speed = 64;

    // runtime
    private final Map<Direction, AABB> rangeCache = new EnumMap<>(Direction.class);

    public AABB getRotatedRange(Direction direction) {
        return (direction == Direction.NORTH || direction == null) ? range : this.rangeCache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(range, dir));
    }
}

package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import software.bernie.geckolib.core.animation.Animation;

import java.util.HashMap;
import java.util.List;


@Accessors(chain = true)
@Getter
@Setter
public class AnimationStage implements IConfigurable, ITagSerializable<CompoundTag> {

    public enum LoopType {
        DEFAULT(Animation.LoopType.DEFAULT),
        PLAY_ONCE(Animation.LoopType.PLAY_ONCE),
        HOLD_ON_LAST_FRAME(Animation.LoopType.HOLD_ON_LAST_FRAME),
        LOOP(Animation.LoopType.LOOP);

        public final Animation.LoopType type;

        LoopType(Animation.LoopType type) {
            this.type = type;
        }
    }

    public static final String WAIT = "internal.wait";

    private boolean isWait = false;
    private int additionalTicks = 0;
    @Configurable(name="geckolib_renderer.animation_stage.animation_name", tips="geckolib_renderer.animation_stage.animation_name.tips")
    private String animationName = "";
    @Configurable(name="geckolib_renderer.animation_stage.loop_type", tips="geckolib_renderer.animation_stage.loop_type.tips")
    private AnimationStage.LoopType loopType = AnimationStage.LoopType.DEFAULT;

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putBoolean("isWait", isWait);
        tag.putInt("additionalTicks", additionalTicks);
        tag.putString("animationName", animationName);
        tag.putString("loopType", loopType.name());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        isWait = nbt.getBoolean("isWait");
        additionalTicks = nbt.getInt("additionalTicks");
        animationName = nbt.getString("animationName");
        loopType = AnimationStage.LoopType.valueOf(nbt.getString("loopType"));
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        father.setCollapse(false);
        father.addConfigurators(new ConfiguratorSelectorConfigurator<>(
                "geckolib_renderer.animation_stage.type",
                false, () -> isWait,
                value -> isWait = value,
                false, true,
                List.of(true, false),
                value -> value ? "geckolib_renderer.animation_stage.wait" : "geckolib_renderer.animation_stage.play",
                (value, configurator) -> {
                    if (value) {
                        configurator.addConfigurators(new NumberConfigurator(
                                "geckolib_renderer.animation_stage.additional_ticks",
                                () -> additionalTicks,
                                value1 -> additionalTicks = value1.intValue(),
                                0,
                                true).setRange(0, Integer.MAX_VALUE).setWheel(1));
                    } else {
                        ConfiguratorParser.createConfigurators(configurator, new HashMap<>(), getClass(), this);
                    }
                }
        ));
    }
}

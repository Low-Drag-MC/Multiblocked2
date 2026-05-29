package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Accessors(chain = true)
@Getter
@Setter
public class AnimationStage implements IConfigurable {
    public enum LoopType {
        DEFAULT(software.bernie.geckolib.animation.Animation.LoopType.DEFAULT),
        PLAY_ONCE(software.bernie.geckolib.animation.Animation.LoopType.PLAY_ONCE),
        HOLD_ON_LAST_FRAME(software.bernie.geckolib.animation.Animation.LoopType.HOLD_ON_LAST_FRAME),
        LOOP(software.bernie.geckolib.animation.Animation.LoopType.LOOP);

        public final software.bernie.geckolib.animation.Animation.LoopType type;

        LoopType(software.bernie.geckolib.animation.Animation.LoopType type) {
            this.type = type;
        }
    }

    private transient Supplier<List<AnimationInfo>> animationInfoSource = List::of;
    private boolean isWait = false;
    private int additionalTicks = 0;
    private String animationName = "";
    @Configurable(name = "geckolib_renderer.animation_stage.loop_type", tips = "geckolib_renderer.animation_stage.loop_type.tips")
    private LoopType loopType = LoopType.DEFAULT;

    public AnimationStage attachAnimationInfoSource(Supplier<List<AnimationInfo>> animationInfoSource) {
        this.animationInfoSource = animationInfoSource == null ? List::of : animationInfoSource;
        return this;
    }

    public List<AnimationInfo> getAvailableAnimations() {
        return animationInfoSource.get();
    }

    public List<String> getAvailableAnimationNames() {
        return getAvailableAnimations().stream().map(AnimationInfo::name).toList();
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putBoolean("isWait", isWait);
        tag.putInt("additionalTicks", additionalTicks);
        tag.putString("animationName", animationName);
        tag.putString("loopType", loopType.name());
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        isWait = nbt.getBoolean("isWait");
        additionalTicks = nbt.getInt("additionalTicks");
        animationName = nbt.getString("animationName");
        try {
            loopType = LoopType.valueOf(nbt.getString("loopType"));
        } catch (IllegalArgumentException ignored) {
            loopType = LoopType.DEFAULT;
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        father.setCollapse(false);
        father.addConfigurators(new ConfiguratorSelectorConfigurator<>(
                "geckolib_renderer.animation_stage.type",
                () -> isWait,
                value -> isWait = value,
                false,
                true,
                List.of(true, false),
                value -> value ? "geckolib_renderer.animation_stage.wait" : "geckolib_renderer.animation_stage.play",
                (selectedWait, configurator) -> {
                    if (selectedWait) {
                        configurator.addConfigurators(new NumberConfigurator(
                                "geckolib_renderer.animation_stage.additional_ticks",
                                () -> additionalTicks,
                                value1 -> additionalTicks = value1.intValue(),
                                0,
                                true).setRange(0, Integer.MAX_VALUE).setWheel(1));
                    } else {
                        configurator.addConfigurators(new SearchComponentConfigurator<>(
                                "geckolib_renderer.animation_stage.animation_name",
                                () -> animationName,
                                selectedName -> animationName = selectedName == null ? "" : selectedName,
                                "",
                                true,
                                (word, consumer) -> {
                                    var filter = word == null ? "" : word.toLowerCase(Locale.ROOT);
                                    getAvailableAnimations().stream()
                                            .map(AnimationInfo::name)
                                            .filter(name -> filter.isBlank() || name.toLowerCase(Locale.ROOT).contains(filter))
                                            .forEach(consumer);
                                },
                                selectedName -> selectedName == null ? "" : selectedName,
                                candidate -> new Label().setText(candidate == null ? Component.literal("---") : Component.literal(candidate))
                        ).setTips("geckolib_renderer.animation_stage.animation_name.tips"));
                        ConfiguratorParser.createConfigurators(configurator, new HashMap<>(), getClass(), this);
                    }
                }
        ));
    }
}

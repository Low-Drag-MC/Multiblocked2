package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Accessors(chain = true)
@Getter
@Setter
public class Animation implements IConfigurable {
    private transient Supplier<List<AnimationInfo>> animationInfoSource = List::of;
    @Configurable(name = "geckolib_renderer.animation.animation_name", tips = "geckolib_renderer.animation.animation_name.tips")
    private String name = "";
    private final List<AnimationStage> stages = new AnimationStageList();

    public Animation attachAnimationInfoSource(Supplier<List<AnimationInfo>> animationInfoSource) {
        this.animationInfoSource = animationInfoSource == null ? List::of : animationInfoSource;
        stages.forEach(stage -> stage.attachAnimationInfoSource(this.animationInfoSource));
        return this;
    }

    public List<AnimationInfo> getAvailableAnimations() {
        return animationInfoSource.get();
    }

    public List<String> getAvailableAnimationNames() {
        return getAvailableAnimations().stream().map(AnimationInfo::name).toList();
    }

    private AnimationStage attachStage(AnimationStage stage) {
        return stage.attachAnimationInfoSource(animationInfoSource);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        attachAnimationInfoSource(animationInfoSource);
        IConfigurable.super.buildConfigurator(father);
        var stageConfigurator = new ArrayConfiguratorGroup<>(
                "geckolib_renderer.animation_stages",
                false,
                () -> new ArrayList<>(stages),
                (getter, setter) -> {
                    var value = getter.get();
                    value.attachAnimationInfoSource(animationInfoSource);
                    var group = new ConfiguratorGroup("geckolib_renderer.animation_stage", false);
                    value.buildConfigurator(group);
                    return group;
                },
                true);
        stageConfigurator.setOnReorder((i, stage) -> {});
        stageConfigurator.setAddDefault(() -> new AnimationStage().attachAnimationInfoSource(animationInfoSource));
        stageConfigurator.setOnUpdate(list -> {
            stages.clear();
            stages.addAll(list);
            attachAnimationInfoSource(animationInfoSource);
        });
        father.addConfigurators(stageConfigurator);
    }

    public RawAnimation toRawAnimation() {
        var rawAnimation = RawAnimation.begin();
        for (var stage : stages) {
            if (stage.isWait()) {
                rawAnimation.thenWait(stage.getAdditionalTicks());
            } else if (!stage.getAnimationName().isBlank()) {
                rawAnimation.then(stage.getAnimationName(), stage.getLoopType().type);
            }
        }
        return rawAnimation;
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putString("name", name);
        var stages = new ListTag();
        for (var stage : this.stages) {
            stages.add(stage.serializeNBT());
        }
        tag.put("stages", stages);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        name = nbt.getString("name");
        stages.clear();
        var stages = nbt.getList("stages", Tag.TAG_COMPOUND);
        for (var stage : stages) {
            var stageInstance = new AnimationStage();
            stageInstance.deserializeNBT((CompoundTag) stage);
            this.stages.add(stageInstance);
        }
    }

    private class AnimationStageList extends ArrayList<AnimationStage> {
        @Override
        public boolean add(AnimationStage stage) {
            return super.add(attachStage(stage));
        }

        @Override
        public void add(int index, AnimationStage element) {
            super.add(index, attachStage(element));
        }

        @Override
        public boolean addAll(Collection<? extends AnimationStage> collection) {
            return super.addAll(collection.stream().map(Animation.this::attachStage).toList());
        }

        @Override
        public boolean addAll(int index, Collection<? extends AnimationStage> collection) {
            return super.addAll(index, collection.stream().map(Animation.this::attachStage).toList());
        }

        @Override
        public AnimationStage set(int index, AnimationStage element) {
            return super.set(index, attachStage(element));
        }
    }
}

package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true)
@Getter
@Setter
public class Animation implements IConfigurable, ITagSerializable<CompoundTag> {
    @Configurable(name = "geckolib_renderer.animation.animation_name", tips = "geckolib_renderer.animation.animation_name.tips")
    private String name = "";
    private List<AnimationStage> stages = new ArrayList<>();

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        var stageConfigurator = new ArrayConfiguratorGroup<>("geckolib_renderer.animation_stages", false,
                () -> new ArrayList<>(stages), (getter, setter) -> {
            var value = getter.get();
            var group = new ConfiguratorGroup("geckolib_renderer.animation_stage", false);
            value.buildConfigurator(group);
            return group;
        }, true);
        stageConfigurator.setOnReorder((i, stage) -> {});
        stageConfigurator.setAddDefault(AnimationStage::new);
        stageConfigurator.setOnAdd(newStage -> stages.add(newStage));
        stageConfigurator.setOnRemove(stages::remove);
        stageConfigurator.setOnUpdate(list -> {
            stages.clear();
            stages.addAll(list);
        });
        father.addConfigurators(stageConfigurator);
    }

    public RawAnimation toRawAnimation() {
        var rawAnimation = RawAnimation.begin();
        for (var stage : stages) {
            if (stage.isWait()) {
                rawAnimation.thenWait(stage.getAdditionalTicks());
            } else {
                rawAnimation.then(stage.getAnimationName(), stage.getLoopType().type);
            }
        }
        return rawAnimation;
    }

    @Override
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

    @Override
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
}

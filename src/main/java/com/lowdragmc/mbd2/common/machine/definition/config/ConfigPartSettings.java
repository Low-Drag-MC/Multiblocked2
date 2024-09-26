package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigPanel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigPartSettings implements IToggleConfigurable, IPersistedSerializable {

    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public void setEnable(boolean enable) {
        if (enable && !this.enable && LDLib.isClient() && Editor.INSTANCE instanceof MachineEditor machineEditor
                && machineEditor.getCurrentProject() instanceof MachineProject project) {
            // ask to add the "formed" state
            if (!project.getDefinition().stateMachine().hasState("formed")) {
                DialogWidget.showCheckBox(Editor.INSTANCE, "config.part_settings.formed_tips", "config.part_settings.formed_tips.info", result -> {
                    if (result) {
                        var state = project.getDefinition().stateMachine().getRootState();
                        var newState = state.addChild("formed");
                        machineEditor.getTabPages().tabs.values().stream()
                                .filter(MachineConfigPanel.class::isInstance)
                                .map(MachineConfigPanel.class::cast)
                                .findAny().ifPresent(panel -> panel.onStateAdded(newState));
                    }
                });
            }
        }
        this.enable = enable;
    }

    @Configurable(name = "config.part_settings.can_share", tips = {"config.part_settings.can_share.tooltip"})
    @Builder.Default
    protected boolean canShare = true;
    @Builder.Default
    protected final List<RecipeModifier> recipeModifiers = new ArrayList<>();

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        var modifiers = new ListTag();
        for (RecipeModifier modifier : recipeModifiers) {
            modifiers.add(modifier.serializeNBT());
        }
        tag.put("recipeModifiers", modifiers);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(tag);
        recipeModifiers.clear();
        var modifiers = tag.getList("recipeModifiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < modifiers.size(); i++) {
            var modifier = new RecipeModifier();
            modifier.deserializeNBT(modifiers.getCompound(i));
            recipeModifiers.add(modifier);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IToggleConfigurable.super.buildConfigurator(father);
        var modifiers = new ArrayConfiguratorGroup<>("config.part_settings.recipe_modifiers", false,
                () -> recipeModifiers, (getter, setter) -> {
            var recipeModifier = getter.get();
            var group = new ConfiguratorGroup("config.part_settings.content_modifier", false);
            recipeModifier.buildConfigurator(group);
            return group;
        }, true);
        modifiers.setTips("config.part_settings.recipe_modifiers.tooltip");
        modifiers.setAddDefault(RecipeModifier::new);
        modifiers.setOnAdd(recipeModifiers::add);
        modifiers.setOnRemove(recipeModifiers::remove);
        modifiers.setOnUpdate(list -> {
            recipeModifiers.clear();
            recipeModifiers.addAll(list);
        });
        father.addConfigurators(modifiers);
    }

    /**
     * To modify the controller recipe on the fly. You can use it to make a upgrade/plugin part.
     */
    public static class RecipeModifier implements IConfigurable, IPersistedSerializable {
        @Configurable(name = "config.part_settings.content_modifier", subConfigurable = true, tips = {"config.part_settings.content_modifier.tooltip"}, collapse = false)
        public final ContentModifier contentModifier = ContentModifier.of(1, 0);
        @Configurable(name = "config.part_settings.target_content", tips = {"config.part_settings.target_content.tooltip"})
        public final IO targetContent = IO.BOTH;
        @Configurable(name = "config.part_settings.duration_modifier", subConfigurable = true, tips = {"config.part_settings.duration_modifier.tooltip"}, collapse = false)
        public final ContentModifier durationModifier = ContentModifier.of(1, 0);
        public final List<RecipeCondition> recipeConditions = new ArrayList<>();

        @Override
        public CompoundTag serializeNBT() {
            var tag = IPersistedSerializable.super.serializeNBT();
            ListTag conditions = new ListTag();
            for (RecipeCondition condition : recipeConditions) {
                CompoundTag conditionTag = new CompoundTag();
                conditionTag.putString("type", MBDRegistries.RECIPE_CONDITIONS.getKey(condition.getClass()));
                conditionTag.put("data", condition.toNBT());
                conditions.add(conditionTag);
            }
            tag.put("recipeConditions", conditions);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            IPersistedSerializable.super.deserializeNBT(tag);
            recipeConditions.clear();
            ListTag conditions = tag.getList("recipeConditions", Tag.TAG_COMPOUND);
            for (int i = 0; i < conditions.size(); i++) {
                CompoundTag conditionTag = conditions.getCompound(i);
                var condition = RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get(conditionTag.getString("type")));
                if (condition != null) {
                    condition.fromNBT(conditionTag.getCompound("data"));
                    recipeConditions.add(condition);
                }
            }
        }

        @Override
        public void buildConfigurator(ConfiguratorGroup father) {
            IConfigurable.super.buildConfigurator(father);
            var conditions = new ArrayConfiguratorGroup<>("config.part_settings.recipe_conditions", false,
                    () -> recipeConditions, (getter, setter) -> new ConfiguratorSelectorConfigurator<>("config.part_settings.recipe_condition.type", false,
                    () -> getter.get().getType(), type -> {
                var current = getter.get();
                var condition = RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get(type));
                if (condition != null) {
                    recipeConditions.set(recipeConditions.indexOf(current), condition);
                }
            }, "rain", true, MBDRegistries.RECIPE_CONDITIONS.registry().keySet().stream().toList(),
                    String::toString, (type, container) -> {
                var current = getter.get();
                current.buildConfigurator(container);
            }), true);
            conditions.setTips("config.part_settings.recipe_conditions.tooltip");
            conditions.setAddDefault(() -> RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get("rain")));
            conditions.setOnAdd(recipeConditions::add);
            conditions.setOnRemove(recipeConditions::remove);
            conditions.setOnUpdate(list -> {
                recipeConditions.clear();
                recipeConditions.addAll(list);
            });
            father.addConfigurators(conditions);
        }
    }

}

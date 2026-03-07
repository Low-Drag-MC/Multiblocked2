package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "machine_custom_data", registry = "mbd2:recipe_condition")
public class MachineNBTCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.machine_custom_data.data", tips="config.recipe.condition.machine_custom_data.data.tips")
    private CompoundTag data = new CompoundTag();
    @Configurable(name = "config.recipe.condition.machine_custom_data.only_check_custom_data",
            tips = {"config.recipe.condition.machine_custom_data.only_check_custom_data.tips.0",
                    "config.recipe.condition.machine_custom_data.only_check_custom_data.tips.1"})
    private boolean onlyCheckCustomData = true;

    public MachineNBTCondition(CompoundTag data, boolean onlyCheckCustomData) {
        this.data = data;
    }

    public MachineNBTCondition(boolean isReverse, CompoundTag data, boolean onlyCheckCustomData) {
        super(isReverse);
        this.data = data;
        this.onlyCheckCustomData = onlyCheckCustomData;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.machine_custom_data.tooltip", this.data);
    }

    @Override
    public IGuiTexture getIcon() {
        return new TextTexture("D");
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        // check if the machine has the same custom data
        if (!data.isEmpty() && recipeLogic.getMachine() instanceof MBDMachine mbdMachine) {
            var machineData = onlyCheckCustomData ? mbdMachine.getCustomData() : mbdMachine.getHolder().saveWithId(mbdMachine.getLevel().registryAccess());
            var copied = machineData.copy();
            copied.merge(this.data);
            return copied.equals(machineData);
        }
        return data.isEmpty();
    }

}

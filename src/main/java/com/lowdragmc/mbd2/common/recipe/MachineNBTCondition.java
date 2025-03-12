package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
public class MachineNBTCondition extends RecipeCondition {
    public static final MapCodec<MachineNBTCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(val -> val.data),
            Codec.BOOL.optionalFieldOf("onlyCheckCustomData", true).forGetter(val -> val.onlyCheckCustomData)
    ).apply(instance, MachineNBTCondition::new));

    public final static MachineNBTCondition INSTANCE = new MachineNBTCondition();
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
    public String getType() {
        return "machine_custom_data";
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
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
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

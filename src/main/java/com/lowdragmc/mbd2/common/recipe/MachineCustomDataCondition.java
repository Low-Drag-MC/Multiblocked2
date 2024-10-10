package com.lowdragmc.mbd2.common.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.NBTToJsonConverter;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nonnull;

@Getter
@NoArgsConstructor
public class MachineCustomDataCondition extends RecipeCondition {

    public final static MachineCustomDataCondition INSTANCE = new MachineCustomDataCondition();
    @Configurable(name = "config.recipe.condition.machine_custom_data.data", tips="config.recipe.condition.machine_custom_data.data.tips")
    private CompoundTag data = new CompoundTag();

    public MachineCustomDataCondition(CompoundTag data) {
        this.data = data;
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
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!data.isEmpty() && recipeLogic.getMachine() instanceof MBDMachine mbdMachine) {
            var machineData = mbdMachine.getCustomData();
            var copied = machineData.copy();
            copied.merge(this.data);
            return copied.equals(machineData);
        }
        return data.isEmpty();
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.add("data", NBTToJsonConverter.getObject(this.data));
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        this.data = CraftingHelper.getNBT(config.getAsJsonObject("data"));
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        data = buf.readNbt();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeNbt(data);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.put("data", data);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        data = tag.getCompound("data");
        return this;
    }

}

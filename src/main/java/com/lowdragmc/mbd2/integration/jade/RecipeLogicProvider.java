package com.lowdragmc.mbd2.integration.jade;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.impl.ui.ElementHelper;
import snownee.jade.impl.ui.ProgressElement;
import snownee.jade.impl.ui.SimpleProgressStyle;

public class RecipeLogicProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig config) {
        if (IMachine.ofMachine(blockAccessor.getBlockEntity()).isEmpty()) return;
        var data = blockAccessor.getServerData();
        if (data.contains("recipe_logic")) {
            appendLogic(tooltip, data.getCompound("recipe_logic"));
        }
        var others = data.getList("recipe_logics", Tag.TAG_COMPOUND);
        for (int i = 0; i < others.size(); i++) {
            appendLogic(tooltip, others.getCompound(i));
        }
    }

    private void appendLogic(ITooltip tooltip, CompoundTag data) {
        var status = Component.translatable("recipe_logic.status." + data.getString("status").toLowerCase());
        tooltip.add(data.contains("address") ? Component.literal(data.getString("address") + ": ").append(status) : status);

        if (data.contains("duration")) {
            var progress = data.getInt("progress");
            var duration = data.getInt("duration");
            tooltip.add(ElementHelper.INSTANCE.progress(progress * 1f / duration,
                    Component.literal("%.2fs / %.2fs".formatted(progress / 20f, duration / 20f)).withStyle(ChatFormatting.WHITE),
                    new SimpleProgressStyle().color(ColorPattern.GREEN.color), BoxStyle.getNestedBox(), true));
        }

        if (data.contains("fuel")) {
            var fuel = data.getInt("fuel");
            var maxFuel = data.getInt("maxFuel");
            tooltip.add(new ProgressElement(fuel * 1f / maxFuel,
                    Component.literal("%.2f / %.2f ".formatted(fuel / 20f, maxFuel / 20f)).withStyle(ChatFormatting.WHITE),
                    new SimpleProgressStyle().color(ColorPattern.ORANGE.color), BoxStyle.getNestedBox(), true));
        }
        if (data.contains("waitingReason")) {
            var reason = Component.Serializer.fromJson(data.getString("waitingReason"), Platform.getFrozenRegistry());
            tooltip.add(reason);
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        IMachine.ofMachine(blockAccessor.getBlockEntity()).ifPresent(machine -> {
            if (machine.runRecipeLogic()) {
                data.put("recipe_logic", logicData(machine, machine.getRecipeLogic()));
            }
            var others = new ListTag();
            for (var logic : machine.getRecipeLogics()) {
                if (logic != machine.getRecipeLogic() && machine.runRecipeLogic(logic)) {
                    var tag = logicData(machine, logic);
                    tag.putString("address", logic.getAddress());
                    others.add(tag);
                }
            }
            if (!others.isEmpty()) {
                data.put("recipe_logics", others);
            }
        });
    }

    private static CompoundTag logicData(IMachine machine, RecipeLogic recipeLogic) {
        var tag = new CompoundTag();
        tag.putString("status", recipeLogic.getStatus().name());
        if (recipeLogic.getDuration() > 0) {
            tag.putInt("progress", recipeLogic.getProgress());
            tag.putInt("duration", recipeLogic.getDuration());
        }
        if (recipeLogic.needFuel()) {
            tag.putInt("fuel", recipeLogic.getFuelTime());
            tag.putInt("maxFuel", recipeLogic.getFuelMaxTime());
        }
        if (recipeLogic.isWaiting() && recipeLogic.getWaitingReason() != null) {
            tag.putString("waitingReason", Component.Serializer.toJson(recipeLogic.getWaitingReason(), machine.getLevel().registryAccess()));
        }
        return tag;
    }

    @Override
    public ResourceLocation getUid() {
        return MBD2.id("recipe_logic_provider");
    }
}

package com.lowdragmc.mbd2.integration.jade;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
            data = data.getCompound("recipe_logic");
            var status = data.getString("status");
            tooltip.add(Component.translatable("recipe_logic.status." + status.toLowerCase()));

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
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        IMachine.ofMachine(blockAccessor.getBlockEntity()).ifPresent(machine -> {
            var tag = new CompoundTag();
            if (!machine.runRecipeLogic()) return;
            var recipeLogic = machine.getRecipeLogic();
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
            data.put("recipe_logic", tag);
        });
    }

    @Override
    public ResourceLocation getUid() {
        return MBD2.id("recipe_logic_provider");
    }
}

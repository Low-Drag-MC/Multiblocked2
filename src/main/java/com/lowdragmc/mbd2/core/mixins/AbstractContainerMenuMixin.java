package com.lowdragmc.mbd2.core.mixins;

import com.lowdragmc.mbd2.common.trait.item.LargeItemHandlerSlot;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow
    @Final
    public NonNullList<Slot> slots;
    @Shadow
    private int quickcraftStatus;

    /**
     * Same conditions as the swap branch of {@code doClick}, where a drag in progress is cancelled first.
     */
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void mbd2$swapLargeSlot(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (clickType == ClickType.SWAP && quickcraftStatus == 0 && (button >= 0 && button < 9 || button == 40)
                && slotId >= 0 && slotId < slots.size()
                && slots.get(slotId) instanceof LargeItemHandlerSlot slot
                && slot.swapWithInventory(player, button)) {
            ci.cancel();
        }
    }
}

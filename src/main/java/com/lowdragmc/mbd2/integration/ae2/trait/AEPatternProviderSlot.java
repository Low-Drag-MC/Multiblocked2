package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignSelf;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Optional;

@LDLRegister(name = "ae-pattern-provider-slot", group = "inventory", registry = "ldlib2:ui_element", modID = "ae2")
public class AEPatternProviderSlot extends UIElement {
    public static SpriteTexture PATTERN_ICON = SpriteTexture.of("ae2:textures/guis/states.png")
            .setSprite(16 * 4, 16 * 5, 16, 16);
    public static SpriteTexture MOVE_DOWN_ICON = SpriteTexture.of("ae2:textures/guis/states.png")
            .setSprite(16 * 1, 16 * 3, 16, 16);


    public final UIElement patternRow = new UIElement();
    public final Label cacheLabel = new Label();
    public final UIElement cacheRow = new UIElement();
    public final Label returnLabel = new Label();
    public final UIElement returnRow = new UIElement();
    public final Button returnButton = new Button();
    @Getter
    @Nullable
    private MEPatternProviderTrait patternProviderTrait;

    public AEPatternProviderSlot() {
        getLayout().flexDirection(FlexDirection.COLUMN).gapAll(2);
        patternRow.setId("pattern_row");
        patternRow.getLayout().flexDirection(FlexDirection.ROW).gapAll(1);
        cacheLabel.setId("cache_label");
        cacheLabel.setText(Component.translatable("mbd2.ae_pattern_provider.cache_row"));
        cacheRow.setId("cache_row");
        cacheRow.getLayout().flexDirection(FlexDirection.ROW).gapAll(1);
        returnLabel.setId("return_label");
        returnLabel.setText(Component.translatable("mbd2.ae_pattern_provider.return_row"));
        returnRow.setId("return_row");
        returnRow.getLayout().flexDirection(FlexDirection.ROW).gapAll(1);
        returnButton.setId("return_button");
        returnButton.getLayout().width(16).height(16).alignSelf(AlignItems.CENTER);
        returnButton.getButtonStyle()
                .baseTexture(MOVE_DOWN_ICON)
                .hoverTexture(MOVE_DOWN_ICON.copy().setColor(0xffDDDDDD))
                .pressedTexture(MOVE_DOWN_ICON.copy().setColor(ColorPattern.LIGHT_GRAY.color));
        returnButton.noText();
        returnButton.getStyle().appendTooltips(
                Component.translatable("mbd2.ae_pattern_provider.return_to_network.tooltip.0"),
                Component.translatable("mbd2.ae_pattern_provider.return_to_network.tooltip.1")
        );
        returnButton.setOnServerClick(event -> returnAllToNetwork());
        addChildren(patternRow, cacheLabel, cacheRow, returnButton, returnLabel, returnRow);
        internalSetup();
    }

    public void setupTemplate(int patternSize, int slotSize) {
        patternRow.clearAllChildren();
        cacheRow.clearAllChildren();
        returnRow.clearAllChildren();
        for (int i = 0; i < patternSize; i++) {
            var slot = new ItemSlot();
            slot.setId("pattern_" + i);
            slot.getStyle().appendTooltips(Component.translatable("mbd2.ae_pattern_provider.pattern_slot.tooltip"));
            slot.getSlotStyle().slotOverlay(PATTERN_ICON);
            patternRow.addChild(slot);
        }
        for (int i = 0; i < slotSize; i++) {
            cacheRow.addChild(createStackColumn("cache", i, "mbd2.ae_pattern_provider.cache_slot.tooltip"));
            returnRow.addChild(createStackColumn("return", i, "mbd2.ae_pattern_provider.return_slot.tooltip"));
        }
    }

    private UIElement createStackColumn(String idPrefix, int index, String tooltipKey) {
        var column = new UIElement();
        column.setId(idPrefix + "_" + index);
        column.getLayout().flexDirection(FlexDirection.COLUMN).gapAll(1);
        var itemSlot = new ItemSlot();
        itemSlot.setId(idPrefix + "_item_" + index);
        itemSlot.getStyle().appendTooltips(Component.translatable(tooltipKey));
        var fluidSlot = new FluidSlot();
        fluidSlot.setId(idPrefix + "_fluid_" + index);
        fluidSlot.getStyle().appendTooltips(Component.translatable(tooltipKey));
        column.addChildren(itemSlot, fluidSlot);
        return column;
    }

    public void bindPatternProvider(MEPatternProviderTrait trait, int patternSize, int slotSize, IO guiIO) {
        this.patternProviderTrait = trait;
        setupTemplate(patternSize, slotSize);
        for (var child : patternRow.getChildren()) {
            if (child instanceof ItemSlot slot) {
                var index = parseIndex(slot.getId());
                if (index >= 0 && index < patternSize) {
                    slot.bind(new ItemHandlerSlot(new PatternItemHandler(trait, index), 0).addChangeListener(trait::onchange));
                }
            }
        }
        bindStackRow(cacheRow, trait.getStorage(), slotSize, guiIO.support(IO.IN), guiIO.support(IO.OUT));
        bindStackRow(returnRow, trait.getReturnInventory(), slotSize, true, true);
    }

    private void bindStackRow(UIElement row, GenericStackInv inventory, int slotSize, boolean canInsert, boolean canExtract) {
        for (var child : row.getChildren()) {
            var index = parseIndex(child.getId());
            if (index < 0 || index >= slotSize) continue;
            var itemSlotIndex = index * 2;
            var fluidSlotIndex = itemSlotIndex + 1;
            for (var slotElement : child.getChildren()) {
                if (slotElement instanceof ItemSlot itemSlot) {
                    var handlerSlot = new ItemHandlerSlot(createAEItemHandler(inventory, itemSlotIndex, canInsert, canExtract), 0);
                    handlerSlot.setCanPlace(stack -> canInsert);
                    handlerSlot.setCanTake(player -> canExtract);
                    itemSlot.bind(handlerSlot.addChangeListener(() -> {
                        if (patternProviderTrait != null) {
                            patternProviderTrait.onchange();
                        }
                    }));
                } else if (slotElement instanceof FluidSlot fluidSlot) {
                    fluidSlot.bind(createAEFluidHandler(inventory, fluidSlotIndex, canInsert, canExtract), 0);
                    fluidSlot.setAllowClickDrained(canInsert);
                    fluidSlot.setAllowClickFilled(canExtract);
                }
            }
        }
    }

    private int parseIndex(String id) {
        var lastUnderscore = id.lastIndexOf('_');
        if (lastUnderscore < 0) return -1;
        try {
            return Integer.parseInt(id.substring(lastUnderscore + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void returnAllToNetwork() {
        if (patternProviderTrait != null) {
            patternProviderTrait.returnAllToNetwork();
        }
    }

    public static @NotNull IItemHandlerModifiable createAEItemHandler(GenericStackInv inventory, int slotIndex, boolean canInsert, boolean canExtract) {
        return new IItemHandlerModifiable() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return Optional.ofNullable(inventory.getStack(slotIndex)).map(stack -> {
                    if (stack.what() instanceof AEItemKey itemKey) {
                        return itemKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()));
                    }
                    return ItemStack.EMPTY;
                }).orElse(ItemStack.EMPTY);
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                if (slot != 0) return;
                if (stack.isEmpty()) {
                    inventory.setStack(slotIndex, null);
                    return;
                }
                var itemKey = AEItemKey.of(stack);
                if (itemKey != null) {
                    inventory.setStack(slotIndex, new GenericStack(itemKey, stack.getCount()));
                }
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (slot != 0 || stack.isEmpty() || !canInsert || !inventory.canInsert()) return stack;
                var itemKey = AEItemKey.of(stack);
                if (itemKey == null) return stack;
                var inserted = inventory.insert(slotIndex, itemKey, stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return stack.copyWithCount((int) Math.max(0, stack.getCount() - inserted));
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 0 || amount <= 0 || !canExtract || !inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEItemKey itemKey)) {
                    return ItemStack.EMPTY;
                }
                var extracted = inventory.extract(slotIndex, itemKey, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return itemKey.toStack((int) Math.min(Integer.MAX_VALUE, extracted));
            }

            @Override
            public int getSlotLimit(int slot) {
                return (int) Math.min(Integer.MAX_VALUE, inventory.getCapacity(AEKeyType.items()));
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                var itemKey = AEItemKey.of(stack);
                return slot == 0 && canInsert && itemKey != null && inventory.isAllowedIn(slotIndex, itemKey);
            }
        };
    }

    public static @NotNull IFluidHandler createAEFluidHandler(GenericStackInv inventory, int slotIndex, boolean canInsert, boolean canExtract) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                if (tank != 0) return FluidStack.EMPTY;
                return Optional.ofNullable(inventory.getStack(slotIndex)).map(stack -> {
                    if (stack.what() instanceof AEFluidKey fluidKey) {
                        return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()));
                    }
                    return FluidStack.EMPTY;
                }).orElse(FluidStack.EMPTY);
            }

            @Override
            public int getTankCapacity(int tank) {
                return (int) Math.min(Integer.MAX_VALUE, inventory.getCapacity(AEKeyType.fluids()));
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                var fluidKey = AEFluidKey.of(stack);
                return tank == 0 && canInsert && fluidKey != null && inventory.isAllowedIn(slotIndex, fluidKey);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !canInsert || !inventory.canInsert()) return 0;
                var fluidKey = AEFluidKey.of(resource);
                if (fluidKey == null) return 0;
                var inserted = inventory.insert(slotIndex, fluidKey, resource.getAmount(), action.execute() ? Actionable.MODULATE : Actionable.SIMULATE);
                return (int) Math.min(Integer.MAX_VALUE, inserted);
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !canExtract || !inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEFluidKey fluidKey) || !fluidKey.matches(resource)) {
                    return FluidStack.EMPTY;
                }
                var drained = inventory.extract(slotIndex, fluidKey, resource.getAmount(), action.execute() ? Actionable.MODULATE : Actionable.SIMULATE);
                return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, drained));
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (maxDrain <= 0 || !canExtract || !inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEFluidKey fluidKey)) {
                    return FluidStack.EMPTY;
                }
                var drained = inventory.extract(slotIndex, fluidKey, maxDrain, action.execute() ? Actionable.MODULATE : Actionable.SIMULATE);
                return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, drained));
            }
        };
    }

    private static class PatternItemHandler implements IItemHandlerModifiable {
        private final MEPatternProviderTrait trait;
        private final int patternIndex;

        private PatternItemHandler(MEPatternProviderTrait trait, int patternIndex) {
            this.trait = trait;
            this.patternIndex = patternIndex;
        }

        private InternalInventory inventory() {
            return trait.getPatternProviderLogic().getPatternInv();
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot != 0) return;
            if (stack.isEmpty()) {
                inventory().setItemDirect(patternIndex, ItemStack.EMPTY);
            } else if (isItemValid(slot, stack)) {
                inventory().setItemDirect(patternIndex, stack.copyWithCount(1));
            }
            trait.getPatternProviderLogic().updatePatterns();
            trait.onchange();
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inventory().getStackInSlot(patternIndex) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !getStackInSlot(slot).isEmpty() || !isItemValid(slot, stack)) {
                return stack;
            }
            if (!simulate) {
                setStackInSlot(slot, stack.copyWithCount(1));
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
            var stored = getStackInSlot(slot);
            if (stored.isEmpty()) return ItemStack.EMPTY;
            var extracted = stored.copyWithCount(1);
            if (!simulate) {
                setStackInSlot(slot, ItemStack.EMPTY);
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot != 0 || stack.isEmpty()) return false;
            var level = trait.getBlockEntity().getLevel();
            var details = level == null ? null : PatternDetailsHelper.decodePattern(stack.copyWithCount(1), level);
            return details != null && details.supportsPushInputsToExternalInventory();
        }
    }
}

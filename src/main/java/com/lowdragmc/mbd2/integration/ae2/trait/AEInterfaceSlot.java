package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigInventory;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@LDLRegister(name = "ae-interface-slot", group = "inventory", registry = "ldlib2:ui_element", modID = "ae2")
public class AEInterfaceSlot extends UIElement {
    private static final int MIN_ITEM_AMOUNT = 1;
    private static final int MAX_ITEM_AMOUNT = 64;
    private static final int ITEM_SCROLL_STEP = 1;
    private static final int ITEM_SHIFT_SCROLL_STEP = 10;
    private static final int MIN_FLUID_AMOUNT = 1;
    private static final int MAX_FLUID_AMOUNT = 4000;
    private static final int FLUID_SCROLL_STEP = 1;
    private static final int FLUID_SHIFT_SCROLL_STEP = 1000;

    public final ItemSlot phantomSlot = new ItemSlot().xeiPhantom();
    public final ItemSlot slot = new ItemSlot();
    public final FluidSlot phantomTank = new FluidSlot().xeiPhantom();
    public final FluidSlot tank = new FluidSlot();
    @Getter
    @Nullable
    private SerializableInterfaceLogic interfaceLogic;
    @Getter
    private int slotIndex;

    public AEInterfaceSlot() {
        phantomSlot.setId("phantom_slot");
        phantomSlot.getSlotStyle().slotOverlay(Icons.DOWN_ARROW_NO_BAR.copy().scale(0.7f)).showSlotOverlayOnlyEmpty(true);
        slot.setId("slot");
        phantomTank.setId("phantom_tank");
        phantomTank.getSlotStyle().slotOverlay(Icons.DOWN_ARROW_NO_BAR.copy().scale(0.7f)).showSlotOverlayOnlyEmpty(true);
        tank.setId("tank");
        addChild(phantomSlot);
        addChild(slot);
        addChild(phantomTank);
        addChild(tank);
        setupPhantomControls();
        internalSetup();
    }

    public void setItemInterfaceLogic(SerializableInterfaceLogic interfaceLogic, int slotIndex) {
        this.interfaceLogic = interfaceLogic;
        this.slotIndex = slotIndex;
        slot.bind(createAEItemHandler(interfaceLogic.getStorage(), slotIndex * 2), 0);
        phantomSlot.bind(DataBindingBuilder.itemStack(
                () -> Optional.ofNullable(interfaceLogic.getConfig().getStack(slotIndex * 2)).map(stack -> {
                    if (stack.what() instanceof AEItemKey itemKey) {
                        return itemKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()));
                    }
                    return ItemStack.EMPTY;
                }).orElse(ItemStack.EMPTY), itemStack -> {
                    if (itemStack.isEmpty()) {
                        interfaceLogic.getConfig().setStack(slotIndex * 2, null);
                        return;
                    }
                    var itemKey = AEItemKey.of(itemStack);
                    if (itemKey != null) {
                        interfaceLogic.getConfig().setStack(slotIndex * 2, new GenericStack(itemKey, itemStack.getCount()));
                    }
                }).build());
        tank.bind(createAEFluidHandler(interfaceLogic.getStorage(), slotIndex * 2 + 1), 0);
        phantomTank.bind(DataBindingBuilder.fluidStack(
                () -> Optional.ofNullable(interfaceLogic.getConfig().getStack(slotIndex * 2 + 1)).map(stack -> {
                    if (stack.what() instanceof AEFluidKey fluidKey) {
                        return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()));
                    }
                    return FluidStack.EMPTY;
                }).orElse(FluidStack.EMPTY), fluidStack -> {
                    if (fluidStack.isEmpty()) {
                        interfaceLogic.getConfig().setStack(slotIndex * 2 + 1, null);
                        return;
                    }
                    var fluidKey = AEFluidKey.of(fluidStack);
                    if (fluidKey != null) {
                        interfaceLogic.getConfig().setStack(slotIndex * 2 + 1, new GenericStack(fluidKey, fluidStack.getAmount()));
                    }
                }).build());
    }

    public void setStorage(ConfigInventory storage, int slotIndex) {
        this.interfaceLogic = null;
        this.slotIndex = slotIndex;
        phantomSlot.setDisplay(false);
        phantomTank.setDisplay(false);
        slot.bind(createAEItemHandler(storage, slotIndex * 2), 0);
        tank.bind(createAEFluidHandler(storage, slotIndex * 2 + 1), 0);
    }

    public void setIngredientIO(IO io) {
        switch (io) {
            case IN -> {
                slot.xeiRecipeIngredient(IngredientIO.INPUT);
                tank.xeiRecipeIngredient(IngredientIO.INPUT);
            }
            case OUT -> {
                slot.xeiRecipeIngredient(IngredientIO.OUTPUT);
                tank.xeiRecipeIngredient(IngredientIO.OUTPUT);
            }
            case BOTH -> {
                slot.xeiRecipeIngredient(IngredientIO.INPUT);
                slot.xeiRecipeIngredient(IngredientIO.OUTPUT);
                tank.xeiRecipeIngredient(IngredientIO.INPUT);
                tank.xeiRecipeIngredient(IngredientIO.OUTPUT);
            }
        }
    }

    public void setCanTakeItems(boolean support) {
        if (slot.getSlot() instanceof ItemHandlerSlot handlerSlot) {
            handlerSlot.setCanTake(player -> support);
        }
        tank.setAllowClickFilled(support);
    }

    public void setCanPutItems(boolean support) {
        if (slot.getSlot() instanceof ItemHandlerSlot handlerSlot) {
            handlerSlot.setCanPlace(stack -> support);
        }
        tank.setAllowClickDrained(support);
    }

    private void setupPhantomControls() {
        phantomSlot.getStyle().appendTooltips(
                Component.translatable("mbd2.ae_interface_slot.phantom_slot.tooltip.click"),
                Component.translatable("mbd2.ae_interface_slot.phantom_slot.tooltip.scroll"),
                Component.translatable("mbd2.ae_interface_slot.phantom_slot.tooltip.xei"));
        phantomTank.getStyle().appendTooltips(
                Component.translatable("mbd2.ae_interface_slot.phantom_tank.tooltip.click"),
                Component.translatable("mbd2.ae_interface_slot.phantom_tank.tooltip.scroll"),
                Component.translatable("mbd2.ae_interface_slot.phantom_tank.tooltip.xei"));

        if (!LDLib2.isClient() || LDLib2.isServer()) return;
        phantomSlot.addEventListener(UIEvents.MOUSE_DOWN, this::onPhantomSlotClicked);
        phantomSlot.addEventListener(UIEvents.MOUSE_WHEEL, this::onPhantomSlotScrolled);
        phantomTank.addEventListener(UIEvents.MOUSE_DOWN, this::onPhantomTankClicked);
        phantomTank.addEventListener(UIEvents.MOUSE_WHEEL, this::onPhantomTankScrolled);
    }

    private void onPhantomSlotClicked(UIEvent event) {
        var carried = getCarriedItem();
        if (carried.isEmpty()) {
            phantomSlot.setItem(ItemStack.EMPTY);
        } else {
            phantomSlot.setItem(carried.copyWithCount(Mth.clamp(carried.getCount(), MIN_ITEM_AMOUNT, MAX_ITEM_AMOUNT)));
        }
        event.stopPropagation();
    }

    private void onPhantomSlotScrolled(UIEvent event) {
        var scrollDirection = getScrollDirection(event);
        var current = phantomSlot.getValue();
        if (scrollDirection == 0 || current.isEmpty()) return;
        var step = event.isShiftDown() ? ITEM_SHIFT_SCROLL_STEP : ITEM_SCROLL_STEP;
        phantomSlot.setItem(current.copyWithCount(Mth.clamp(current.getCount() + scrollDirection * step, MIN_ITEM_AMOUNT, MAX_ITEM_AMOUNT)));
        event.stopPropagation();
    }

    private void onPhantomTankClicked(UIEvent event) {
        phantomTank.setFluid(getCarriedFluid());
        event.stopPropagation();
    }

    private void onPhantomTankScrolled(UIEvent event) {
        var scrollDirection = getScrollDirection(event);
        var current = phantomTank.getValue();
        if (scrollDirection == 0 || current.isEmpty()) return;
        var step = event.isShiftDown() ? FLUID_SHIFT_SCROLL_STEP : FLUID_SCROLL_STEP;
        phantomTank.setFluid(current.copyWithAmount(Mth.clamp(current.getAmount() + scrollDirection * step, MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT)));
        event.stopPropagation();
    }

    private ItemStack getCarriedItem() {
        var mui = getModularUI();
        if (mui == null || mui.getMenu() == null) return ItemStack.EMPTY;
        return mui.getMenu().getCarried();
    }

    private FluidStack getCarriedFluid() {
        var carried = getCarriedItem();
        if (carried.isEmpty()) return FluidStack.EMPTY;
        return FluidUtil.getFluidHandler(carried)
                .flatMap(handler -> {
                    for (int i = 0; i < handler.getTanks(); i++) {
                        var fluid = handler.getFluidInTank(i);
                        if (!fluid.isEmpty()) {
                            return Optional.of(fluid.copyWithAmount(Mth.clamp(fluid.getAmount(), MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT)));
                        }
                    }
                    return Optional.empty();
                }).orElse(FluidStack.EMPTY);
    }

    private static int getScrollDirection(UIEvent event) {
        if (event.deltaY > 0) return 1;
        if (event.deltaY < 0) return -1;
        return 0;
    }

    public static @NotNull IItemHandlerModifiable createAEItemHandler(GenericStackInv inventory, int slotIndex) {
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
                if (slot != 0 || stack.isEmpty() || !inventory.canInsert()) return stack;
                var itemKey = AEItemKey.of(stack);
                if (itemKey == null) return stack;
                var inserted = inventory.insert(slotIndex, itemKey, stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return stack.copyWithCount((int) Math.max(0, stack.getCount() - inserted));
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 0 || amount <= 0 || !inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEItemKey itemKey)) {
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
                return slot == 0 && itemKey != null && inventory.isAllowedIn(slotIndex, itemKey);
            }
        };
    }

    public static @NotNull IFluidHandler createAEFluidHandler(GenericStackInv inventory, int slotIndex) {
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
                return tank == 0 && fluidKey != null && inventory.isAllowedIn(slotIndex, fluidKey);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !inventory.canInsert()) return 0;
                var fluidKey = AEFluidKey.of(resource);
                if (fluidKey == null) return 0;
                var inserted = inventory.insert(slotIndex, fluidKey, resource.getAmount(), action.execute() ? Actionable.MODULATE : Actionable.SIMULATE);
                return (int) Math.min(Integer.MAX_VALUE, inserted);
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEFluidKey fluidKey) || !fluidKey.matches(resource)) {
                    return FluidStack.EMPTY;
                }
                var drained = inventory.extract(slotIndex, fluidKey, resource.getAmount(), action.execute() ? Actionable.MODULATE : Actionable.SIMULATE);
                return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, drained));
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (maxDrain <= 0 || !inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEFluidKey fluidKey)) {
                    return FluidStack.EMPTY;
                }
                var drained = inventory.extract(slotIndex, fluidKey, maxDrain, action.execute() ? Actionable.MODULATE : Actionable.SIMULATE);
                return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, drained));
            }
        };
    }

}

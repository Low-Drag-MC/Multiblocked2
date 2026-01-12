package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.lowdragmc.lowdraglib.gui.widget.SlotWidget.ITEM_SLOT_TEXTURE;
import static com.lowdragmc.lowdraglib.gui.widget.TankWidget.FLUID_SLOT_TEXTURE;


@LDLRegister(name = "ae_interface_slot", group = "widget.container", modID = "ae2")
public class AEInterfaceSlotWidget extends WidgetGroup {
    private PhantomSlotWidget phantomSlot = new PhantomSlotWidget();
    private SlotWidget slot = new SlotWidget();
    private PhantomTankWidget phantomTank = new PhantomTankWidget();
    private TankWidget tank = new TankWidget();
    @Getter @Nullable
    private SerializableInterfaceLogic interfaceLogic;
    @Getter
    private int slotIndex;

    public AEInterfaceSlotWidget() {
        super(0, 0, 20, 18 * 4 + 2);
        phantomSlot.setSelfPosition(1, 1);
        slot.setSelfPosition(1, 19);
        phantomSlot.setId("phantom_slot");
        slot.setId("slot");
        phantomTank.setSelfPosition(1, 1 + 36);
        tank.setSelfPosition(1, 1 + 18 * 3);
        phantomTank.setId("phantom_tank");
        tank.setId("tank");
        addWidget(phantomSlot);
        addWidget(slot);
        addWidget(phantomTank);
        addWidget(tank);
    }

    @Override
    public void initTemplate() {
        phantomSlot.initTemplate();
        slot.initTemplate();
        phantomSlot.setBackgroundTexture(new GuiTextureGroup(ITEM_SLOT_TEXTURE, Icons.DOWN.copy().setColor(ColorPattern.GRAY.color).scale(0.8f)));
        phantomTank.setBackground(new GuiTextureGroup(FLUID_SLOT_TEXTURE, Icons.DOWN.copy().setColor(ColorPattern.GRAY.color).scale(0.8f)));
    }

    public void setItemInterfaceLogic(SerializableInterfaceLogic interfaceLogic, int slotIndex) {
        this.interfaceLogic = interfaceLogic;
        this.slotIndex = slotIndex;
        slot.setHandlerSlot(createAEItemTransfer(interfaceLogic.getStorage(), slotIndex * 2), 0);
        phantomSlot.setHandlerSlot(createAEItemTransfer(interfaceLogic.getConfig(), slotIndex * 2), 0);
        tank.setFluidTank(createAEFluidTransfer(interfaceLogic.getStorage(), slotIndex * 2 + 1), 0);
        phantomTank.setFluidTank(createAEFluidTransfer(interfaceLogic.getConfig(), slotIndex * 2 + 1), 0);
    }


    public void setIngredientIO(IngredientIO ingredientIO) {
        slot.setIngredientIO(ingredientIO);
        tank.setIngredientIO(ingredientIO);
    }

    public void setCanTakeItems(boolean support) {
        slot.setCanTakeItems(support);
        tank.setAllowClickFilled(support);
    }

    public void setCanPutItems(boolean support) {
        slot.setCanPutItems(support);
        tank.setAllowClickDrained(support);
    }

    public static @NotNull IItemTransfer createAEItemTransfer(ConfigInventory inventory, int slotIndex) {
        return new IItemTransfer() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return Optional.ofNullable(inventory.getStack(slotIndex)).map(stack -> {
                    if (stack.what() instanceof AEItemKey itemKey) {
                        return new ItemStack(itemKey.getItem(), (int) stack.amount());
                    }
                    return ItemStack.EMPTY;
                }).orElse(ItemStack.EMPTY);
            }

            @Override
            public void setStackInSlot(int index, ItemStack stack) {
                if (index != 0) return;
                if (stack.isEmpty()) {
                    inventory.setStack(slotIndex, null);
                } else {
                    inventory.setStack(slotIndex, new GenericStack(AEItemKey.of(stack), stack.getCount()));
                }
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges) {
                if (!inventory.canInsert()) return stack;
                var inserted = inventory.insert(slotIndex, AEItemKey.of(stack), stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return stack.copyWithCount((int) (stack.getCount() - inserted));
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate, boolean notifyChanges) {
                if (!inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEItemKey itemKey) || itemKey.getItem() == Items.AIR)
                    return ItemStack.EMPTY;
                var extracted = inventory.extract(slotIndex, itemKey, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return new ItemStack(itemKey.getItem(), (int) extracted);
            }

            @Override
            public int getSlotLimit(int slot) {
                return (int) inventory.getCapacity(AEKeyType.items());
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return inventory.isAllowed(new GenericStack(AEItemKey.of(stack), stack.getCount()));
            }

            @Override
            public @NotNull Object createSnapshot() {
                return new Object();
            }

            @Override
            public void restoreFromSnapshot(Object snapshot) {

            }
        };
    }

    public static @NotNull IFluidTransfer createAEFluidTransfer(ConfigInventory inventory, int slotIndex) {
        return new IFluidTransfer() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                return Optional.ofNullable(inventory.getStack(slotIndex)).map(stack -> {
                    if (stack.what() instanceof AEFluidKey fluidKey) {
                        return FluidStack.create(fluidKey.getFluid(), stack.amount(), fluidKey.copyTag());
                    }
                    return FluidStack.empty();
                }).orElse(FluidStack.empty());
            }

            @Override
            public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
                if (tank != 0) return;
                if (fluidStack.isEmpty()) {
                    inventory.setStack(slotIndex, null);
                } else {
                    inventory.setStack(slotIndex, new GenericStack(AEFluidKey.of(FluidHelperImpl.toFluidStack(fluidStack)), fluidStack.getAmount()));
                }
            }

            @Override
            public long getTankCapacity(int tank) {
                return inventory.getCapacity(AEKeyType.fluids());
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                return inventory.isAllowed(new GenericStack(AEFluidKey.of(FluidHelperImpl.toFluidStack(stack)), stack.getAmount()));
            }

            @Override
            public long fill(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
                if (!inventory.canInsert()) return 0;
                return inventory.insert(slotIndex, AEFluidKey.of(FluidHelperImpl.toFluidStack(resource)), resource.getAmount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            }

            @Override
            public boolean supportsFill(int tank) {
                return inventory.canInsert();
            }

            @Override
            public @NotNull FluidStack drain(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
                if (!inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEFluidKey fluidKey) || FluidHelperImpl.toFluidStack(resource).isEmpty() || !fluidKey.getFluid().isSame(FluidHelperImpl.toFluidStack(resource).getFluid()))
                    return FluidStack.empty();
                var drained = inventory.extract(slotIndex, fluidKey, resource.getAmount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return FluidStack.create(fluidKey.getFluid(), drained, fluidKey.copyTag());
            }

            @Override
            public @NotNull FluidStack drain(long maxDrain, boolean simulate, boolean notifyChanges) {
                if (!inventory.canExtract() || !(inventory.getKey(slotIndex) instanceof AEFluidKey fluidKey))
                    return FluidStack.empty();
                var extracted = inventory.extract(slotIndex, fluidKey, maxDrain, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return FluidStack.create(fluidKey.getFluid(), extracted, fluidKey.copyTag());
            }

            @Override
            public boolean supportsDrain(int tank) {
                return inventory.canExtract();
            }

            @Override
            public @NotNull Object createSnapshot() {
                return new Object();
            }

            @Override
            public void restoreFromSnapshot(Object snapshot) {

            }
        };
    }

    @Override
    public void deserializeInnerNBT(CompoundTag nbt) {
        super.deserializeInnerNBT(nbt);
        for (Widget widget : widgets) {
            if (widget instanceof PhantomSlotWidget phantomSlotWidget && phantomSlotWidget.getId().equals("phantom_slot")) {
                this.phantomSlot = phantomSlotWidget;
            } else if (widget instanceof SlotWidget slotWidget && slotWidget.getId().equals("slot")) {
                this.slot = slotWidget;
            } else if (widget instanceof PhantomTankWidget phantomTankWidget && phantomTankWidget.getId().equals("phantom_tank")) {
                this.phantomTank = phantomTankWidget;
            } else if (widget instanceof TankWidget tankWidget && tankWidget.getId().equals("tank")) {
                this.tank = tankWidget;
            }
        }
    }

}

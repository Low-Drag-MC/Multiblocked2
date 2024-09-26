package com.lowdragmc.mbd2.integration.mekanism;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.ClickableIngredient;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import mekanism.api.Action;
import mekanism.api.chemical.*;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.api.math.MathUtils;
import mekanism.client.gui.GuiUtils;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

@Accessors(chain = true)
public abstract class ChemicalTankWidget<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends Widget implements IRecipeIngredientSlot, IConfigurableWidget {
    public final static ResourceBorderTexture FLUID_SLOT_TEXTURE = new ResourceBorderTexture("ldlib:textures/gui/fluid_slot.png", 18, 18, 1, 1);

    @Nullable
    @Getter
    protected IChemicalHandler<CHEMICAL, STACK> chemicalHandler;
    @Getter
    protected int tank;
    @Configurable(name = "ldlib.gui.editor.name.showAmount")
    @Setter
    protected boolean showAmount;
    @Configurable(name = "ldlib.gui.editor.name.allowClickFilled")
    @Setter
    protected boolean allowClickFilled;
    @Configurable(name = "ldlib.gui.editor.name.allowClickDrained")
    @Setter
    protected boolean allowClickDrained;
    @Configurable(name = "ldlib.gui.editor.name.drawHoverOverlay")
    @Setter
    public boolean drawHoverOverlay = true;
    @Configurable(name = "ldlib.gui.editor.name.drawHoverTips")
    @Setter
    protected boolean drawHoverTips;
    @Configurable(name = "ldlib.gui.editor.name.fillDirection")
    @Setter
    protected ProgressTexture.FillDirection fillDirection = ProgressTexture.FillDirection.ALWAYS_FULL;
    @Setter
    protected BiConsumer<ChemicalTankWidget<CHEMICAL, STACK>, List<Component>> onAddedTooltips;
    @Setter @Getter
    protected IngredientIO ingredientIO = IngredientIO.RENDER_ONLY;
    @Setter @Getter
    protected float XEIChance = 1f;
    protected ChemicalStack<CHEMICAL> lastChemicalInTank;
    protected long lastTankCapacity;
    @Setter
    protected Runnable changeListener;

    public ChemicalTankWidget() {
        this(null, 0, 0, true, true);
    }

    public ChemicalTankWidget(@Nullable IChemicalHandler<CHEMICAL, STACK> chemicalHandler, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        this(chemicalHandler, x, y, 18, 18, allowClickContainerFilling, allowClickContainerEmptying);
    }

    public ChemicalTankWidget(@Nullable IChemicalHandler<CHEMICAL, STACK> chemicalHandler, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        this(chemicalHandler, 0, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
    }

    public ChemicalTankWidget(@Nullable IChemicalHandler<CHEMICAL, STACK> chemicalHandler, int tank, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        this(chemicalHandler, tank, x, y, 18, 18, allowClickContainerFilling, allowClickContainerEmptying);
    }

    public ChemicalTankWidget(@Nullable IChemicalHandler<CHEMICAL, STACK> chemicalHandler, int tank, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        super(new Position(x, y), new Size(width, height));
        this.chemicalHandler = chemicalHandler;
        this.tank = tank;
        this.showAmount = true;
        this.allowClickFilled = allowClickContainerFilling;
        this.allowClickDrained = allowClickContainerEmptying;
        this.drawHoverTips = true;
    }

    @Override
    public void initTemplate() {
        setBackground(FLUID_SLOT_TEXTURE);
        setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);
    }

    public abstract Capability<? extends IChemicalHandler<CHEMICAL, STACK>> getCapability();
    public abstract ChemicalStack<CHEMICAL> readStack(CompoundTag tag);

    public ChemicalTankWidget<CHEMICAL, STACK> setChemicalTank(IChemicalHandler<CHEMICAL, STACK> chemicalHandler) {
        this.chemicalHandler = chemicalHandler;
        this.tank = 0;
        if (isClientSideWidget) {
            setClientSideWidget();
        }
        return this;
    }

    public ChemicalTankWidget<CHEMICAL, STACK> setChemicalTank(IChemicalHandler<CHEMICAL, STACK> chemicalHandler, int tank) {
        this.chemicalHandler = chemicalHandler;
        this.tank = tank;
        if (isClientSideWidget) {
            setClientSideWidget();
        }
        return this;
    }

    @Override
    public ChemicalTankWidget<CHEMICAL, STACK> setClientSideWidget() {
        super.setClientSideWidget();
        if (chemicalHandler != null) {
            this.lastChemicalInTank = chemicalHandler.getChemicalInTank(tank).copy();
        } else {
            this.lastChemicalInTank = null;
        }
        this.lastTankCapacity = chemicalHandler != null ? chemicalHandler.getTankCapacity(tank) : 0;
        return this;
    }

    public ChemicalTankWidget<CHEMICAL, STACK> setBackground(IGuiTexture background) {
        super.setBackground(background);
        return this;
    }

    @Override
    public List<Object> getXEIIngredients() {
        if (lastChemicalInTank == null || lastChemicalInTank.isEmpty()) return Collections.emptyList();
        if (LDLib.isModLoaded(LDLib.MODID_JEI)) {
            var pos = getPosition();
            var size = getSize();
            var jeiIngredient = JEIPlugin.jeiHelpers.getIngredientManager().createTypedIngredient(lastChemicalInTank)
                    .map(typedIngredient -> new ClickableIngredient<>(typedIngredient, pos.x, pos.y, size.width, size.height));
            if (jeiIngredient.isPresent()) {
                return List.of(jeiIngredient.get());
            }
        }
//        if (LDLib.isReiLoaded()) {
//            return List.of(EntryStacks.of(dev.architectury.fluid.FluidStack.create(lastChemicalInTank.getFluid(), lastChemicalInTank.getAmount(), lastChemicalInTank.getTag())));
//        }
//        if (LDLib.isEmiLoaded()) {
//            return List.of(EmiStack.of(lastChemicalInTank.getFluid(), lastChemicalInTank.getTag(), lastChemicalInTank.getAmount()).setChance(XEIChance));
//        }
        return List.of(lastChemicalInTank);
    }

    @Override
    public List<Component> getTooltipTexts() {
        List<Component> tooltips = getAdditionalToolTips(new ArrayList<>());
        tooltips.addAll(tooltipTexts);
        return tooltips;
    }

    public List<Component> getAdditionalToolTips(List<Component> list) {
        if (this.onAddedTooltips != null) {
            this.onAddedTooltips.accept(this, list);
        }
        return list;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (isClientSideWidget && chemicalHandler != null) {
            var chemicalStack = chemicalHandler.getChemicalInTank(tank);
            long capacity = chemicalHandler.getTankCapacity(tank);
            if (capacity != lastTankCapacity) {
                this.lastTankCapacity = capacity;
            }
            if (!chemicalStack.isStackIdentical(lastChemicalInTank)) {
                this.lastChemicalInTank = chemicalStack.copy();
            } else if (chemicalStack.getAmount() != lastChemicalInTank.getAmount()) {
                this.lastChemicalInTank.setAmount(chemicalStack.getAmount());
            }
        }
        Position pos = getPosition();
        Size size = getSize();
        if (lastChemicalInTank != null) {
            RenderSystem.disableBlend();
            if (!lastChemicalInTank.isEmpty()) {
                double progress = lastChemicalInTank.getAmount() * 1.0 / Math.max(Math.max(lastChemicalInTank.getAmount(), lastTankCapacity), 1);
                float drawnU = (float) fillDirection.getDrawnU(progress);
                float drawnV = (float) fillDirection.getDrawnV(progress);
                float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
                float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
                int width = size.width - 2;
                int height = size.height - 2;
                int x = pos.x + 1;
                int y = pos.y + 1;
                drawChemical(graphics, (int) (x + drawnU * width), (int) (y + drawnV * height), ((int) (width * drawnWidth)), ((int) (height * drawnHeight)), lastChemicalInTank);
            }

            if (showAmount && !lastChemicalInTank.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1);
                String s = TextFormattingUtil.formatLongToCompactStringBuckets(lastChemicalInTank.getAmount(), 3) + "B";
                var fontRenderer = Minecraft.getInstance().font;
                graphics.drawString(fontRenderer, s, (int) ((pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 21), (int) ((pos.y + (size.height / 3f) + 6) * 2), 0xFFFFFF, true);
                graphics.pose().popPose();
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (drawHoverOverlay && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(graphics, getPosition().x + 1, getPosition().y + 1, getSize().width - 2, getSize().height - 2, 0x80FFFFFF);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static <CHEMICAL extends Chemical<CHEMICAL>> void drawChemical(GuiGraphics graphics, int xPosition, int yPosition, int width, int height, @Nonnull ChemicalStack<CHEMICAL> stack) {
        int desiredHeight = MathUtils.clampToInt(height);
        if (desiredHeight < 1) {
            desiredHeight = 1;
        }

        if (desiredHeight > height) {
            desiredHeight = height;
        }

        Chemical<?> chemical = stack.getType();
        MekanismRenderer.color(graphics, chemical);
        GuiUtils.drawTiledSprite(graphics, xPosition, yPosition, height, width, desiredHeight, MekanismRenderer.getSprite(chemical.getIcon()), 16, 16, 100, GuiUtils.TilingDirection.UP_RIGHT);
        MekanismRenderer.resetColor(graphics);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (drawHoverTips && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this) {
            List<Component> tooltips = new ArrayList<>();
            if (lastChemicalInTank != null && !lastChemicalInTank.isEmpty()) {
                tooltips.add(lastChemicalInTank.getTextComponent());
                tooltips.add(Component.translatable("recipe.capability.mek_chemical.type.format", LocalizationUtils.format("recipe.capability.mek_chemical.type." + ChemicalType.getTypeFor(lastChemicalInTank.getType()).getSerializedName())));
                tooltips.add(Component.translatable("ldlib.fluid.amount", lastChemicalInTank.getAmount(), lastTankCapacity).append(" mB"));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.empty"));
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, lastTankCapacity).append(" mB"));
            }
            if (gui != null) {
                tooltips.addAll(getTooltipTexts());
                gui.getModularUIGui().setHoverTooltip(tooltips, ItemStack.EMPTY, null, null);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1f);
        } else {
            super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (chemicalHandler != null) {
            var chemicalStack = chemicalHandler.getChemicalInTank(tank);
            long capacity = chemicalHandler.getTankCapacity(tank);
            if (capacity != lastTankCapacity) {
                this.lastTankCapacity = capacity;
                writeUpdateInfo(0, buffer -> buffer.writeVarLong(lastTankCapacity));
            }
            if (!chemicalStack.isStackIdentical(lastChemicalInTank)) {
                this.lastChemicalInTank = chemicalStack.copy();
                var chemicalStackTag = chemicalStack.write(new CompoundTag());
                writeUpdateInfo(2, buffer -> buffer.writeNbt(chemicalStackTag));
            } else if (chemicalStack.getAmount() != lastChemicalInTank.getAmount()) {
                this.lastChemicalInTank.setAmount(chemicalStack.getAmount());
                writeUpdateInfo(3, buffer -> buffer.writeVarLong(lastChemicalInTank.getAmount()));
            } else {
                super.detectAndSendChanges();
                return;
            }
            if (changeListener != null) {
                changeListener.run();
            }
        }
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(chemicalHandler != null);
        if (chemicalHandler != null) {
            this.lastTankCapacity = chemicalHandler.getTankCapacity(tank);
            buffer.writeVarLong(lastTankCapacity);
            var chemicalStack = chemicalHandler.getChemicalInTank(tank);
            this.lastChemicalInTank = chemicalStack.copy();
            buffer.writeNbt(chemicalStack.write(new CompoundTag()));
        }
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            this.lastTankCapacity = buffer.readVarLong();
            readUpdateInfo(2, buffer);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == 0) {
            this.lastTankCapacity = buffer.readVarLong();
        } else if (id == 1) {
            this.lastChemicalInTank = null;
        } else if (id == 2) {
            this.lastChemicalInTank = readStack(buffer.readNbt());
        } else if (id == 3 && lastChemicalInTank != null) {
            this.lastChemicalInTank.setAmount(buffer.readVarLong());
        } else if (id == 4) {
            ItemStack currentStack = gui.getModularUIContainer().getCarried();
            int newStackSize = buffer.readVarInt();
            currentStack.setCount(newStackSize);
            gui.getModularUIContainer().setCarried(currentStack);
        } else {
            super.readUpdateInfo(id, buffer);
            return;
        }
        if (changeListener != null) {
            changeListener.run();
        }
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 1) {
            boolean isFill = buffer.readBoolean();
            boolean tryMax = buffer.readBoolean();
            int clickResult = tryClickContainer(isFill, tryMax);
            if (clickResult >= 0) {
                writeUpdateInfo(4, buf -> buf.writeVarInt(clickResult));
            }
        }
    }

    private int tryClickContainer(boolean isFill, boolean tryMax) {
        if (chemicalHandler == null) return -1;
        Player player = gui.entityPlayer;
        ItemStack currentStack = gui.getModularUIContainer().getCarried();
        var optional = currentStack.getCapability(getCapability()).resolve();
        if (optional.isEmpty()) return -1;
        var handler = optional.get();
        if (isFill && allowClickFilled && chemicalHandler.getChemicalInTank(tank).getAmount() > 0) {
            boolean performedFill = false;
            while (true) {
                var remaining = handler.insertChemical(chemicalHandler.getChemicalInTank(tank), Action.SIMULATE);
                if (remaining.isStackIdentical(chemicalHandler.getChemicalInTank(tank))) break;
                currentStack.getCapability(getCapability()).ifPresent(cap -> {
                    var left = handler.insertChemical(chemicalHandler.getChemicalInTank(tank), Action.EXECUTE);
                    chemicalHandler.setChemicalInTank(tank, left);
                });
                performedFill = true;
                if (!tryMax) break;
            }
            if (performedFill) {
                var soundevent = FluidHelper.getFillSound(FluidStack.create(Fluids.WATER, 1000));
                if (soundevent != null) {
                    player.level().playSound(null, player.position().x, player.position().y + 0.5, player.position().z, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                gui.getModularUIContainer().setCarried(currentStack);
                return currentStack.getCount();
            }
        } else if (!isFill && allowClickDrained) {
            boolean performedEmptying = false;
            while (true) {
                var available = chemicalHandler.getChemicalInTank(tank).copy();
                STACK extracted;
                if (available.isEmpty()) {
                    extracted = handler.extractChemical(chemicalHandler.getTankCapacity(tank), Action.SIMULATE);
                } else {
                    available.setAmount(chemicalHandler.getTankCapacity(tank) - available.getAmount());
                    extracted = handler.extractChemical((STACK) available, Action.SIMULATE);
                }
                if (extracted.isEmpty()) break;
                currentStack.getCapability(getCapability()).ifPresent(cap -> {
                    var realExtracted = handler.extractChemical(extracted, Action.EXECUTE);
                    realExtracted.setAmount(realExtracted.getAmount() + chemicalHandler.getChemicalInTank(tank).getAmount());
                    chemicalHandler.setChemicalInTank(tank, realExtracted);
                });
                performedEmptying = true;
                if (!tryMax) break;
            }
            if (performedEmptying) {
                var soundevent = FluidHelper.getEmptySound(FluidStack.create(Fluids.WATER, 1000));
                if (soundevent != null) {
                    player.level().playSound(null, player.position().x, player.position().y + 0.5, player.position().z, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                gui.getModularUIContainer().setCarried(currentStack);
                return currentStack.getCount();
            }
        }

        return -1;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((allowClickDrained || allowClickFilled) && isMouseOverElement(mouseX, mouseY)) {
            if (button == 0 || button == 1) {
                ItemStack currentStack = gui.getModularUIContainer().getCarried();
                var optional = currentStack.getCapability(getCapability()).resolve();
                var isFill = button == 0;
                var tryMax = isShiftDown();
                if (optional.isPresent()) {
                    writeClientAction(1, writer -> {
                        writer.writeBoolean(isFill);
                        writer.writeBoolean(tryMax);
                    });
                    playButtonClickSound();
                    return true;
                }
            }
        }
        return false;
    }

    @LDLRegister(name = "chemical_gas_slot", group = "widget.container", modID = "mekanism")
    @NoArgsConstructor
    public static class Gas extends ChemicalTankWidget<mekanism.api.chemical.gas.Gas, GasStack> {

        public Gas(@Nullable IGasHandler chemicalHandler, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, x, y, allowClickContainerFilling, allowClickContainerEmptying);
        }

        public Gas(@Nullable IGasHandler chemicalHandler, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
        }

        public Gas(@Nullable IGasHandler chemicalHandler, int tank, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, tank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
        }

        public Gas(@Nullable IGasHandler chemicalHandler, int tank, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, tank, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
        }

        @Override
        public Capability<IGasHandler> getCapability() {
            return Capabilities.GAS_HANDLER;
        }

        @Override
        public GasStack readStack(CompoundTag tag) {
            return GasStack.readFromNBT(tag);
        }

        @Override
        public void buildConfigurator(ConfiguratorGroup father) {
            if (ChemicalTankBuilder.GAS.createAllValid(2000, null) instanceof IGasHandler handler) {
                handler.insertChemical(MekanismChemicalRecipeCapability.CAP_GAS.createDefaultContent(), Action.EXECUTE);
                father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", new Gas() {
                    @Override
                    public void updateScreen() {
                        super.updateScreen();
                        setHoverTooltips(Gas.this.tooltipTexts);
                        this.backgroundTexture = Gas.this.backgroundTexture;
                        this.hoverTexture = Gas.this.hoverTexture;
                        this.showAmount = Gas.this.showAmount;
                        this.drawHoverTips = Gas.this.drawHoverTips;
                        this.fillDirection = Gas.this.fillDirection;
                        this.overlay = Gas.this.overlay;
                    }
                }.setAllowClickDrained(false).setAllowClickFilled(false).setChemicalTank(handler)));
            }
            super.buildConfigurator(father);
        }
    }

    @LDLRegister(name = "chemical_infuse_slot", group = "widget.container", modID = "mekanism")
    @NoArgsConstructor
    public static class Infuse extends ChemicalTankWidget<InfuseType, InfusionStack> {

        public Infuse(@Nullable IInfusionHandler chemicalHandler, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, x, y, allowClickContainerFilling, allowClickContainerEmptying);
        }

        public Infuse(@Nullable IInfusionHandler chemicalHandler, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
        }

        public Infuse(@Nullable IInfusionHandler chemicalHandler, int tank, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, tank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
        }

        public Infuse(@Nullable IInfusionHandler chemicalHandler, int tank, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
            super(chemicalHandler, tank, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
        }

        @Override
        public Capability<IInfusionHandler> getCapability() {
            return Capabilities.INFUSION_HANDLER;
        }

        @Override
        public InfusionStack readStack(CompoundTag tag) {
            return InfusionStack.readFromNBT(tag);
        }

        @Override
        public void buildConfigurator(ConfiguratorGroup father) {
            if (ChemicalTankBuilder.INFUSION.createAllValid(2000, null) instanceof IInfusionHandler handler) {
                handler.insertChemical(MekanismChemicalRecipeCapability.CAP_INFUSE.createDefaultContent(), Action.EXECUTE);
                father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", new Infuse() {
                    @Override
                    public void updateScreen() {
                        super.updateScreen();
                        setHoverTooltips(Infuse.this.tooltipTexts);
                        this.backgroundTexture = Infuse.this.backgroundTexture;
                        this.hoverTexture = Infuse.this.hoverTexture;
                        this.showAmount = Infuse.this.showAmount;
                        this.drawHoverTips = Infuse.this.drawHoverTips;
                        this.fillDirection = Infuse.this.fillDirection;
                        this.overlay = Infuse.this.overlay;
                    }
                }.setAllowClickDrained(false).setAllowClickFilled(false).setChemicalTank(handler)));
            }
            super.buildConfigurator(father);
        }
    }


    @LDLRegister(name = "chemical_pigment_slot", group = "widget.container", modID = "mekanism")
    @NoArgsConstructor
    public static class Pigment extends ChemicalTankWidget<mekanism.api.chemical.pigment.Pigment, PigmentStack> {

            public Pigment(@Nullable IPigmentHandler chemicalHandler, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, x, y, allowClickContainerFilling, allowClickContainerEmptying);
            }

            public Pigment(@Nullable IPigmentHandler chemicalHandler, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
            }

            public Pigment(@Nullable IPigmentHandler chemicalHandler, int tank, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, tank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
            }

            public Pigment(@Nullable IPigmentHandler chemicalHandler, int tank, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, tank, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
            }

            @Override
            public Capability<IPigmentHandler> getCapability() {
                return Capabilities.PIGMENT_HANDLER;
            }

            @Override
            public PigmentStack readStack(CompoundTag tag) {
                return PigmentStack.readFromNBT(tag);
            }

            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                if (ChemicalTankBuilder.PIGMENT.createAllValid(2000, null) instanceof IPigmentHandler handler) {
                    handler.insertChemical(MekanismChemicalRecipeCapability.CAP_PIGMENT.createDefaultContent(), Action.EXECUTE);
                    father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", new Pigment() {
                        @Override
                        public void updateScreen() {
                            super.updateScreen();
                            setHoverTooltips(Pigment.this.tooltipTexts);
                            this.backgroundTexture = Pigment.this.backgroundTexture;
                            this.hoverTexture = Pigment.this.hoverTexture;
                            this.showAmount = Pigment.this.showAmount;
                            this.drawHoverTips = Pigment.this.drawHoverTips;
                            this.fillDirection = Pigment.this.fillDirection;
                            this.overlay = Pigment.this.overlay;
                        }
                    }.setAllowClickDrained(false).setAllowClickFilled(false).setChemicalTank(handler)));
                }
                super.buildConfigurator(father);
            }
    }

    @LDLRegister(name = "chemical_slurry_slot", group = "widget.container", modID = "mekanism")
    @NoArgsConstructor
    public static class Slurry extends ChemicalTankWidget<mekanism.api.chemical.slurry.Slurry, SlurryStack> {
            
            public Slurry(@Nullable ISlurryHandler chemicalHandler, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, x, y, allowClickContainerFilling, allowClickContainerEmptying);
            }
    
            public Slurry(@Nullable ISlurryHandler chemicalHandler, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
            }
    
            public Slurry(@Nullable ISlurryHandler chemicalHandler, int tank, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, tank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
            }
    
            public Slurry(@Nullable ISlurryHandler chemicalHandler, int tank, int x, int y, int width, int height, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
                super(chemicalHandler, tank, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
            }
    
            @Override
            public Capability<ISlurryHandler> getCapability() {
                return Capabilities.SLURRY_HANDLER;
            }
    
            @Override
            public SlurryStack readStack(CompoundTag tag) {
                return SlurryStack.readFromNBT(tag);
            }

            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                if (ChemicalTankBuilder.SLURRY.createAllValid(2000, null) instanceof ISlurryHandler handler) {
                    handler.insertChemical(MekanismChemicalRecipeCapability.CAP_SLURRY.createDefaultContent(), Action.EXECUTE);
                    father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", new Slurry() {
                        @Override
                        public void updateScreen() {
                            super.updateScreen();
                            setHoverTooltips(Slurry.this.tooltipTexts);
                            this.backgroundTexture = Slurry.this.backgroundTexture;
                            this.hoverTexture = Slurry.this.hoverTexture;
                            this.showAmount = Slurry.this.showAmount;
                            this.drawHoverTips = Slurry.this.drawHoverTips;
                            this.fillDirection = Slurry.this.fillDirection;
                            this.overlay = Slurry.this.overlay;
                        }
                    }.setAllowClickDrained(false).setAllowClickFilled(false).setChemicalTank(handler)));
                }
                super.buildConfigurator(father);
            }
    }
}

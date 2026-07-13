package com.lowdragmc.mbd2.integration.mekanism;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import mekanism.api.Action;
import mekanism.api.IMekanismAccess;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "chemical-slot", group = "inventory", registry = "ldlib2:ui_element", modID = "mekanism")
public class ChemicalSlot extends BindableUIElement<ChemicalStack> {

    public final Label amountLabel = new Label();
    @Getter @Setter @Configurable
    private boolean allowClickFilled = true;
    @Getter @Setter @Configurable
    private boolean allowClickDrained = true;
    @Getter
    private ChemicalStack chemical = ChemicalStack.EMPTY;
    @Getter @Setter
    private long capacity;
    @Getter @Setter @Configurable
    private FillDirection fillDirection = FillDirection.ALWAYS_FULL;

    private final RPCEmitter clickEvent;

    @Nullable
    private IChemicalHandler boundHandler;
    private int tankIndex;
    @Nullable
    private ISubscription handlerSubscription;

    public ChemicalSlot() {
        addClass("fluid-slot_bg");
        getLayout().width(18);
        getLayout().height(18);
        getLayout().paddingAll(1);
        getStyle().backgroundTexture(Sprites.RECT_DARK);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        clickEvent = addRPCEvent(RPCEventBuilder.simple(Boolean.class, this::tryClickContainer));

        amountLabel.layout(layout -> layout.widthPercent(100).heightPercent(100));
        amountLabel.textStyle(textStyle -> textStyle
                .textAlignVertical(Vertical.BOTTOM)
                .textAlignHorizontal(Horizontal.RIGHT)
                .fontSize(4.5f));
        amountLabel.bindDataSource(SupplierDataSource.of(this::getChemicalAmountText));
        addChild(amountLabel);
        internalSetup();
    }

    public ChemicalSlot bind(@Nullable IChemicalHandler handler, int tankIndex) {
        if (handlerSubscription != null) {
            handlerSubscription.unsubscribe();
        }
        this.boundHandler = handler;
        if (handler == null) return this;
        if (tankIndex < 0 || tankIndex >= handler.getChemicalTanks()) {
            throw new IllegalArgumentException("Invalid tank index: " + tankIndex);
        }
        this.tankIndex = tankIndex;
        if (capacity <= 0) {
            capacity = handler.getChemicalTankCapacity(tankIndex);
        }
        // ChemicalStack is registered as an LDLib sync type (see MBDSyncedFieldAccessors), so use a
        // server->client DataBinding exactly like FluidSlot. A client-side tick poll would only read
        // the local handler, which isn't kept in sync while the GUI is open, so the slot would stay
        // empty until the block entity is reloaded (relog/restart).
        var chemicalBinding = DataBindingBuilder.create(
                        () -> handler.getChemicalInTank(this.tankIndex), (ChemicalStack ignored) -> {})
                .syncType(ChemicalStack.class)
                .c2sStrategy(SyncStrategy.NONE)
                .build();
        var capacitySyncValue = DataBindingBuilder.longValS2C(() -> handler.getChemicalTankCapacity(this.tankIndex))
                .remoteSetter(this::setCapacity).build().getSyncValue();
        bind(chemicalBinding);
        addSyncValue(capacitySyncValue);
        handlerSubscription = () -> {
            unbind(chemicalBinding);
            removeSyncValue(capacitySyncValue);
            handlerSubscription = null;
        };
        return this;
    }

    @Override
    public ChemicalStack getValue() {
        return chemical;
    }

    @Override
    public ChemicalSlot setValue(@Nullable ChemicalStack value, boolean notify) {
        if (value == null) value = ChemicalStack.EMPTY;
        if (value.isEmpty() && chemical.isEmpty()) return this;
        if (ChemicalStack.isSameChemical(value, chemical) && value.getAmount() == chemical.getAmount()) {
            return this;
        }
        this.chemical = value;
        if (notify) notifyListeners();
        return this;
    }

    public ChemicalSlot setChemical(ChemicalStack stack) {
        return setValue(stack, true);
    }

    // region tooltip + label
    private Component getChemicalAmountText() {
        if (chemical.isEmpty()) return Component.empty();
        return Component.literal(TextFormattingUtil.formatLongToCompactString(chemical.getAmount(), 3) + "mB");
    }

    private List<Component> buildTooltips() {
        var tooltips = new ArrayList<Component>();
        if (chemical.isEmpty()) {
            tooltips.add(Component.translatable("mbd2.tooltip.chemical.empty"));
        } else {
            tooltips.add(chemical.getTextComponent());
            var cap = Math.max(capacity, chemical.getAmount());
            tooltips.add(Component.translatable("mbd2.tooltip.chemical.amount", chemical.getAmount(), cap));
        }
        tooltips.addAll(getStyle().tooltips().asList());
        return tooltips;
    }

    private void onHoverTooltips(UIEvent event) {
        event.hoverTooltips = new HoverTooltips(buildTooltips(), null, null, null);
    }
    // endregion

    // region click container interaction
    private void onMouseDown(UIEvent event) {
        clickEvent.send(event.isShiftDown());
    }

    private void tryClickContainer(boolean isShiftKeyDown) {
        if (boundHandler == null) return;
        if (tankIndex < 0 || tankIndex >= boundHandler.getChemicalTanks()) return;
        var mui = getModularUI();
        if (mui == null || mui.getMenu() == null) return;
        var player = mui.player;
        if (player == null) return;
        var menu = mui.getMenu();
        var carried = menu.getCarried();
        var carriedHandler = Capabilities.CHEMICAL.getCapability(carried);
        if (carriedHandler == null) return;

        int maxAttempts = isShiftKeyDown ? carried.getCount() : 1;
        var initialFluid = boundHandler.getChemicalInTank(tankIndex);

        // tank has content -> try to drain into carried container
        if (allowClickFilled && !initialFluid.isEmpty()) {
            boolean drained = false;
            for (int i = 0; i < maxAttempts; i++) {
                if (!transferOnce(boundHandler, carriedHandler, player, carried)) break;
                drained = true;
            }
            if (drained) {
                menu.setCarried(carried);
                return;
            }
        }

        // carried container has content -> try to fill the tank
        if (allowClickDrained) {
            for (int i = 0; i < maxAttempts; i++) {
                if (!transferOnce(carriedHandler, boundHandler, player, carried)) break;
            }
            menu.setCarried(carried);
        }
    }

    /**
     * Attempts to move one whole "slot of chemical" from source to target, modeled after
     * FluidUtil.tryFillContainer / tryEmptyContainer. Returns true if anything moved.
     */
    private static boolean transferOnce(IChemicalHandler source, IChemicalHandler target,
                                        Player player, ItemStack carried) {
        var available = source.extractChemical(Long.MAX_VALUE, Action.SIMULATE);
        if (available.isEmpty()) return false;
        var accepted = target.insertChemical(available, Action.SIMULATE);
        long moved = available.getAmount() - accepted.getAmount();
        if (moved <= 0) return false;
        var extracted = source.extractChemical(moved, Action.EXECUTE);
        if (extracted.isEmpty()) return false;
        var leftover = target.insertChemical(extracted, Action.EXECUTE);
        if (!leftover.isEmpty()) {
            // shouldn't happen normally — return leftover to source to avoid voiding
            source.insertChemical(leftover, Action.EXECUTE);
        }
        return true;
    }
    // endregion

    // region rendering
    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawBackgroundAdditional(GUIContext guiContext) {
        if (chemical.isEmpty()) {
            if (isHover() || isSelfOrChildHover()) {
                drawHover(guiContext);
            }
            return;
        }
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();
        var max = Math.max(Math.max(chemical.getAmount(), capacity), 1L);
        double progress = chemical.getAmount() * 1.0 / max;
        float drawnU = (float) fillDirection.getDrawnU(progress);
        float drawnV = (float) fillDirection.getDrawnV(progress);
        float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
        float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
        drawChemical(guiContext.graphics, chemical,
                contentX + drawnU * contentWidth,
                contentY + drawnV * contentHeight,
                contentWidth * drawnWidth,
                contentHeight * drawnHeight);
        if (isHover() || isSelfOrChildHover()) {
            drawHover(guiContext);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawHover(GUIContext guiContext) {
        guiContext.drawTexture(new ColorRectTexture(0x80FFFFFF), getContentX(), getContentY(), getContentWidth(), getContentHeight());
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawChemical(GuiGraphics graphics, ChemicalStack stack, float x, float y, float width, float height) {
        TextureAtlasSprite sprite = MekanismRenderer.getChemicalTexture(stack);
        if (sprite == null) return;
        int color = stack.getChemicalTint() | 0xff000000;
        RenderSystem.enableBlend();
        int xTiles = (int) (width / 16);
        float xRemainder = width - xTiles * 16f;
        int yTiles = (int) (height / 16);
        float yRemainder = height - yTiles * 16f;
        float yEnd = y + height;
        for (int xt = 0; xt <= xTiles; xt++) {
            for (int yt = 0; yt <= yTiles; yt++) {
                float w = (xt == xTiles) ? xRemainder : 16f;
                float h = (yt == yTiles) ? yRemainder : 16f;
                if (w <= 0 || h <= 0) continue;
                float sx = x + xt * 16f;
                float sy = yEnd - (yt + 1) * 16f;
                drawSpriteClipped(graphics, sprite, sx, sy, w, h, color);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSpriteClipped(GuiGraphics graphics, TextureAtlasSprite sprite, float x, float y, float width, float height, int color) {
        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();
        float maskRight = 16f - width;
        float maskTop = 16f - height;
        float uClippedMax = uMax - maskRight / 16f * (uMax - uMin);
        float vClippedMax = vMax - maskTop / 16f * (vMax - vMin);
        var pose = graphics.pose().last().pose();
        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.guiTexture(InventoryMenu.BLOCK_ATLAS));
        buffer.addVertex(pose, x, y + 16, 0).setUv(uMin, vClippedMax).setColor(color);
        buffer.addVertex(pose, x + 16 - maskRight, y + 16, 0).setUv(uClippedMax, vClippedMax).setColor(color);
        buffer.addVertex(pose, x + 16 - maskRight, y + maskTop, 0).setUv(uClippedMax, vMin).setColor(color);
        buffer.addVertex(pose, x, y + maskTop, 0).setUv(uMin, vMin).setColor(color);
    }
    // endregion

    // region XEI integration
    public ChemicalSlot xeiRecipeIngredient(IngredientIO io) {
        return xeiRecipeIngredient(io, () -> Stream.of(getChemical()));
    }

    public ChemicalSlot xeiRecipeIngredient(IngredientIO io, Supplier<Stream<ChemicalStack>> allPossibleChemicals) {
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeIngredient(this, io, allPossibleChemicals);
        }
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeIngredient(this, io, allPossibleChemicals);
        }
        return this;
    }

    public ChemicalSlot xeiRecipeSlot(IngredientIO io, float chance) {
        return xeiRecipeSlot(io, chance, (int) Math.min(getChemical().getAmount(), Integer.MAX_VALUE),
                () -> Stream.of(getChemical()));
    }

    public ChemicalSlot xeiRecipeSlot(IngredientIO io, float chance, int amount, Supplier<Stream<ChemicalStack>> allPossibleChemicals) {
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeSlot(this, () -> chance, () -> amount, allPossibleChemicals);
        }
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeSlot(this, allPossibleChemicals);
        }
        return this;
    }

    public static class JEISupport {
        @SuppressWarnings("unchecked")
        private static <T> mezz.jei.api.ingredients.IIngredientType<T> chemicalType() {
            var helper = IMekanismAccess.INSTANCE.jeiHelper().getChemicalStackHelper();
            return (mezz.jei.api.ingredients.IIngredientType<T>) helper.getIngredientType();
        }

        public static void recipeIngredient(ChemicalSlot slot, IngredientIO io, Supplier<Stream<ChemicalStack>> allPossible) {
            LDLibJEIPlugin.recipeIngredient(slot, io, () -> allPossible.get()
                    .map(stack -> LDLibJEIPlugin.createTypedIngredient(chemicalType(), stack))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
        }

        public static void recipeSlot(ChemicalSlot slot, Supplier<Stream<ChemicalStack>> allPossible) {
            LDLibJEIPlugin.recipeSlot(slot, () -> {
                var current = slot.getChemical();
                if (current.isEmpty()) return null;
                return LDLibJEIPlugin.createTypedIngredient(JEISupport.<ChemicalStack>chemicalType(), current).orElse(null);
            }, () -> allPossible.get()
                    .map(stack -> LDLibJEIPlugin.createTypedIngredient(JEISupport.<ChemicalStack>chemicalType(), stack).orElseThrow())
                    .collect(Collectors.toList()));
        }
    }

    public static class EMISupport {
        private static EmiStack toEmi(ChemicalStack stack) {
            return IMekanismAccess.INSTANCE.emiHelper().createEmiStack(stack);
        }

        public static void recipeIngredient(ChemicalSlot slot, IngredientIO io, Supplier<Stream<ChemicalStack>> allPossible) {
            LDLibEMIPlugin.recipeIngredient(slot, io, () -> allPossible.get()
                    .map(EMISupport::toEmi)
                    .collect(Collectors.toList()));
        }

        public static void recipeSlot(ChemicalSlot slot, Supplier<Float> chance, IntSupplier amount, Supplier<Stream<ChemicalStack>> allPossible) {
            LDLibEMIPlugin.recipeSlot(slot, () -> {
                var list = allPossible.get().map(EMISupport::toEmi).map(s -> s.setChance(chance.get())).collect(Collectors.toList());
                return new dev.emi.emi.api.stack.ListEmiIngredient(list, amount.getAsInt()).setChance(chance.get());
            });
        }
    }
    // endregion
}

package com.lowdragmc.mbd2.common.gui.recipe.ingredient.entity;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.rei.LDLibREIPlugin;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.stack.ItemEmiStack;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "entity-ingredient-slot", group = "inventory", registry = "ldlib2:ui_element")
public class EntityIngredientSlot extends BindableUIElement<EntityIngredient> {
    @Configurable(name = "SlotStyle")
    public class SlotStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.HOVER_OVERLAY,
        };
        public SlotStyle() {
            super(EntityIngredientSlot.this);
            setDefault(PropertyRegistry.HOVER_OVERLAY, new ColorRectTexture(0x80FFFFFF));
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture hoverOverlay() {
            return getValueSave(PropertyRegistry.HOVER_OVERLAY);
        }

        public EntityIngredientSlot.SlotStyle hoverOverlay(IGuiTexture texture) {
            set(PropertyRegistry.HOVER_OVERLAY, texture);
            return this;
        }
    }

    public final Label countLabel = new Label();
    @Getter
    private final EntityIngredientSlot.SlotStyle slotStyle = new EntityIngredientSlot.SlotStyle();
    // editor support
    @Configurable(name = "EditorEntityDisplay")
    private EntityIngredient editorEntityDisplay = EntityIngredient.EMPTY;
    @Configurable(name = "EditorAllowLookup")
    private boolean allowXEILookup = true;
    // runtime
    private TrackedDummyWorld dummyWorld;
    @Nullable
    private List<Entity> entities;
    @Getter
    private EntityIngredient entityIngredient = EntityIngredient.EMPTY;

    public EntityIngredientSlot() {
        addClass("fluid-slot_bg");
        getLayout().width(18);
        getLayout().height(18);
        getLayout().paddingAll(1);
        getStyle().backgroundTexture(Sprites.RECT_DARK);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        if (LDLib2.isJeiLoaded()) {
            EntityIngredientSlot.JEISupport.clickableIngredient(this);
        }
        if (LDLib2.isReiLoaded()) {
            EntityIngredientSlot.REISupport.focusedStack(this);
        }
        if (LDLib2.isEmiLoaded()) {
            EntityIngredientSlot.EMISupport.stackProvider(this);
        }

        countLabel.addClass("__fluid-slot_amount-label__");
        countLabel.layout(layout -> layout.widthPercent(100).heightPercent(100));
        countLabel.textStyle(textStyle -> textStyle
                .textAlignVertical(Vertical.BOTTOM)
                .textAlignHorizontal(Horizontal.RIGHT)
                .fontSize(4.5f)
        );
        countLabel.bindDataSource(SupplierDataSource.of(this::getIngredientCountText));
        addChild(countLabel);
        internalSetup();
    }

    public EntityIngredientSlot slotStyle(Consumer<EntityIngredientSlot.SlotStyle> style) {
        style.accept(slotStyle);
        return this;
    }

    public EntityIngredientSlot xeiPhantom() {
        if (LDLib2.isJeiLoaded()) {
            EntityIngredientSlot.JEISupport.ghostIngredient(this);
        }
        if (LDLib2.isReiLoaded()) {
            EntityIngredientSlot.REISupport.draggableStackBounds(this);
            EntityIngredientSlot.REISupport.acceptDraggableStack(this);
        }
        if (LDLib2.isEmiLoaded()) {
            EntityIngredientSlot.EMISupport.renderDragHandler(this);
            EntityIngredientSlot.EMISupport.dropStackHandler(this);
        }
        return this;
    }

    public EntityIngredientSlot xeiRecipeIngredient(IngredientIO io) {
        if (LDLib2.isJeiLoaded()) {
            EntityIngredientSlot.JEISupport.recipeIngredient(this, io);
        }
        if (LDLib2.isReiLoaded()) {
            EntityIngredientSlot.REISupport.recipeIngredient(this, io);
        }
        if (LDLib2.isEmiLoaded()) {
            EntityIngredientSlot.EMISupport.recipeIngredient(this, io);
        }
        return this;
    }

    public EntityIngredientSlot xeiRecipeSlot() {
        return xeiRecipeSlot(IngredientIO.NONE, 1);
    }

    public EntityIngredientSlot xeiRecipeSlot(IngredientIO io, float chance) {
        if (LDLib2.isJeiLoaded()) {
            EntityIngredientSlot.JEISupport.recipeSlot(this);
        }
        if (LDLib2.isReiLoaded()) {
            EntityIngredientSlot.REISupport.recipeSlot(this, io);
        }
        if (LDLib2.isEmiLoaded()) {
            EntityIngredientSlot.EMISupport.recipeSlot(this, chance);
        }
        return this;
    }


    public EntityIngredientSlot setEntityIngredient(EntityIngredient entityIngredient) {
        return setValue(entityIngredient, true);
    }

    public EntityIngredientSlot setEntityIngredient(EntityIngredient fluid, boolean notify) {
        return setValue(fluid, notify);
    }

    public List<Component> getFullTooltipTexts() {
        var tooltips = new ArrayList<Component>();
        var entity = getCurrentEntity();
        if (entity != null) {
            tooltips.add(entity.getDisplayName());
        }
        tooltips.addAll(getStyle().tooltips().asList());
        return tooltips;
    }

    public Component getIngredientCountText() {
        var entityIngredient = getValue();
        if (entityIngredient.isEmpty()) return Component.empty();
        return Component.literal(TextFormattingUtil.formatLongToCompactStringBuckets(entityIngredient.getCount(), 3));
    }

    protected void onHoverTooltips(UIEvent event) {
        var entity = getValue();
        if (entity.isEmpty()) return;
        event.hoverTooltips = new HoverTooltips(getFullTooltipTexts(), null, null, null);
    }

    @Override
    public EntityIngredient getValue() {
        return entityIngredient;
    }

    @Override
    public EntityIngredientSlot setValue(@Nullable EntityIngredient value, boolean notify) {
        if (value == null) value = EntityIngredient.EMPTY;
        if (Objects.equals(value, entityIngredient)) return this;
        this.entityIngredient = value;
        this.entities = null;
        if (notify) notifyListeners();
        return this;
    }

    public ItemStack getEntityItem() {
        var entity = getCurrentEntity();
        if (entity != null) {
            var egg = SpawnEggItem.byId(entity.getType());
            if (egg != null) {
                return new ItemStack(egg);
            }
        }
        return ItemStack.EMPTY;
    }

    public Stream<ItemStack> getEntityItems() {
        return Arrays.stream(getEntityIngredient().getTypes())
                .map(SpawnEggItem::byId)
                .filter(Objects::nonNull)
                .map(ItemStack::new)
                .filter(itemStack -> !itemStack.isEmpty());
    }

    @Nullable
    public Entity getCurrentEntity() {
        if (entities == null) {
            entities = new ArrayList<>();
            if (!entityIngredient.isEmpty() && LDLib2.isClient()) {
                if (dummyWorld == null) {
                    dummyWorld = new TrackedDummyWorld();
                }
                for (var entityType : entityIngredient.getTypes()) {
                    var entity = entityType.create(dummyWorld);
                    if (entity != null) {
                        if (entityIngredient.getNbt() != null) {
                            var tag = entity.saveWithoutId(new CompoundTag());
                            tag.merge(entityIngredient.getNbt());
                            entity.load(tag);
                        }
                        entities.add(entity);
                    }
                }
            }
        }
        if (entities.isEmpty()) return null;
        var index = Math.abs((int)(System.currentTimeMillis() / 1000) % entities.size());
        return entities.get(index);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var renderedEntity = getValue();
        var hovered = isHover() || isSelfOrChildHover();
        if (renderedEntity.isEmpty() && !hovered) return;
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        var currentEntity = getCurrentEntity();
        if (currentEntity != null) {
            var width = getContentWidth();
            var height = getContentHeight();
            var scaleH = height / currentEntity.getBbHeight();
            var scaleW = width / currentEntity.getBbWidth();
            renderEntityInInventory(guiContext.graphics,
                    getContentX() + width / 2,
                    getContentY() + height / 2,
                    Math.min(scaleW, scaleH) * 0.45, currentEntity);
        }

        if (hovered) {
            guiContext.drawTexture(slotStyle.hoverOverlay(), contentX, contentY, contentWidth, contentHeight);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderEntityInInventory(GuiGraphics pGuiGraphics, float x, float y, double pScale, Entity entity) {
        pGuiGraphics.flush();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        var offset = (System.currentTimeMillis() % 3600) / 3600f;
        Quaternionf quaternionf = (new Quaternionf()).rotateXYZ(0, (float)Math.PI * 2 * offset, (float)Math.PI);
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(x, y, 0);
        pGuiGraphics.pose().mulPose((new Matrix4f()).scaling((float)pScale, (float)pScale, (float)(-pScale)));
        pGuiGraphics.pose().translate(0, entity.getBbHeight() / 2, 0);
        pGuiGraphics.pose().mulPose(quaternionf);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        entityrenderdispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0, 1,
                pGuiGraphics.pose(), pGuiGraphics.bufferSource(), 15728880));
        pGuiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        pGuiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    /// Editor Support
    @ConfigSetter(field = "editorEntityDisplay")
    private void setEditorEntityDisplay(EntityIngredient entityIngredient) {
        this.editorEntityDisplay = entityIngredient;
        setValue(entityIngredient, false);
        countLabel.setValue(getIngredientCountText());
    }

    @SkipPersistedValue(field = "editorEntityDisplay")
    private boolean skipEditorFluidDisplay(EntityIngredient entityIngredient) {
        return entityIngredient == EntityIngredient.EMPTY;
    }

    @ConfigSetter(field = "allowXEILookup")
    private void setAllowXEILookup(boolean allowXEILookup) {
        this.allowXEILookup = allowXEILookup;
    }

    @SkipPersistedValue(field = "allowXEILookup")
    private boolean skipAllowXEILookup(boolean allowXEILookup) {
        return allowXEILookup;
    }

    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        this.editorEntityDisplay = EntityIngredient.EMPTY;
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        if (!editorEntityDisplay.isEmpty()) {
            setValue(editorEntityDisplay, false);
        }
    }

    @Override
    public void loadXml(Element element) {
        // allow xei lookup
        if (element.hasAttribute("allow-xei-Lookup")) {
            setAllowXEILookup(XmlUtils.getAsBoolean(element, "allow-xei-Lookup", allowXEILookup));
        }
        // fluid display
        var entityIngredient = getEntityIngredientXml(element);
        if (entityIngredient != EntityIngredient.EMPTY) {
            setEditorEntityDisplay(entityIngredient);
        }

        super.loadXml(element);
    }

    public static EntityIngredient getEntityIngredientXml(Element element) {
        int count = XmlUtils.getAsInt(element, "count", 1);
        var entityIngredient = EntityIngredient.EMPTY;
        if (element.hasAttribute("fluid")) {
            var entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(element.getAttribute("entity")));
            entityIngredient = EntityIngredient.of(count, entityType);
            CompoundTag tag = null;
            NodeList nodeList = element.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i) instanceof Element subElement && subElement.getNodeName().equals("nbt")) {
                    tag = XmlUtils.getCompoundTag(subElement);
                    break;
                }
            }
            if (tag != null) entityIngredient.setNbt(tag);
        }
        return entityIngredient;
    }

    // region XEI Support
    public static class JEISupport {
        public static void clickableIngredient(EntityIngredientSlot entityIngredientSlot) {
            LDLibJEIPlugin.clickableIngredient(entityIngredientSlot, () -> {
                if (!entityIngredientSlot.allowXEILookup) return null;
                var current = entityIngredientSlot.getEntityItem();
                if (current.isEmpty()) return null;
                return LDLibJEIPlugin.createTypedIngredient(VanillaTypes.ITEM_STACK, current)
                        .orElse(null);
            });
        }

        public static void ghostIngredient(EntityIngredientSlot entityIngredientSlot) {
            LDLibJEIPlugin.ghostIngredient(entityIngredientSlot, VanillaTypes.ITEM_STACK,
                    ingredient -> ingredient.getIngredient().getItem() instanceof SpawnEggItem,
                    itemStack -> {
                if (itemStack.getItem() instanceof SpawnEggItem spawnEggItem) {
                    entityIngredientSlot.setValue(EntityIngredient.of(entityIngredientSlot.getValue().getCount(), spawnEggItem.getType(itemStack)));
                }
            });
        }

        public static void recipeIngredient(EntityIngredientSlot entityIngredientSlot, IngredientIO io) {
            LDLibJEIPlugin.recipeIngredient(entityIngredientSlot, io, () -> entityIngredientSlot.getEntityItems().map(item ->
                LDLibJEIPlugin.createTypedIngredient(VanillaTypes.ITEM_STACK, item).orElseThrow()).collect(Collectors.toList())
            );
        }

        public static void recipeSlot(EntityIngredientSlot entityIngredientSlot) {
            LDLibJEIPlugin.recipeSlot(entityIngredientSlot, () -> {
                var item = entityIngredientSlot.getEntityItem();
                return item.isEmpty() ? null : LDLibJEIPlugin
                        .createTypedIngredient(VanillaTypes.ITEM_STACK, entityIngredientSlot.getEntityItem())
                        .orElse(null);
            }, () -> {
                var item = entityIngredientSlot.getEntityItem();
                return List.of(LDLibJEIPlugin.createTypedIngredient(VanillaTypes.ITEM_STACK, item)
                        .orElseThrow());
            });
        }
    }

    public static class REISupport {
        public static void focusedStack(EntityIngredientSlot entityIngredientSlot) {
            LDLibREIPlugin.focusedStack(entityIngredientSlot, () -> {
                if (!entityIngredientSlot.allowXEILookup) return null;
                var item = entityIngredientSlot.getEntityItem();
                if (item.isEmpty()) return null;
                return EntryStacks.of(item);
            });
        }

        public static void draggableStackBounds(EntityIngredientSlot entityIngredientSlot) {
            LDLibREIPlugin.draggableStackBounds(entityIngredientSlot,
                    VanillaEntryTypes.ITEM,
                    stack -> true);
        }

        public static void acceptDraggableStack(EntityIngredientSlot entityIngredientSlot) {
            LDLibREIPlugin.acceptDraggableStack(entityIngredientSlot,
                    VanillaEntryTypes.ITEM,
                    stack -> true,
                    stack -> {
                        if (stack.getValue() instanceof ItemStack itemStack && itemStack.getItem() instanceof SpawnEggItem egg) {
                            entityIngredientSlot.setValue(EntityIngredient.of(entityIngredientSlot.getValue().getCount(), egg.getType(itemStack)));
                        }
                    });
        }

        public static void recipeIngredient(EntityIngredientSlot entityIngredientSlot, IngredientIO io) {
            LDLibREIPlugin.recipeIngredient(entityIngredientSlot, io, () ->
                    entityIngredientSlot.getEntityItems().map(EntryIngredients::of).toList()
            );
        }

        public static void recipeSlot(EntityIngredientSlot entityIngredientSlot, IngredientIO io) {
            LDLibREIPlugin.recipeSlot(entityIngredientSlot, io,
                    () -> EntryStacks.of(entityIngredientSlot.getEntityItem()),
                    () -> List.of(EntryStacks.of(entityIngredientSlot.getEntityItem())));
        }
    }

    public static class EMISupport {
        public static void stackProvider(EntityIngredientSlot entityIngredientSlot) {
            LDLibEMIPlugin.stackProvider(entityIngredientSlot, () -> {
                if (!entityIngredientSlot.allowXEILookup) return null;
                var item = entityIngredientSlot.getEntityItem();
                if (item.isEmpty()) return null;
                return new EmiStackInteraction(EmiStack.of(item), null, false);
            });
        }

        public static void renderDragHandler(EntityIngredientSlot entityIngredientSlot) {
            LDLibEMIPlugin.renderDragHandler(entityIngredientSlot,
                    dragged ->  dragged instanceof ItemEmiStack item
                            && item.getItemStack().getItem() instanceof SpawnEggItem);
        }

        public static void dropStackHandler(EntityIngredientSlot entityIngredientSlot) {
            LDLibEMIPlugin.dropStackHandler(entityIngredientSlot,
                    dragged -> dragged instanceof ItemEmiStack item
                            && item.getItemStack().getItem() instanceof SpawnEggItem,
                    dragged -> {
                        if (dragged instanceof ItemEmiStack item && item.getItemStack().getItem() instanceof SpawnEggItem egg) {
                            entityIngredientSlot.setValue(EntityIngredient.of(entityIngredientSlot.getValue().getCount(),
                                    egg.getType(item.getItemStack())));
                        }
                    });
        }

        public static void recipeIngredient(EntityIngredientSlot entityIngredientSlot, IngredientIO io) {
            LDLibEMIPlugin.recipeIngredient(entityIngredientSlot, io, () ->
                    entityIngredientSlot.getEntityItems().map(EmiStack::of).collect(Collectors.toList())
            );
        }

        public static void recipeSlot(EntityIngredientSlot entityIngredientSlot, float chance) {
            LDLibEMIPlugin.recipeSlot(entityIngredientSlot, () -> {
                var item = entityIngredientSlot.getEntityItem();
                return EmiStack.of(item).setChance(chance);
            });
        }
    }
    // endregion
}

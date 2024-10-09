package com.lowdragmc.mbd2.common.gui.recipe.ingredient.entity;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static com.lowdragmc.lowdraglib.gui.widget.TankWidget.FLUID_SLOT_TEXTURE;

@LDLRegister(name = "entity_preview", group = "widget.container")
@Accessors(chain = true)
public class EntityPreviewWidget extends Widget implements IRecipeIngredientSlot, IConfigurableWidget {
    @Getter
    @Nullable
    private EntityIngredient entityIngredient;
    @Configurable(name = "ldlib.gui.editor.name.showAmount")
    @Setter
    protected boolean showAmount = true;
    @Configurable(name = "ldlib.gui.editor.name.drawHoverOverlay")
    @Setter
    public boolean drawHoverOverlay = true;
    @Configurable(name = "ldlib.gui.editor.name.drawHoverTips")
    @Setter
    protected boolean drawHoverTips = true;
    @Setter
    protected BiConsumer<EntityPreviewWidget, List<Component>> onAddedTooltips;
    @Setter @Getter
    protected IngredientIO ingredientIO = IngredientIO.RENDER_ONLY;
    @Setter @Getter
    protected float XEIChance = 1f;

    // runtime
    private final List<Entity> entities = new ArrayList<>();
    private TrackedDummyWorld dummyWorld;

    public EntityPreviewWidget() {
        this(null, 0, 0, 30, 30);
    }

    public EntityPreviewWidget(EntityIngredient entityIngredient, int x, int y, int width, int height) {
        super(x, y, width, height);
        setEntityIngredient(entityIngredient);
    }

    @Override
    public void initTemplate() {
        setBackground(FLUID_SLOT_TEXTURE);
    }

    public EntityPreviewWidget setEntityIngredient(EntityIngredient entityIngredient) {
        this.entityIngredient = entityIngredient;
        entities.clear();
        if (entityIngredient != null && LDLib.isClient()) {
            if (dummyWorld == null) {
                dummyWorld = new TrackedDummyWorld();
            }
            for (var entityType : entityIngredient.getTypes()) {
                var entity = entityType.create(dummyWorld);
                if (entity != null) {
                    if (entityIngredient.getNbt() != null) {
                        var tag = entity.serializeNBT();
                        tag.merge(entityIngredient.getNbt());
                        entity.load(tag);
                    }
                    entities.add(entity);
                }
            }
        }
        return this;
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
    public List<Object> getXEIIngredients() {
        var items = entities.stream().map(Entity::getType).map(ForgeSpawnEggItem::fromEntityType).filter(Objects::nonNull).map(ItemStack::new).toList();
        if (items.isEmpty()) return Collections.emptyList();
        var realStack = items.get(0);

        if (LDLib.isJeiLoaded()) {
            var ingredient = JEIPlugin.getItemIngredient(realStack, getPosition().x, getPosition().y, getSize().width, getSize().height);
            return ingredient == null ? Collections.emptyList() : List.of(ingredient);
        } else if (LDLib.isReiLoaded()) {
            return SlotWidget.REICallWrapper.getReiIngredients(realStack);
        } else if (LDLib.isEmiLoaded()) {
            return SlotWidget.EMICallWrapper.getEmiIngredients(realStack, getXEIChance());
        }
        return List.of(realStack);
    }

    @Override
    public List<Component> getFullTooltipTexts() {
        var tooltips = new ArrayList<Component>();
        var entity = getCurrentEntity();
        if (entity != null) {
            tooltips.add(entity.getDisplayName());
        }
        tooltips.addAll(getTooltipTexts());
        return tooltips;
    }

    @Nullable
    public Entity getCurrentEntity() {
        if (entities.isEmpty()) return null;
        var index = Math.abs((int)(System.currentTimeMillis() / 1000) % entities.size());
        return entities.get(index);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var currentEntity = getCurrentEntity();
        var pos = getPosition();
        var size = getSize();
        if (currentEntity != null) {
            var scaleH = size.height / currentEntity.getBbHeight();
            var scaleW = size.width / currentEntity.getBbWidth();
            renderEntityInInventory(graphics, pos.x + size.width / 2, pos.y + size.height / 2, Math.min(scaleW, scaleH) * 0.45, currentEntity);
        }
        if (showAmount && entityIngredient != null) {
            graphics.pose().pushPose();
//            graphics.pose().scale(0.5F, 0.5F, 1);
            String s = TextFormattingUtil.formatLongToCompactString(entityIngredient.getCount(), 3);
            var fontRenderer = Minecraft.getInstance().font;
            graphics.drawString(fontRenderer, s, pos.x + size.width - fontRenderer.width(s) - 2, pos.y + size.height - fontRenderer.lineHeight - 2, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (drawHoverOverlay && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(graphics, getPosition().x + 1, getPosition().y + 1, getSize().width - 2, getSize().height - 2, 0x80FFFFFF);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (drawHoverTips && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this) {
            if (gui != null) {
                gui.getModularUIGui().setHoverTooltip(getFullTooltipTexts(), ItemStack.EMPTY, null, null);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1f);
        } else {
            super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderEntityInInventory(GuiGraphics pGuiGraphics, int x, int y, double pScale, Entity entity) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        var offset = (System.currentTimeMillis() % 3600) / 3600f;
        Quaternionf quaternionf = (new Quaternionf()).rotateXYZ(0, (float)Math.PI * 2 * offset, (float)Math.PI);
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(x, y, 0);
        pGuiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling((float)pScale, (float)pScale, (float)(-pScale)));
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

}

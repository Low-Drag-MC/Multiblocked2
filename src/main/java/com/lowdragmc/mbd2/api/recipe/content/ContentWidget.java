package com.lowdragmc.mbd2.api.recipe.content;

import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ContentWidget<T> extends WidgetGroup {
    private final RecipeCapability<T> cap;
    private final Content content;
    private Tag lastContentTag;

    public ContentWidget(int x, int y, RecipeCapability<T> cap, Content content) {
        super(x, y, 20, 20);
        this.cap = cap;
        this.content = content;
        this.lastContentTag = cap.serializer.toNBT(cap.of(content.getContent()));
        var contentWidget = cap.createPreviewWidget(cap.of(content.getContent()));
        contentWidget.setSelfPosition(1, 1);
        addWidget(contentWidget);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        var currentTag = cap.serializer.toNBT(cap.of(content.getContent()));
        if (!currentTag.equals(lastContentTag)) {
            clearAllWidgets();
            lastContentTag = currentTag;
            var contentWidget = cap.createPreviewWidget(cap.of(content.getContent()));
            contentWidget.setSelfPosition(1, 1);
            addWidget(contentWidget);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        content.createOverlay().draw(graphics, mouseX, mouseY, getPositionX() + 1, getPositionY() + 1, 18, 18);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        if (isMouseOver(getPositionX() + 1, getPositionY() + 1, 18, 18, mouseX, mouseY)) {
            List<Component> tooltips = new ArrayList<>();
            if (gui.getModularUIGui().tooltipTexts != null) {
                tooltips.addAll(gui.getModularUIGui().tooltipTexts);
            }
            content.appendTooltip(tooltips);
            if (Editor.INSTANCE instanceof MachineEditor) {
                if (!content.slotName.isEmpty()) {
                    tooltips.add(Component.translatable("mbd2.gui.content.slot_name", content.slotName));
                }
                if (!content.uiName.isEmpty()) {
                    tooltips.add(Component.translatable("mbd2.gui.content.ui_name", content.uiName));
                }
            }
            if (!tooltips.isEmpty()) {
                gui.getModularUIGui().tooltipTexts = tooltips;
            }
        }
    }
}

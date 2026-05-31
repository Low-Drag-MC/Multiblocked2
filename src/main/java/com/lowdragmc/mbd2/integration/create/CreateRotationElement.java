package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.simibubi.create.AllBlocks;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Unified rotation widget. Layout (always present): {@code [cogIcon] [rpmLabel] [stressLabel] [torqueLabel]}.
 *
 * <p>Internal state lives on three {@link BindableValue}{@code <Float>} children — {@link #rpmValue},
 * {@link #stressValue}, {@link #torqueValue}. The trait UI binds a server-side float supplier
 * directly onto these (via {@code element.getRpmValue().bind(...)} etc.). The recipe XEI path
 * calls {@link #setValue(CreateRotation)} which pushes values + uses {@code setDisplay} on
 * the mode-irrelevant labels (and on the torque label when override is disabled).
 *
 * <p>The cog icon and the labels themselves are always added to the tree — visibility is only
 * controlled per-label by the recipe path. The cog spins continuously off the world tick; its
 * rate uses {@code rpm} when nonzero, otherwise {@code stress / max(torque, 4)}.
 */
@LDLRegister(name = "create-rotation-element", group = "widget.container", registry = "ldlib2:ui_element", modID = "create")
public class CreateRotationElement extends UIElement {

    @Getter private final BindableValue<Float> rpmValue = new BindableValue<>(0f);
    @Getter private final BindableValue<Float> stressValue = new BindableValue<>(0f);
    @Getter private final BindableValue<Float> torqueValue = new BindableValue<>(0f);

    @Getter private final UIElement cogIcon;
    @Getter private final Label rpmLabel;
    @Getter private final Label stressLabel;
    @Getter private final Label torqueLabel;

    public CreateRotationElement() {
        layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(16));

        cogIcon = new UIElement()
                .layout(l -> l.heightPercent(100).aspectRatio(1f).marginRight(4));
        if (LDLib2.isClient()) {
            cogIcon.style(s -> s.background(new RotatingBlockTexture(AllBlocks.COGWHEEL::asStack, this::effectiveRpm)));
        }

        rpmLabel = new Label()
                .bindDataSource(SupplierDataSource.of(() -> Component.literal(
                        LocalizationUtils.format("recipe.capability.create_rotation.rpm.unit",
                                rpmValue.getValue()))));
        rpmLabel.layout(l -> l.flex(1).heightPercent(100));
        rpmLabel.textStyle(s -> s.textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.LEFT).fontSize(8f));

        stressLabel = new Label()
                .bindDataSource(SupplierDataSource.of(() -> Component.literal(
                        LocalizationUtils.format("recipe.capability.create_rotation.stress.unit",
                                stressValue.getValue()))));
        stressLabel.layout(l -> l.flex(1).heightPercent(100));
        stressLabel.textStyle(s -> s.textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.LEFT).fontSize(8f));

        torqueLabel = new Label()
                .bindDataSource(SupplierDataSource.of(() -> Component.literal(
                        LocalizationUtils.format("recipe.capability.create_rotation.torque.unit",
                                torqueValue.getValue()))));
        torqueLabel.layout(l -> l.flex(1).heightPercent(100));
        torqueLabel.textStyle(s -> s.textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.LEFT).fontSize(8f));

        // BindableValue is a UIElement; mount the three so their sync values are tracked under us.
        addChild(rpmValue);
        addChild(stressValue);
        addChild(torqueValue);

        addChild(cogIcon);
        addChild(rpmLabel);
        addChild(stressLabel);
        addChild(torqueLabel);
        internalSetup();
    }

    /**
     * Recipe-XEI entry point: push the content's value into the relevant BindableValue and
     * hide labels that don't apply ({@code mode=STRESS} hides the rpm label, {@code mode=RPM}
     * hides the stress label; torque label hidden when override is disabled). The cog icon
     * never hides.
     */
    public CreateRotationElement setValue(@Nullable CreateRotation value) {
        if (value == null) {
            rpmValue.setValue(0f);
            stressValue.setValue(0f);
            torqueValue.setValue(0f);
            rpmLabel.setDisplay(true);
            stressLabel.setDisplay(true);
            torqueLabel.setDisplay(true);
            return this;
        }
        boolean isRpm = value.mode == CreateRotation.Mode.RPM;
        rpmValue.setValue(isRpm ? value.value : 0f);
        stressValue.setValue(isRpm ? 0f : value.value);
        boolean overrideEnabled = value.torqueOverride != null && value.torqueOverride.isEnable();
        torqueValue.setValue(overrideEnabled ? value.torqueOverride.getValue() : 0f);

        rpmLabel.setDisplay(isRpm);
        stressLabel.setDisplay(!isRpm);
        torqueLabel.setDisplay(overrideEnabled);
        return this;
    }

    /**
     * Spin rate used by the cog texture. Prefer the live RPM; if that's zero, derive it from
     * {@code stress / torque}. If torque is also zero, default to 4 (the sensible Create
     * fallback used by small machines so the cog never freezes when the user hasn't bound
     * any value yet).
     */
    private float effectiveRpm() {
        float r = rpmValue.getValue();
        if (r != 0f) return r;
        float t = torqueValue.getValue();
        if (t == 0f) t = 4f;
        return stressValue.getValue() / t;
    }
}

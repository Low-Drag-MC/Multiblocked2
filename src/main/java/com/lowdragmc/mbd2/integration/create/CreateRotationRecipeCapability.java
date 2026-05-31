package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;
import com.simibubi.create.AllBlocks;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Single rotation recipe capability replacing the old {@code create_rpm} and
 * {@code create_stress}. Content is {@link CreateRotation} which carries its own STRESS/RPM
 * mode + optional torque override.
 */
public class CreateRotationRecipeCapability extends RecipeCapability<CreateRotation> {
    @LDLRegister(name = "create_rotation", registry = "mbd2:recipe_capability", modID = "create")
    public static final CreateRotationRecipeCapability CAP = new CreateRotationRecipeCapability();

    protected CreateRotationRecipeCapability() {
        super("create_rotation", SerializerCreateRotation.INSTANCE);
    }

    @Override
    public CreateRotation createDefaultContent() {
        return CreateRotation.stress(128f);
    }

    @Override
    public UIElement createPreview(Supplier<CreateRotation> content) {
        return new UIElement()
                .style(style -> style.background(new ItemStackTexture(AllBlocks.SHAFT.asStack())))
                .layout(layout -> layout.width(18).height(18))
                .addChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() -> {
                            var c = content.get();
                            return Component.literal(((long) of(c).value) + (c.mode == CreateRotation.Mode.RPM ? " r" : " s"));
                        }))
                        .textStyle(textStyle -> textStyle
                                .textAlignVertical(Vertical.BOTTOM)
                                .textAlignHorizontal(Horizontal.RIGHT)
                                .fontSize(4.5f))
                        .layout(layout -> layout.widthPercent(100).heightPercent(100)));
    }

    @Override
    public XEILayoutType xeiLayoutType() {
        return XEILayoutType.BAR;
    }

    @Override
    public UIElement createXEITemplate() {
        var element = new CreateRotationElement();
        element.getLayout().height(16);
        return element;
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof CreateRotationElement rotationElement) {
            rotationElement.setValue(of(content.content));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<CreateRotation> supplier, Consumer<CreateRotation> onUpdate) {
        // value
        var numberConfigurator = new NumberConfigurator("recipe.capability.create_rotation.value",
                () -> supplier.get().value,
                number -> { var c = supplier.get(); c.value = number.floatValue(); onUpdate.accept(c); },
                128f, true).setRange(0, Float.MAX_VALUE);
        father.addConfigurators(numberConfigurator);
        // mode
        father.addConfigurators(EnumAccessor.create(
                "recipe.capability.create_rotation.mode",
                List.of(CreateRotation.Mode.values()),
                () -> supplier.get().mode,
                mode -> { var c = supplier.get(); c.mode = mode; onUpdate.accept(c); },
                CreateRotation.Mode.STRESS,
                false));
        // torque override (toggle + nested float)
        var torqueGroup = new ConfiguratorGroup("config.recipe.create_rotation.torque_override");
        ToggleFloat to = supplier.get().torqueOverride;
        to.buildConfigurator(torqueGroup);
        torqueGroup.addEventListener(Configurator.CHANGE_EVENT, e -> onUpdate.accept(supplier.get()));
        father.addConfigurators(torqueGroup);
    }

    @Override
    public Component getLeftErrorInfo(List<CreateRotation> left) {
        float stressSum = 0f, rpmMax = 0f;
        for (var c : left) {
            if (c.mode == CreateRotation.Mode.STRESS) stressSum += c.value;
            else rpmMax = Math.max(rpmMax, c.value);
        }
        if (stressSum > 0f && rpmMax > 0f) {
            return Component.literal(stressSum + " stress / " + rpmMax + " rpm");
        }
        if (stressSum > 0f) return Component.literal(stressSum + " stress");
        return Component.literal(rpmMax + " rpm");
    }
}

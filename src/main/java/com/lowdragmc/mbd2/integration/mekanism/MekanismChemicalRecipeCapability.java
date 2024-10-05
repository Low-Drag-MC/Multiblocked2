package com.lowdragmc.mbd2.integration.mekanism;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SearchComponentWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.*;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.registries.MekanismGases;
import mekanism.common.registries.MekanismInfuseTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MekanismChemicalRecipeCapability<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends RecipeCapability<STACK> {
    public static final MekanismChemicalRecipeCapability<Gas, GasStack> CAP_GAS =
            new MekanismChemicalRecipeCapability<>("mek_gas",
                    MekanismAPI.EMPTY_GAS,
                    MekanismGases.HYDROGEN::getChemical,
                    MekanismAPI::gasRegistry,
                    GasStack::new,
                    ChemicalTankBuilder.GAS,
                    GasStack::readFromPacket,
                    ChemicalTankWidget.Gas::new);

    public static final MekanismChemicalRecipeCapability<InfuseType, InfusionStack> CAP_INFUSE =
            new MekanismChemicalRecipeCapability<>("mek_infuse",
                    MekanismAPI.EMPTY_INFUSE_TYPE,
                    MekanismInfuseTypes.DIAMOND::getChemical,
                    MekanismAPI::infuseTypeRegistry,
                    InfusionStack::new,
                    ChemicalTankBuilder.INFUSION,
                    InfusionStack::readFromPacket,
                    ChemicalTankWidget.Infuse::new);

    public static final MekanismChemicalRecipeCapability<Pigment, PigmentStack> CAP_PIGMENT =
            new MekanismChemicalRecipeCapability<>("mek_pigment",
                    MekanismAPI.EMPTY_PIGMENT,
                    null,
                    MekanismAPI::pigmentRegistry,
                    PigmentStack::new,
                    ChemicalTankBuilder.PIGMENT,
                    PigmentStack::readFromPacket,
                    ChemicalTankWidget.Pigment::new);

    public static final MekanismChemicalRecipeCapability<Slurry, SlurryStack> CAP_SLURRY =
            new MekanismChemicalRecipeCapability<>("mek_slurry",
                    MekanismAPI.EMPTY_SLURRY,
                    null,
                    MekanismAPI::slurryRegistry,
                    SlurryStack::new,
                    ChemicalTankBuilder.SLURRY,
                    SlurryStack::readFromPacket,
                    ChemicalTankWidget.Slurry::new);

    public final CHEMICAL empty;
    @Nullable
    public final Supplier<CHEMICAL> defaultChemical;
    public final Supplier<IForgeRegistry<CHEMICAL>> registry;
    public final BiFunction<CHEMICAL, Long, STACK> createStack;
    public final ChemicalTankBuilder<CHEMICAL, STACK, ? extends IChemicalTank<CHEMICAL, STACK>> tankBuilder;
    public final Supplier<ChemicalTankWidget<CHEMICAL, STACK>> createTankWidget;

    protected MekanismChemicalRecipeCapability(String name,
                                               CHEMICAL empty,
                                               @Nullable Supplier<CHEMICAL> defaultChemical,
                                               Supplier<IForgeRegistry<CHEMICAL>> registry,
                                               BiFunction<CHEMICAL, Long, STACK> createStack,
                                               ChemicalTankBuilder<CHEMICAL, STACK, ? extends IChemicalTank<CHEMICAL, STACK>> tankBuilder,
                                               Function<FriendlyByteBuf, STACK> readFromBuffer, Supplier<ChemicalTankWidget<CHEMICAL, STACK>> createTankWidget) {
        super(name, new ChemicalStackIContentSerializer<>(readFromBuffer, empty, registry, createStack));
        this.empty = empty;
        this.defaultChemical = defaultChemical;
        this.registry = registry;
        this.createStack = createStack;
        this.tankBuilder = tankBuilder;
        this.createTankWidget = createTankWidget;
    }

    public CHEMICAL createDefaultChemical() {
        return defaultChemical == null ? registry.get().getValues().stream().findAny().orElse(empty) : defaultChemical.get();
    }

    @Override
    public STACK createDefaultContent() {
        return createStack.apply(createDefaultChemical(), 1000L);
    }

    @Override
    public Widget createPreviewWidget(STACK content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        var tankWidget = createTankWidget.get();
        var ingredient = of(content);
        if (tankBuilder.create(ingredient.getAmount(), null) instanceof IChemicalHandler handler) {
            handler.insertChemical(ingredient, Action.EXECUTE);
            tankWidget.setChemicalTank(handler, 0);
        }
        previewGroup.addWidget(tankWidget);
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        var tankWidget = createTankWidget.get();
        tankWidget.initTemplate();
        tankWidget.setSize(new Size(20, 58));
        tankWidget.setOverlay(new ResourceTexture("mbd2:textures/gui/fluid_tank_overlay.png"));
        tankWidget.setShowAmount(false);
        return tankWidget;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof ChemicalTankWidget tankWidget) {
            var ingredient = of(content.content);
            if (tankBuilder.create(ingredient.getAmount(), null) instanceof IChemicalHandler handler) {
                handler.insertChemical(ingredient, Action.EXECUTE);
                tankWidget.setChemicalTank(handler, 0);
            }
            if (tankWidget.getOverlay() == null || tankWidget.getOverlay() == IGuiTexture.EMPTY) {
                tankWidget.setOverlay(content.createOverlay());
            } else {
                var groupTexture = new GuiTextureGroup(tankWidget.getOverlay(), content.createOverlay());
                tankWidget.setOverlay(groupTexture);
            }
            tankWidget.setIngredientIO(ingredientIO);
            tankWidget.setAllowClickDrained(false);
            tankWidget.setAllowClickFilled(false);
            tankWidget.setXEIChance(content.chance);
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<STACK> supplier, Consumer<STACK> onUpdate) {
        var searchHandler = new SearchComponentWidget.IWidgetSearch<CHEMICAL>() {

            @Override
            public String resultDisplay(CHEMICAL value) {
                if (value.isEmptyType()) {
                    return "empty";
                }
                var key = registry.get().getKey(value);
                return key == null ? "empty" : key.toString();
            }

            @Override
            public void selectResult(CHEMICAL value) {
                onUpdate.accept(createStack.apply(value, Math.max(1, supplier.get().getAmount())));
            }

            @Override
            public void search(String word, Consumer<CHEMICAL> find) {
                var lowerCase = word.toLowerCase();
                if ("empty".contains(lowerCase)) {
                    find.accept(empty);
                    return;
                }
                var words = lowerCase.split(" ");
                for (var entry : registry.get().getEntries()) {
                    var key = entry.getKey();
                    var chemical = entry.getValue();
                    for (String s : words) {
                        if (key.toString().toLowerCase().contains(s) ||
                                LocalizationUtils.format(chemical.getTranslationKey()).toLowerCase().contains(s)) {
                            find.accept(chemical);
                            break;
                        }
                    }
                }
            }
        };
        var typeGroup = new WidgetGroup(0, 0, 180, 10);
        typeGroup.addWidget(new ImageWidget(0, 0, 180, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        var searchComponent = new SearchComponentWidget<>(3, 0, 180 - 3, 10, searchHandler);
        searchComponent.setShowUp(true);
        searchComponent.setCapacity(5);
        var textFieldWidget = searchComponent.textFieldWidget;
        textFieldWidget.setClientSideWidget();
        textFieldWidget.setCurrentString(searchHandler.resultDisplay(supplier.get().getType()));
        textFieldWidget.setBordered(false);
        typeGroup.addWidget(searchComponent);
        father.addConfigurators(new WrapperConfigurator("recipe.capability.mek_chemical.type", typeGroup));
        father.addConfigurators(new NumberConfigurator("recipe.capability.mek_chemical.amount",
                () -> supplier.get().getAmount(),
                number -> onUpdate.accept(createStack.apply(supplier.get().getType(), number.longValue())), 1, true).setRange(1, Long.MAX_VALUE));
    }

    private record ChemicalStackIContentSerializer<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>>(
            Function<FriendlyByteBuf, STACK> readFromBuffer, CHEMICAL empty,
            Supplier<IForgeRegistry<CHEMICAL>> registry,
            BiFunction<CHEMICAL, Long, STACK> createStack) implements IContentSerializer<STACK> {

        @Override
        public void toNetwork(FriendlyByteBuf buf, STACK content) {
            content.writeToPacket(buf);
        }

        @Override
        public STACK fromNetwork(FriendlyByteBuf buf) {
            return readFromBuffer.apply(buf);
        }

        @Override
        public STACK fromJson(JsonElement json) {
            ResourceLocation type = new ResourceLocation(json.getAsJsonObject().get("type").getAsString());
            long amount = json.getAsJsonObject().get("amount").getAsLong();
            CHEMICAL chemical = ChemicalUtils.readChemicalFromRegistry(type, empty, registry.get());
            return createStack.apply(chemical, amount);
        }

        @Override
        public JsonElement toJson(STACK content) {
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("type", content.getType().getRegistryName().toString());
            jsonObj.addProperty("amount", content.getAmount());
            return jsonObj;
        }

        public STACK of(Object o) {
            if (o instanceof ChemicalStack<?> chemicalStack && ChemicalType.getTypeFor(chemicalStack.getType()) == ChemicalType.getTypeFor(empty)) {
                return (STACK) chemicalStack;
            } else if (o instanceof CharSequence) {
                String str = o.toString();
                // parse "Nx ID"

                int x = str.indexOf('x');
                if (x > 0 && x < str.length() - 2 && str.charAt(x + 1) == ' ') {
                    try {
                        var chemical = registry.get().getValue(new ResourceLocation(str.substring(x + 2)));
                        var amount = Long.parseLong(str.substring(0, x));
                        return createStack.apply(chemical, amount);
                    } catch (Exception ignore) {
                        throw new IllegalStateException("Invalid chemical input: " + str);
                    }
                } else {
                    return createStack.apply(registry.get().getValue(new ResourceLocation(str)), 1L);
                }
            }
            return (STACK) empty.getStack(0);
        }

        @Override
        public STACK copyInner(STACK content) {
            return (STACK) content.copy();
        }

        @Override
        public STACK copyWithModifier(STACK content, ContentModifier modifier) {
            STACK copy = (STACK) content.copy();
            copy.setAmount(modifier.apply(copy.getAmount()).longValue());
            return copy;
        }
    }
}

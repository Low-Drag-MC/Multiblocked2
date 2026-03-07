//package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;
//
//import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.LDLRegister;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.NumberRange;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ResourceTexture;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.lowdraglib2.jei.IngredientIO;
//import com.lowdragmc.lowdraglib2.utils.Position;
//import com.lowdragmc.lowdraglib2.utils.Size;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.machine.IMachine;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import com.lowdragmc.mbd2.common.trait.ITrait;
//import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
//import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
//import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
//import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
//import com.lowdragmc.mbd2.utils.WidgetUtils;
//import lombok.Getter;
//import lombok.Setter;
//import mekanism.api.chemical.Chemical;
//import mekanism.api.chemical.ChemicalStack;
//import mekanism.api.chemical.ChemicalTags;
//import mekanism.api.chemical.gas.GasStack;
//import mekanism.api.chemical.infuse.InfusionStack;
//import mekanism.api.chemical.pigment.PigmentStack;
//import mekanism.api.chemical.slurry.SlurryStack;
//import mekanism.common.registries.MekanismBlocks;
//
//public abstract class ChemicalTankCapabilityTraitDefinition<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends SimpleCapabilityTraitDefinition {
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.fluid_tank.tank_size", tips = "config.definition.trait.fluid_tank.tank_size.tooltip")
//    @ConfigNumber(range = {1, Integer.MAX_VALUE})
//    private int tankSize = 1;
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.fluid_tank.capacity")
//    @ConfigNumber(range = {1, Integer.MAX_VALUE})
//    private int capacity = 1000;
//    @Getter
//    @Configurable(name = "config.definition.trait.fluid_tank.filter", subConfigurable = true, tips = "config.definition.trait.fluid_tank.filter.tooltip")
//    private final ChemicalFilterSettings<CHEMICAL, STACK> chemicalFilterSettings;
//    @Getter
//    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.fluid_tank.auto_io.tooltip")
//    private final ToggleAutoIO autoIO = new ToggleAutoIO();
//    @Configurable(name = "config.definition.trait.fluid_tank.fancy_renderer", subConfigurable = true,
//            tips = {"config.definition.trait.fluid_tank.fancy_renderer.tooltip.0", "config.definition.trait.fluid_tank.fancy_renderer.tooltip.1"})
//    private final ChemicalFancyRendererSettings fancyRendererSettings = new ChemicalFancyRendererSettings(this);
//
//    @Getter
//    public final MekanismChemicalRecipeCapability<CHEMICAL, STACK> recipeCapability;
//
//    protected ChemicalTankCapabilityTraitDefinition(
//            MekanismChemicalRecipeCapability<CHEMICAL, STACK> recipeCapability,
//            ChemicalTags<CHEMICAL> chemicalTags) {
//        this.recipeCapability = recipeCapability;
//        this.chemicalFilterSettings = new ChemicalFilterSettings<>(chemicalTags, recipeCapability);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(MekanismBlocks.ADVANCED_CHEMICAL_TANK.getItemStack());
//    }
//
//    @Override
//    public IRenderer getBESRenderer(IMachine machine) {
//        return fancyRendererSettings.getFancyRenderer(machine);
//    }
//
//    @Override
//    public void createTraitUITemplate(WidgetGroup ui) {
//        var prefix = uiPrefixName();
//        for (var i = 0; i < this.tankSize; i++) {
//            var tankWidget = recipeCapability.createTankWidget.get();
//            tankWidget.initTemplate();
//            tankWidget.setSelfPosition(new Position(10 + i * 20, 10));
//            tankWidget.setSize(new Size(20, 58));
//            tankWidget.setOverlay(new ResourceTexture("mbd2:textures/gui/fluid_tank_overlay.png"));
//            tankWidget.setId(prefix + "_" + i);
//            tankWidget.setShowAmount(false);
//            ui.addWidget(tankWidget);
//        }
//    }
//
//    @Override
//    public void initTraitUI(ITrait trait, WidgetGroup group) {
//        if (trait instanceof ChemicalTankCapabilityTrait fluidTankTrait) {
//            var prefix = uiPrefixName();
//            var guiIO = getGuiIO();
//            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
//            var allowClickDrained = guiIO == IO.BOTH || guiIO == IO.OUT;
//            var allowClickFilled = guiIO == IO.BOTH || guiIO == IO.IN;
//            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), recipeCapability.createTankWidget.get().getClass(), tankWidget -> {
//                var index = WidgetUtils.widgetIdIndex(tankWidget);
//                if (index >= 0 && index < fluidTankTrait.storages.length) {
//                    tankWidget.setChemicalTank(fluidTankTrait.storages[index]);
//                    tankWidget.setIngredientIO(ingredientIO);
//                    tankWidget.setAllowClickDrained(allowClickDrained);
//                    tankWidget.setAllowClickFilled(allowClickFilled);
//                }
//            });
//        }
//    }
//
//    @LDLRegister(name = "mek_gas_container", group = "trait", modID = "mekanism")
//    public static class Gas extends ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.gas.Gas, GasStack> {
//        public Gas() {
//            super(MekanismChemicalRecipeCapability.CAP_GAS, ChemicalTags.GAS);
//        }
//
//        @Override
//        public SimpleCapabilityTrait createTrait(MBDMachine machine) {
//            return new ChemicalTankCapabilityTrait.Gas(machine, this);
//        }
//    }
//
//    @LDLRegister(name = "mek_infuse_container", group = "trait", modID = "mekanism")
//    public static class Infuse extends ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.infuse.InfuseType, InfusionStack> {
//        public Infuse() {
//            super(MekanismChemicalRecipeCapability.CAP_INFUSE, ChemicalTags.INFUSE_TYPE);
//        }
//
//        @Override
//        public SimpleCapabilityTrait createTrait(MBDMachine machine) {
//            return new ChemicalTankCapabilityTrait.Infuse(machine, this);
//        }
//
//    }
//
//    @LDLRegister(name = "mek_pigment_container", group = "trait", modID = "mekanism")
//    public static class Pigment extends ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.pigment.Pigment, PigmentStack> {
//        public Pigment() {
//            super(MekanismChemicalRecipeCapability.CAP_PIGMENT, ChemicalTags.PIGMENT);
//        }
//
//        @Override
//        public SimpleCapabilityTrait createTrait(MBDMachine machine) {
//            return new ChemicalTankCapabilityTrait.Pigment(machine, this);
//        }
//
//    }
//
//    @LDLRegister(name = "mek_slurry_container", group = "trait", modID = "mekanism")
//    public static class Slurry extends ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.slurry.Slurry, SlurryStack> {
//        public Slurry() {
//            super(MekanismChemicalRecipeCapability.CAP_SLURRY, ChemicalTags.SLURRY);
//        }
//
//        @Override
//        public SimpleCapabilityTrait createTrait(MBDMachine machine) {
//            return new ChemicalTankCapabilityTrait.Slurry(machine, this);
//        }
//
//    }
//}

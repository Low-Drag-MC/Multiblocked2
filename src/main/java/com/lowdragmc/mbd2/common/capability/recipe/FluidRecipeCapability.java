package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib.gui.editor.accessors.CompoundTagAccessor;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.utils.CycleFluidStorage;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.TagOrCycleFluidTransfer;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerFluidIngredient;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote FluidRecipeCapability
 */
public class FluidRecipeCapability extends RecipeCapability<FluidIngredient> {

    public static final String FLUID_TYPE = "recipe.capability.fluid.ingredient.values.fluid";
    public static final String TAG_TYPE = "recipe.capability.fluid.ingredient.values.tag";

    public final static FluidRecipeCapability CAP = new FluidRecipeCapability();

    protected FluidRecipeCapability() {
        super("fluid", SerializerFluidIngredient.INSTANCE);
    }

    @Override
    public FluidIngredient createDefaultContent() {
        return FluidIngredient.of(1000, Fluids.WATER);
    }

    @Override
    public Widget createPreviewWidget(FluidIngredient content) {
        var storage = new CycleFluidStorage(content.getAmount(), Arrays.stream(content.getStacks()).toList());
        return new TankWidget(storage, 0, 0, false, false).setDrawHoverOverlay(false);
    }

    @Override
    public Widget createXEITemplate() {
        var tankWidget = new TankWidget();
        tankWidget.initTemplate();
        tankWidget.setSize(new Size(20, 58));
        tankWidget.setOverlay(new ResourceTexture("mbd2:textures/gui/fluid_tank_overlay.png"));
        tankWidget.setShowAmount(false);
        return tankWidget;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof TankWidget tankWidget) {
            var fluidIngredient = of(content.content);
            Either<List<Pair<TagKey<Fluid>, Long>>, List<FluidStack>> either = null;
            // if all fluid tags
            if (Arrays.stream(fluidIngredient.values).allMatch(FluidIngredient.TagValue.class::isInstance)) {
                either = Either.left(Arrays.stream(fluidIngredient.values)
                        .map(FluidIngredient.TagValue.class::cast)
                        .map(FluidIngredient.TagValue::getTag)
                        .map(tagValue -> new Pair<>(tagValue, fluidIngredient.getAmount())).toList());
            }
            if (either == null) {
                either = Either.right(List.of(fluidIngredient.getStacks()));
            }
            if (tankWidget.getOverlay() == null || tankWidget.getOverlay() == IGuiTexture.EMPTY) {
                tankWidget.setOverlay(content.createOverlay());
            } else {
                var groupTexture = new GuiTextureGroup(tankWidget.getOverlay(), content.createOverlay());
                tankWidget.setOverlay(groupTexture);
            }
            tankWidget.setFluidTank(new TagOrCycleFluidTransfer(List.of(either)), 0);
            tankWidget.setIngredientIO(ingredientIO);
            tankWidget.setAllowClickDrained(false);
            tankWidget.setAllowClickFilled(false);
            tankWidget.setXEIChance(content.chance);
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<FluidIngredient> supplier, Consumer<FluidIngredient> onUpdate) {
        // sized ingredient amount
        father.addConfigurators(new NumberConfigurator("recipe.capability.fluid.ingredient.amount",
                () -> supplier.get().getAmount(),
                number -> {
                    var amount = number.intValue();
                    onUpdate.accept(supplier.get().copy(amount));
                }, 1, true).setRange(1, Integer.MAX_VALUE));
        // fluid ingredient
        var valuesGroup = new ArrayConfiguratorGroup<>("recipe.capability.fluid.ingredient.candidates", false,
                () -> Arrays.stream(supplier.get().values).collect(Collectors.toList()), (getter, setter) -> {
            // check values type
            return new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.values.type", false, getter, setter,
                    new FluidIngredient.FluidValue(Fluids.WATER), true,
                    List.of(
                            // values candidates
                            new FluidIngredient.FluidValue(Fluids.WATER),
                            new FluidIngredient.TagValue(FluidTags.LAVA)),
                    value -> {
                        if (value instanceof FluidIngredient.FluidValue) {
                            return FLUID_TYPE;
                        } else if (value instanceof FluidIngredient.TagValue) {
                            return TAG_TYPE;
                        }
                        return FLUID_TYPE;
                    }, (value, valueGroup) -> {
                // preview slot
                var fluidStorage = new CycleFluidStorage(1, value.getStacks().stream().map(fluid -> FluidStack.create(fluid, 1)).toList());
                var tank = new TankWidget(fluidStorage, 0, 0, false, false);
                tank.setBackground(TankWidget.FLUID_SLOT_TEXTURE);
                tank.setShowAmount(false);
                tank.setClientSideWidget();

                if (value instanceof FluidIngredient.FluidValue fluidValue) {
                    // fluid value
                    valueGroup.addConfigurators(new FluidConfigurator(FLUID_TYPE,
                            fluidValue::getFluid,
                            fluid -> {
                                fluidValue.setFluid(fluid);
                                fluidStorage.updateStacks(value.getStacks().stream().map(f -> FluidStack.create(f, 1)).toList());
                                setter.accept(value);
                            },
                            Fluids.WATER, true));
                } else if (value instanceof FluidIngredient.TagValue tagValue) {
                    // tag value
                    valueGroup.addConfigurators(new SearchComponentConfigurator<>(TAG_TYPE,
                            () -> tagValue.getTag().location(), tagKey -> {
                        tagValue.setTag(FluidTags.create(tagKey));
                        fluidStorage.updateStacks(value.getStacks().stream().map(f -> FluidStack.create(f, 1)).toList());
                        setter.accept(value);
                    }, FluidTags.LAVA.location(), true, (word, find) -> {
                        var tags = ForgeRegistries.FLUIDS.tags();
                        if (tags == null) return;
                        for (var tag : tags) {
                            if (Thread.currentThread().isInterrupted()) return;
                            var tagKey = tag.getKey().location();
                            if (tagKey.toString().toLowerCase().contains(word.toLowerCase())) {
                                find.accept(tagKey);
                            }
                        }}, ResourceLocation::toString));
                }
                valueGroup.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", tank));
            });
        }, true);
        valuesGroup.setAddDefault(() -> new FluidIngredient.FluidValue(Fluids.WATER));
        valuesGroup.setOnAdd(value -> {
            var fluidIngredient = supplier.get();
            var values = fluidIngredient.values;
            var newValues = Arrays.copyOf(values, values.length + 1);
            newValues[values.length] = value;
            fluidIngredient.values = newValues;
            fluidIngredient.stacks = null;
        });
        valuesGroup.setOnRemove(value -> {
            var fluidIngredient = supplier.get();
            var values = fluidIngredient.values;
            var newValues = Arrays.stream(values).filter(v -> v != value).toArray(FluidIngredient.Value[]::new);
            newValues[values.length] = value;
            fluidIngredient.values = newValues;
            fluidIngredient.stacks = null;
        });
        valuesGroup.setOnUpdate(values -> {
            var fluidIngredient = supplier.get();
            fluidIngredient.values = values.toArray(FluidIngredient.Value[]::new);
            fluidIngredient.stacks = null;
        });
        father.addConfigurators(valuesGroup);
        // fluid nbt
        try {
            father.addConfigurators(new CompoundTagAccessor().create("ldlib.gui.editor.configurator.nbt",
                    () -> Optional.ofNullable(supplier.get().getNbt()).orElseGet(CompoundTag::new),
                    tag -> {
                        var fluidIngredient = supplier.get();
                        var newTag = tag.isEmpty() ? null : tag;
                        if (Objects.equals(newTag, fluidIngredient.getNbt())) return;
                        fluidIngredient.setNbt(newTag);
                        onUpdate.accept(fluidIngredient);
                    }, false, RecipeCapability.class.getField("name")));
        } catch (Exception ignored) {}
    }

    @Override
    public Component getLeftErrorInfo(List<FluidIngredient> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            var fluidIngredient = left.get(i);
            result.append(fluidIngredient.getAmount() + "x ");
            var stacks = fluidIngredient.getStacks();
            if (stacks.length > 0) {
                result.append(stacks[0].getDisplayName());
            } else {
                result.append("Unknown");
            }
            if (fluidIngredient.getNbt() != null) {
                result.append(" with NBT");
                result.append(fluidIngredient.getNbt().toString());
            }
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }
}

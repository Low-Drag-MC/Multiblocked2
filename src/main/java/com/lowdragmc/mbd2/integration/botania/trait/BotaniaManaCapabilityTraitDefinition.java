package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BotaniaManaCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<ManaReceiver, Direction> {
    public static BlockCapability<ManaReceiver, Direction> manaReceiverCapability() {
        return BotaniaForgeCapabilities.MANA_RECEIVER;
    }

    public static BlockCapability<SparkAttachable, Direction> sparkAttachableCapability() {
        return BotaniaForgeCapabilities.SPARK_ATTACHABLE;
    }

    @LDLRegister(name = "botania_mana_storage", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "botania")
    public static final SimpleCapabilityTraitDefinition.Type<ManaReceiver, Direction, BotaniaManaCapabilityTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("botania_mana_storage", "trait") {
                @Override
                public BotaniaManaCapabilityTraitDefinition createDefinition() {
                    return new BotaniaManaCapabilityTraitDefinition();
                }

                @Override
                protected BlockCapability<ManaReceiver, Direction> getCapability() {
                    return manaReceiverCapability();
                }

                @Override
                protected ManaReceiver merge(List<ManaReceiver> contents) {
                    return contents.isEmpty() ? null : new ManaPoolList(contents.toArray(ManaReceiver[]::new));
                }

                @Override
                public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
                    super.registerCapabilities(definition, event);
                    event.registerBlockEntity(sparkCapability(), definition.blockEntityType(), (be, context) -> {
                        if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                            SparkAttachable own = getSparkContent(machine);
                            if (machine instanceof MBDPartMachine part) {
                                SparkAttachable proxied = getProxiedSparkContent(part);
                                return mergeSpark(own, proxied);
                            }
                            return own;
                        }
                        return null;
                    });
                }

                @Override
                @SuppressWarnings("unchecked")
                public void registerGlobalCapabilities(RegisterCapabilitiesEvent event) {
                    super.registerGlobalCapabilities(event);
                    event.registerBlockEntity(sparkCapability(), (BlockEntityType<ProxyPartBlockEntity>) ProxyPartBlockEntity.TYPE(),
                            (be, context) -> getProxyPartSparkContent(be));
                }

                private BlockCapability<SparkAttachable, Direction> sparkCapability() {
                    return sparkAttachableCapability();
                }

                private SparkAttachable mergeSpark(@Nullable SparkAttachable a, @Nullable SparkAttachable b) {
                    return a == null ? b : a;
                }

                private SparkAttachable getSparkContent(MBDMachine machine) {
                    List<SparkAttachable> contents = new ArrayList<>();
                    collectSparkContents(machine, null, contents);
                    return contents.isEmpty() ? null : contents.getFirst();
                }

                private SparkAttachable getProxiedSparkContent(MBDPartMachine part) {
                    var partSettings = part.getDefinition().partSettings();
                    List<ConfigPartSettings.ProxyCapability> staticProxies = partSettings == null ? List.of() : partSettings.proxyControllerCapabilities();
                    var controllers = part.getControllers();
                    if (controllers.isEmpty()) return null;
                    Direction partFront = part.getFrontFacing().orElse(Direction.NORTH);
                    List<SparkAttachable> contents = new ArrayList<>();
                    for (var controller : controllers) {
                        if (!(controller instanceof MBDMultiblockMachine mbdController)) continue;
                        collectProxiedSparkContents(mbdController, staticProxies, partFront, contents);
                        collectProxiedSparkContents(mbdController, part.getPredicateProxyCapabilities(mbdController.getPos()), partFront, contents);
                    }
                    return contents.isEmpty() ? null : contents.getFirst();
                }

                private SparkAttachable getProxyPartSparkContent(ProxyPartBlockEntity be) {
                    var caps = be.getProxyCapabilities();
                    if (caps.isEmpty() || be.getLevel() == null || be.getControllerPos() == null) return null;
                    var machine = IMachine.ofMachine(be.getLevel(), be.getControllerPos()).orElse(null);
                    if (!(machine instanceof MBDMachine controller)) return null;
                    Direction front = controller.getFrontFacing().orElse(Direction.NORTH);
                    List<SparkAttachable> contents = new ArrayList<>();
                    collectProxiedSparkContents(controller, caps, front, contents);
                    return contents.isEmpty() ? null : contents.getFirst();
                }

                private void collectProxiedSparkContents(MBDMachine controller, List<ConfigPartSettings.ProxyCapability> proxies, Direction front, List<SparkAttachable> out) {
                    if (proxies.isEmpty()) return;
                    for (var proxy : proxies) {
                        IO io = proxy.capabilityIO().getIO(front, null);
                        if (io == IO.NONE) continue;
                        String filter = proxy.traitNameFilter();
                        collectSparkContents(controller, filter, out);
                    }
                }

                private void collectSparkContents(MBDMachine machine, @Nullable String filter, List<SparkAttachable> out) {
                    for (var trait : machine.getAdditionalTraits()) {
                        if (trait instanceof BotaniaManaCapabilityTrait manaTrait && manaTrait.getDefinition().type() == this) {
                            if (filter != null && !filter.isEmpty() && !trait.getDefinition().getName().contains(filter)) continue;
                            if (manaTrait.getCapabilityIO(null) != IO.NONE) out.add(manaTrait.storage);
                        }
                    }
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.botania_mana_storage.capacity")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int capacity = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.botania_mana_storage.can_attach_spark",
            tips = "config.definition.trait.botania_mana_storage.can_attach_spark.tooltip")
    private boolean canAttachSpark = true;
    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.botania_mana_storage.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Configurable(name = "config.definition.trait.botania_mana_storage.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.botania_mana_storage.fancy_renderer.tooltip")
    private final BotaniaManaFancyRendererSettings fancyRendererSettings = new BotaniaManaFancyRendererSettings(this);

    @Override
    public BotaniaManaCapabilityTrait createTrait(MBDMachine machine) {
        return new BotaniaManaCapabilityTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(BotaniaBlocks.manaPool.asItem());
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var progress = new ProgressBar();
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(BotaniaManaRecipeCapability.HUD_BACKGROUND);
        progress.bar.getStyle().background(BotaniaManaRecipeCapability.HUD_BAR.copy().setColor(ColorPattern.LIGHT_BLUE.color));
        progress.setProgress(1f);
        progress.label.setText("0/0 mana");
        progress.setId(uiId());
        progress.layout(layout -> layout.height(14));
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof BotaniaManaCapabilityTrait manaTrait) {
            ui.selectId(uiId(), ProgressBar.class).forEach(energyBar -> {
                energyBar.bind(DataBindingBuilder.floatValS2C(() -> {
                    var max = manaTrait.storage.getMaxMana();
                    return max > 0 ? (float) manaTrait.storage.getCurrentMana() / max : 0f;
                }).build());
                var stored = new AtomicInteger(manaTrait.storage.getCurrentMana());
                var maxStored = new AtomicInteger(manaTrait.storage.getMaxMana());
                var storedSync = DataBindingBuilder.intValS2C(manaTrait.storage::getCurrentMana)
                        .remoteSetter(stored::set)
                        .build();
                var maxSync = DataBindingBuilder.intValS2C(manaTrait.storage::getMaxMana)
                        .remoteSetter(maxStored::set)
                        .build();
                energyBar.addSyncValue(storedSync.getSyncValue());
                energyBar.addSyncValue(maxSync.getSyncValue());
                energyBar.label.bindDataSource(SupplierDataSource.of(() -> Component.literal(stored.get() + "/" + maxStored.get() + " mana")));
                energyBar.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.literal(LocalizationUtils.format(
                            "config.definition.trait.botania_mana_storage.ui_container_hover",
                            stored.get(), maxStored.get()))), null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }
}

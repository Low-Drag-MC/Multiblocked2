package com.lowdragmc.mbd2.api.pattern.predicates;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@LDLRegister(name = "partial_state", registry = "mbd2:pattern_predicate", group = "predicate")
@NoArgsConstructor
public class PredicatePartialState extends PatternPredicate {
    @Persisted
    protected Block block = Blocks.RAIL;
    @Persisted
    protected PropertyRequirement[] requirements = new PropertyRequirement[] {new PropertyRequirement("shape", "north_south")};

    public PredicatePartialState(Block block, Map<String, String> requirements) {
        this(block, requirementsFromMap(requirements));
    }

    public PredicatePartialState(Block block, PropertyRequirement... requirements) {
        this.block = block;
        this.requirements = requirements == null ? new PropertyRequirement[0] : requirements;
        buildPredicate();
    }

    public PredicatePartialState(BlockState referenceState, Property<?>... properties) {
        this.block = referenceState == null ? Blocks.BARRIER : referenceState.getBlock();
        this.requirements = Arrays.stream(properties == null ? new Property<?>[0] : properties)
                .filter(Objects::nonNull)
                .filter(property -> referenceState != null && hasProperty(referenceState, property))
                .map(property -> requirementFromState(referenceState, property))
                .toArray(PropertyRequirement[]::new);
        buildPredicate();
    }

    @Override
    public PatternPredicate buildPredicate() {
        block = safeBlock();
        requirements = Arrays.stream(safeRequirements()).filter(Objects::nonNull).toArray(PropertyRequirement[]::new);
        var resolved = resolveRequirements(block, requirements);
        predicate = state -> resolved.valid() && matches(state.getBlockState(), block, resolved.requirements());
        candidates = Suppliers.memoize(() -> new BlockInfo[] {BlockInfo.fromBlockState(candidateState(block, resolved))});
        return super.buildPredicate();
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var group = new ConfiguratorGroup("config.predicate.partial_state", false);
        var lastBlock = new Block[] {safeBlock()};
        var requirementsContainer = new ConfiguratorGroup("", false) {
            @Override
            public void screenTick() {
                super.screenTick();
                var currentBlock = safeBlock();
                if (currentBlock != lastBlock[0]) {
                    lastBlock[0] = currentBlock;
                    sanitizeRequirementsForCurrentBlock();
                    rebuildRequirementsConfigurator(this);
                    buildPredicate();
                }
            }
        }.hideTitle();
        group.addConfigurators(
                new RegistrySearchComponent.Block("config.predicate.partial_state.block",
                        this::safeBlock,
                        value -> {
                            block = value == null ? Blocks.BARRIER : value;
                            lastBlock[0] = block;
                            sanitizeRequirementsForCurrentBlock();
                            rebuildRequirementsConfigurator(requirementsContainer);
                            buildPredicate();
                        },
                        Blocks.RAIL,
                        true),
                requirementsContainer);
        father.addConfigurators(group);
        rebuildRequirementsConfigurator(requirementsContainer);
    }

    private void rebuildRequirementsConfigurator(ConfiguratorGroup group) {
        group.removeAllConfigurators();
        var requirementsConfigurator = new ArrayConfiguratorGroup<>("config.predicate.partial_state.properties", false,
                () -> Arrays.stream(safeRequirements()).toList(),
                this::createRequirementConfigurator,
                true);
        requirementsConfigurator.setAddDefault(this::defaultRequirement);
        requirementsConfigurator.setOnUpdate(list -> {
            requirements = list.stream().filter(Objects::nonNull).toArray(PropertyRequirement[]::new);
            buildPredicate();
        });
        group.addConfigurators(requirementsConfigurator);
    }

    private Configurator createRequirementConfigurator(Supplier<PropertyRequirement> getter, Consumer<PropertyRequirement> setter) {
        var propertyNames = propertyNames(safeBlock());
        if (propertyNames.isEmpty()) {
            return new StringConfigurator("config.predicate.partial_state.property",
                    () -> getter.get() == null ? "" : getter.get().property(),
                    value -> setter.accept(new PropertyRequirement(Objects.toString(value, ""), "")),
                    "",
                    true);
        }

        var defaultProperty = propertyNames.getFirst();
        return new ConfiguratorSelectorConfigurator<>("config.predicate.partial_state.property",
                () -> validPropertyName(safeRequirement(getter.get()).property(), propertyNames, defaultProperty),
                propertyName -> setter.accept(new PropertyRequirement(propertyName, defaultValueName(safeBlock(), propertyName))),
                defaultProperty,
                true,
                propertyNames,
                Function.identity(),
                (propertyName, container) -> {
                    var valueNames = propertyValueNames(safeBlock(), propertyName);
                    if (valueNames.isEmpty()) return;
                    var defaultValue = valueNames.getFirst();
                    container.addConfigurator(new SelectorConfigurator<>("config.predicate.partial_state.value",
                            () -> validPropertyValue(safeRequirement(getter.get()).value(), valueNames, defaultValue),
                            valueName -> setter.accept(new PropertyRequirement(propertyName, valueName)),
                            defaultValue,
                            true,
                            valueNames,
                            Function.identity()));
                });
    }

    private void sanitizeRequirementsForCurrentBlock() {
        requirements = Arrays.stream(safeRequirements())
                .map(this::sanitizeRequirement)
                .filter(requirement -> !requirement.property().isEmpty() && !requirement.value().isEmpty())
                .toArray(PropertyRequirement[]::new);
    }

    private PropertyRequirement sanitizeRequirement(PropertyRequirement raw) {
        var propertyNames = propertyNames(safeBlock());
        if (propertyNames.isEmpty()) return new PropertyRequirement("", "");
        var requirement = safeRequirement(raw);
        var propertyName = validPropertyName(requirement.property(), propertyNames, propertyNames.getFirst());
        var valueNames = propertyValueNames(safeBlock(), propertyName);
        if (valueNames.isEmpty()) return new PropertyRequirement("", "");
        var valueName = validPropertyValue(requirement.value(), valueNames, valueNames.getFirst());
        return new PropertyRequirement(propertyName, valueName);
    }

    private PropertyRequirement defaultRequirement() {
        var propertyNames = propertyNames(safeBlock());
        if (propertyNames.isEmpty()) return new PropertyRequirement("", "");
        var propertyName = propertyNames.getFirst();
        return new PropertyRequirement(propertyName, defaultValueName(safeBlock(), propertyName));
    }

    private Block safeBlock() {
        return block == null ? Blocks.BARRIER : block;
    }

    private PropertyRequirement[] safeRequirements() {
        return requirements == null ? new PropertyRequirement[0] : requirements;
    }

    private static BlockState candidateState(Block block, ResolvedRequirements resolved) {
        if (!resolved.valid()) return Blocks.BARRIER.defaultBlockState();
        var state = block.defaultBlockState();
        for (var requirement : resolved.requirements()) {
            state = applyRequirement(state, requirement);
        }
        return state;
    }

    private static boolean matches(BlockState state, Block block, List<ResolvedRequirement<?>> requirements) {
        if (!state.is(block)) return false;
        for (var requirement : requirements) {
            if (!matchesRequirement(state, requirement)) return false;
        }
        return true;
    }

    private static ResolvedRequirements resolveRequirements(Block block, PropertyRequirement[] requirements) {
        List<ResolvedRequirement<?>> resolved = new ArrayList<>();
        for (var requirement : requirements) {
            var resolvedRequirement = resolveRequirement(block, safeRequirement(requirement));
            if (resolvedRequirement == null) {
                return new ResolvedRequirements(false, List.of());
            }
            resolved.add(resolvedRequirement);
        }
        return new ResolvedRequirements(true, resolved);
    }

    private static ResolvedRequirement<?> resolveRequirement(Block block, PropertyRequirement requirement) {
        if (requirement.property().isEmpty() || requirement.value().isEmpty()) return null;
        var property = block.getStateDefinition().getProperty(requirement.property());
        if (property == null) return null;
        return resolveValue(property, requirement.value());
    }

    private static <T extends Comparable<T>> ResolvedRequirement<T> resolveValue(Property<T> property, String value) {
        return property.getValue(value)
                .map(propertyValue -> new ResolvedRequirement<>(property, propertyValue))
                .orElse(null);
    }

    private static <T extends Comparable<T>> boolean matchesRequirement(BlockState state, ResolvedRequirement<T> requirement) {
        return state.hasProperty(requirement.property()) && state.getValue(requirement.property()).equals(requirement.value());
    }

    private static <T extends Comparable<T>> BlockState applyRequirement(BlockState state, ResolvedRequirement<T> requirement) {
        return state.hasProperty(requirement.property()) ? state.setValue(requirement.property(), requirement.value()) : state;
    }

    private static List<String> propertyNames(Block block) {
        return block.getStateDefinition().getProperties().stream()
                .map(Property::getName)
                .toList();
    }

    private static List<String> propertyValueNames(Block block, String propertyName) {
        var property = block.getStateDefinition().getProperty(propertyName);
        if (property == null) return List.of();
        return propertyValueNames(property);
    }

    private static <T extends Comparable<T>> List<String> propertyValueNames(Property<T> property) {
        return property.getPossibleValues().stream()
                .map(property::getName)
                .toList();
    }

    private static String defaultValueName(Block block, String propertyName) {
        var property = block.getStateDefinition().getProperty(propertyName);
        if (property == null) return "";
        return defaultValueName(block.defaultBlockState(), property);
    }

    private static <T extends Comparable<T>> String defaultValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static String validPropertyName(String propertyName, List<String> propertyNames, String defaultProperty) {
        return propertyNames.contains(propertyName) ? propertyName : defaultProperty;
    }

    private static String validPropertyValue(String valueName, List<String> valueNames, String defaultValue) {
        return valueNames.contains(valueName) ? valueName : defaultValue;
    }

    private static PropertyRequirement[] requirementsFromMap(Map<String, String> requirements) {
        if (requirements == null) return new PropertyRequirement[0];
        return requirements.entrySet().stream()
                .map(entry -> new PropertyRequirement(entry.getKey(), entry.getValue()))
                .toArray(PropertyRequirement[]::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PropertyRequirement requirementFromState(BlockState state, Property<?> property) {
        return typedRequirementFromState(state, (Property) property);
    }

    private static <T extends Comparable<T>> PropertyRequirement typedRequirementFromState(BlockState state, Property<T> typedProperty) {
        return new PropertyRequirement(typedProperty.getName(), typedProperty.getName(state.getValue(typedProperty)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasProperty(BlockState state, Property<?> property) {
        return state.hasProperty((Property) property);
    }

    private static PropertyRequirement safeRequirement(PropertyRequirement requirement) {
        return requirement == null ? new PropertyRequirement("", "") : requirement.safe();
    }

    @NoArgsConstructor
    public static class PropertyRequirement {
        public static final Codec<PropertyRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("property").forGetter(PropertyRequirement::property),
                Codec.STRING.fieldOf("value").forGetter(PropertyRequirement::value)
        ).apply(instance, PropertyRequirement::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PropertyRequirement> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PropertyRequirement::property,
                ByteBufCodecs.STRING_UTF8, PropertyRequirement::value,
                PropertyRequirement::new);

        @Persisted
        public String property = "";
        @Persisted
        public String value = "";

        public PropertyRequirement(String property, String value) {
            this.property = Objects.toString(property, "").trim();
            this.value = Objects.toString(value, "").trim();
        }

        public String property() {
            return Objects.toString(property, "").trim();
        }

        public String value() {
            return Objects.toString(value, "").trim();
        }

        private PropertyRequirement safe() {
            property = property();
            value = value();
            return this;
        }
    }

    private record ResolvedRequirement<T extends Comparable<T>>(Property<T> property, T value) {}

    private record ResolvedRequirements(boolean valid, List<ResolvedRequirement<?>> requirements) {}
}

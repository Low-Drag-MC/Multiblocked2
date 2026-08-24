package com.lowdragmc.mbd2.common.machine.definition.config.blueprint;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.gui.editor.blueprint.MachineBlueprintConfigurator;
import com.lowdragmc.mbd2.common.gui.editor.blueprint.MachineBlueprintResource;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One blueprint attached to a machine definition: which blueprint, whether it runs, and the values for
 * the parameters it exposes.
 *
 * <h2>Exposed parameters</h2>
 * A blueprint exposes a parameter by declaring an {@link VariableKind#INPUT} graph variable. Nothing
 * else — no separate manifest, no annotation. The variable already carries a name, a
 * {@code TypeHandle} and a declared default, which is everything both the configurator row and the
 * runtime seed need. {@link #buildConfigurator} generates one row per INPUT variable, and
 * {@link #resolveVariableValues} turns them into the map that seeds the executor's
 * {@code VariableStore}.
 *
 * <p>Values are stored as serialised {@link Constant}s rather than raw tags so they round-trip through
 * exactly the codec the graph editor uses for the same type — a {@code BlockPos} override and a
 * {@code BlockPos} constant node are the same bytes.</p>
 *
 * <h2>Reference vs. inline</h2>
 * By default a binding <em>references</em> a blueprint resource by path, which is what makes one
 * blueprint reusable across many machines (and editable in one place). The cost is that a machine
 * shipped without its {@code .bp} file silently loses its logic, so {@link #inlined} snapshots the
 * graph into the binding instead, making the machine self-contained. Same mechanism either way — only
 * where the tag comes from differs.
 */
public class MachineBlueprintBinding implements IConfigurable, IPersistedSerializable {

    /** Sentinel for "no blueprint selected". */
    public static final String NO_BLUEPRINT = "";

    /**
     * The referenced resource, as {@link IResourcePath#getPathWithType()}.
     *
     * <p>A string rather than an {@code IResourcePath} because that form <em>is</em> the path's
     * serialised representation ({@code IResourcePath.V2}), so it needs no accessor and round-trips
     * through {@link IResourcePath#parse}.</p>
     */
    @Persisted
    @Getter
    private String blueprintPath = NO_BLUEPRINT;

    /** @see MachineBlueprintBinding */
    @Configurable(name = "config.machine_blueprint.inlined", tips = {
            "config.machine_blueprint.inlined.tooltip.0",
            "config.machine_blueprint.inlined.tooltip.1",
    })
    @Getter
    private boolean inlined = false;

    /** The snapshot used when {@link #inlined}; empty otherwise. */
    @Persisted
    private CompoundTag inlineGraph = new CompoundTag();

    @Configurable(name = "config.machine_blueprint.enabled", tips = "config.machine_blueprint.enabled.tooltip")
    @Getter
    @Setter
    private boolean enabled = true;

    /**
     * Parameter values, keyed by graph-variable name; each value is a serialised {@link Constant}.
     *
     * <p>Kept as a tag rather than a live {@code Map<String, Constant>} because deserialisation runs
     * before any resource is resolvable — a machine definition is read at mod load, its blueprint only
     * when a machine of it is first placed. Materialising against the graph therefore has to be lazy,
     * and this is the form that survives the gap.</p>
     */
    @Persisted
    private CompoundTag variableOverrides = new CompoundTag();

    // ---- resource resolution -----------------------------------------------------------------

    public void setBlueprintPath(String path) {
        this.blueprintPath = path == null ? NO_BLUEPRINT : path;
    }

    public boolean hasBlueprint() {
        return inlined ? !inlineGraph.isEmpty() : !blueprintPath.equals(NO_BLUEPRINT);
    }

    /** The stored tag form of the referenced blueprint, or {@code null} if it cannot be found. */
    @Nullable
    public CompoundTag loadGraphTag() {
        if (inlined) {
            return inlineGraph.isEmpty() ? null : inlineGraph;
        }
        if (blueprintPath.equals(NO_BLUEPRINT)) return null;
        try {
            var path = IResourcePath.parse(blueprintPath);
            return MachineBlueprintResource.INSTANCE.getResourceInstance().getResource(path);
        } catch (Exception e) {
            MBD2.LOGGER.error("Failed to resolve machine blueprint resource {}", blueprintPath, e);
            return null;
        }
    }

    /**
     * Build a live graph from this binding, or {@code null} if the blueprint is missing.
     *
     * <p>Always a fresh instance: a {@code Graph} carries mutable per-run structure (the prepared form,
     * variable stores) and two machines running the same blueprint must not share it.</p>
     */
    @Nullable
    public MachineBlueprintGraph loadGraph() {
        var tag = loadGraphTag();
        if (tag == null) {
            if (hasBlueprint()) {
                MBD2.LOGGER.warn("Machine blueprint {} could not be loaded — its logic will not run.",
                        describe());
            }
            return null;
        }
        try {
            return MachineBlueprintResource.INSTANCE.deserializeGraphResource(tag, null);
        } catch (Exception e) {
            MBD2.LOGGER.error("Failed to deserialize machine blueprint {}", describe(), e);
            return null;
        }
    }

    /**
     * A binding holding {@code graphTag} directly, with no resource behind it.
     *
     * <p>For code that builds a blueprint rather than authoring one — test fixtures, and any
     * datapack/script path that hands MBD2 a graph it constructed itself.</p>
     */
    public static MachineBlueprintBinding ofInline(CompoundTag graphTag) {
        var binding = new MachineBlueprintBinding();
        binding.inlineGraph = graphTag.copy();
        binding.inlined = true;
        return binding;
    }

    /** Set a parameter's value directly, for the same programmatic callers as {@link #ofInline}. */
    public MachineBlueprintBinding withVariable(String name, Constant value) {
        storeOverride(name, value, Platform.getFrozenRegistry());
        return this;
    }

    // ---- editor-side graph cache ---------------------------------------------------------------
    //
    // The inspector rebuilds its parameter rows on a tick listener, so without a cache every tick of
    // an open machine editor would deserialize the whole blueprint. Keyed by tag identity rather than
    // equality: the resource instance hands back the same CompoundTag until its cache is invalidated,
    // and a save replaces it with a new one — so identity already means exactly "unchanged", at O(1)
    // instead of a deep compare of the entire graph.
    @Nullable
    private CompoundTag cachedTag;
    @Nullable
    private MachineBlueprintGraph cachedGraph;

    /**
     * The graph for the editor to read, cached between refreshes.
     *
     * <p>Not for runtime use — {@link MachineBlueprintInstance} calls {@link #loadGraph()} for a graph
     * of its own, because two machines running the same blueprint must not share one.</p>
     */
    @Nullable
    private MachineBlueprintGraph editorGraph() {
        var tag = loadGraphTag();
        if (tag == null) {
            cachedTag = null;
            cachedGraph = null;
            return null;
        }
        if (cachedGraph != null && tag == cachedTag) return cachedGraph;
        cachedTag = tag;
        cachedGraph = loadGraph();
        return cachedGraph;
    }

    /** Snapshot the referenced blueprint into this binding, or drop the snapshot. */
    @ConfigSetter(field = "inlined")
    public void setInlined(boolean inlined) {
        if (this.inlined == inlined) return;
        if (inlined) {
            var tag = loadGraphTag();
            this.inlineGraph = tag == null ? new CompoundTag() : tag.copy();
        } else {
            this.inlineGraph = new CompoundTag();
        }
        this.inlined = inlined;
    }

    public String describe() {
        return inlined ? "<inlined>" : blueprintPath;
    }

    // ---- exposed parameters ------------------------------------------------------------------

    /**
     * The values to seed {@code graph}'s INPUT variables with: this binding's override where it has
     * one, the variable's declared default otherwise.
     */
    public Map<String, Object> resolveVariableValues(MachineBlueprintGraph graph,
                                                     HolderLookup.Provider provider) {
        var values = new LinkedHashMap<String, Object>();
        for (var variable : inputVariables(graph)) {
            values.put(variable.getName(), overrideConstant(variable, provider).getValue());
        }
        return values;
    }

    /** The graph's exposed parameters, in declaration order. */
    public static List<VariableDeclarationModelBase> inputVariables(MachineBlueprintGraph graph) {
        var result = new ArrayList<VariableDeclarationModelBase>();
        for (var variable : graph.graphModel.getGraphVariableModels()) {
            if (variable != null && variable.getVariableKind() == VariableKind.INPUT) {
                result.add(variable);
            }
        }
        return result;
    }

    /**
     * The constant backing {@code variable}'s row: this binding's stored override, else a copy of the
     * variable's own initialisation constant, else a bare constant of the variable's type.
     *
     * <p>Falling back to a <em>copy</em> of the declaration's constant rather than to the declaration
     * itself matters — editing the row must not rewrite the blueprint's default for every other
     * machine using it.</p>
     */
    private Constant overrideConstant(VariableDeclarationModelBase variable, HolderLookup.Provider provider) {
        var name = variable.getName();
        if (variableOverrides.contains(name)) {
            var stored = TypeConstant.deserializeConstant(variableOverrides.getCompound(name), provider);
            if (stored != null) return stored;
        }
        var declared = variable.getInitializationModel();
        if (declared != null) return declared.copy();
        var fresh = new TypeConstant();
        fresh.init(variable.getDataTypeHandle());
        return fresh;
    }

    private void storeOverride(String name, Constant constant, HolderLookup.Provider provider) {
        variableOverrides.put(name, TypeConstant.serializeConstant(constant, provider));
    }

    /** Forget overrides for variables the blueprint no longer declares. */
    public void pruneOverrides(MachineBlueprintGraph graph) {
        var live = inputVariables(graph).stream().map(VariableDeclarationModelBase::getName).toList();
        for (var key : List.copyOf(variableOverrides.getAllKeys())) {
            if (!live.contains(key)) variableOverrides.remove(key);
        }
    }

    // ---- editor UI ---------------------------------------------------------------------------

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        // The selector goes first because everything else on the row is about the blueprint it picks.
        father.addConfigurator(createBlueprintSelector());
        // enabled + inlined come from the annotation scan (`inlined` routes through @ConfigSetter so
        // the snapshot is taken/dropped rather than the field being written behind our back).
        IConfigurable.super.buildConfigurator(father);
        father.addConfigurator(createParameterGroup());
    }

    private Configurator createBlueprintSelector() {
        return new MachineBlueprintConfigurator("config.machine_blueprint.blueprint",
                () -> blueprintPath, this::setBlueprintPath);
    }

    /**
     * The exposed-parameter rows, rebuilt whenever the blueprint's variable list changes.
     *
     * <p>Rebuild is signature-driven on TICK rather than done once, because the blueprint can be
     * edited in another editor tab while this inspector is open — adding an INPUT variable there has
     * to make a row appear here. Same shape as {@code GeckolibRenderer.refreshAnimationInfoGroup}.</p>
     */
    private ConfiguratorGroup createParameterGroup() {
        var group = new ConfiguratorGroup("config.machine_blueprint.parameters", false);
        var signature = new String[]{null};
        group.addEventListener(UIEvents.TICK, event -> refreshParameterGroup(group, signature));
        refreshParameterGroup(group, signature);
        return group;
    }

    private void refreshParameterGroup(ConfiguratorGroup group, String[] signature) {
        var graph = editorGraph();
        var variables = graph == null ? List.<VariableDeclarationModelBase>of() : inputVariables(graph);
        var newSignature = describe() + "|" + variables.stream()
                .map(v -> v.getName() + ":" + v.getDataTypeHandle().getIdentification())
                .reduce("", (left, right) -> left + ";" + right);
        if (newSignature.equals(signature[0])) return;
        signature[0] = newSignature;

        group.removeAllConfigurators();
        if (graph == null) {
            group.addConfigurator(new Configurator("")
                    .addInlineChild(new Label().setText(hasBlueprint()
                            ? Component.translatable("config.machine_blueprint.parameters.missing")
                            : Component.translatable("config.machine_blueprint.parameters.none"))));
            return;
        }
        pruneOverrides(graph);
        if (variables.isEmpty()) {
            group.addConfigurator(new Configurator("")
                    .addInlineChild(new Label().setText(
                            Component.translatable("config.machine_blueprint.parameters.empty"))));
            return;
        }
        var provider = Platform.getFrozenRegistry();
        for (var variable : variables) {
            group.addConfigurator(createParameterRow(variable, provider));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Configurator createParameterRow(VariableDeclarationModelBase variable,
                                            HolderLookup.Provider provider) {
        var name = variable.getName();
        // One constant per row, live for the row's lifetime: the accessor drives it directly and every
        // edit is written straight back to the tag, so there is no "apply" step to forget.
        var constant = overrideConstant(variable, provider);
        try {
            var accessor = (com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor)
                    ConfiguratorAccessors.findByType(variable.getDataType());
            return accessor.create(name, constant::getValue, value -> {
                constant.setValue(value);
                storeOverride(name, constant, provider);
            }, true, null, null);
        } catch (RuntimeException e) {
            // No configurator for this type — a variable of a wire-only type (Machine, Recipe...).
            // Those cannot be authored as a literal at all, so the row says so instead of crashing
            // the whole inspector.
            return new Configurator(name).addInlineChild(new Label().setText(
                    Component.translatable("config.machine_blueprint.parameters.unsupported",
                            variable.getDataTypeHandle().getFriendlyName())));
        }
    }
}

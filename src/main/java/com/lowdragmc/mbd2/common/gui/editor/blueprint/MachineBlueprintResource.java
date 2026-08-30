package com.lowdragmc.mbd2.common.gui.editor.blueprint;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResource;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.client.MBDIcons;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.builtin.BuiltinBlueprints;
import net.minecraft.nbt.CompoundTag;

import java.io.File;

/**
 * The editor resource type that hosts {@link MachineBlueprintGraph}s.
 *
 * <p>Making blueprints a resource rather than a field on the machine definition is what makes them
 * reusable: one blueprint can be referenced by any number of machines, edited in one place, and pulled
 * into another blueprint as an external subgraph (LDLib2's {@code SubgraphNodeModel.Kind.EXTERNAL}
 * plus {@code IGraphReferenceResolver} handle that with no extra code here).</p>
 *
 * <p>The builtin provider points at {@code <MBD2 content dir>/blueprint} rather than LDLib2's default
 * resource folder so blueprints ship the same way machines do — they sit beside the {@code .sm} files
 * a pack author already copies. Resolution is dist-agnostic ({@code ResourceInstance} is a file/pack
 * lookup with a cache), so a referenced blueprint loads on a dedicated server too.</p>
 */
public class MachineBlueprintResource extends GraphResource<MachineBlueprintGraph> {
    public static final MachineBlueprintResource INSTANCE = new MachineBlueprintResource();

    private MachineBlueprintResource() {}

    @Override
    public MachineBlueprintGraph createGraph() {
        return new MachineBlueprintGraph();
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDIcons.BLUEPRINT;
    }

    @Override
    public String getName() {
        return "machine_blueprint";
    }

    @Override
    public String getFileExtension() {
        return ".bp.nbt";
    }

    /**
     * Two providers: the blueprints MBD2 ships, then the pack's own folder.
     *
     * <p>Shipped first because that is the order the panel lists them in, and a player opening the
     * blueprint library for the first time should land on the worked examples rather than on an empty
     * folder. The base implementation is deliberately not called — it would add a second builtin
     * provider and a {@code global} folder under LDLib2's assets directory, and blueprints live beside
     * the machines they belong to.</p>
     */
    @Override
    public void buildBuiltin(ResourceInstance<CompoundTag> resourceInstance) {
        var builtin = new BuiltinResourceProvider<CompoundTag>(BuiltinBlueprints.PROVIDER_NAME, resourceInstance);
        buildBuiltin(builtin);
        resourceInstance.addBuiltinProvider(builtin);

        var global = new FileResourceProvider<>(resourceInstance, new File(MBD2.getLocation(), "blueprint"));
        global.setName("global");
        resourceInstance.addBuiltinProvider(global);
    }

    @Override
    public void buildBuiltin(BuiltinResourceProvider<CompoundTag> provider) {
        BuiltinBlueprints.register(provider);
    }
}

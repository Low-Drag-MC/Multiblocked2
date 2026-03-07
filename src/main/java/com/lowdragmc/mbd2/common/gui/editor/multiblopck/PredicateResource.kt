package com.lowdragmc.mbd2.common.gui.editor.multiblopck

import com.lowdragmc.lowdraglib2.editor.resource.*
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag

class PredicateResource : Resource<PatternPredicate>() {
    companion object {
        @JvmStatic
        val INSTANCE: PredicateResource = PredicateResource()
    }

    override fun buildBuiltin(provider: BuiltinResourceProvider<PatternPredicate>) {
        provider.addResource("any", PatternPredicate.ANY);
        provider.addResource("air", PatternPredicate.AIR);
    }

    override fun getIcon(): IGuiTexture? {
        return Icons.GLOBAL
    }

    override fun getName(): String {
        return "predicate"
    }

    override fun serializeResource(value: PatternPredicate, provider: HolderLookup.Provider): Tag? {
        return PatternPredicate.CODEC.encodeStart(
            provider.createSerializationContext(NbtOps.INSTANCE),
            value)
            .result().orElse(null)
    }

    override fun deserializeResource(nbt: Tag, provider: HolderLookup.Provider): PatternPredicate? {
        return PatternPredicate.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt)
            .result().orElse(null)
    }

    override fun createResourceProviderContainer(provider: IResourceProvider<PatternPredicate>): ResourceProviderContainer<PatternPredicate> {
        val container = super.createResourceProviderContainer(provider)
        container.setUiSupplier {
            element({
                layout = { size(33.px) }
                style = { background(provider.getResource(it)?.previewTexture ?: IGuiTexture.EMPTY) }
            }) { }
        }
        container.setOnEdit({ c, path ->
            val predicate = provider.getResource(path);
            if (predicate != null && predicate != PatternPredicate.ANY && predicate != PatternPredicate.AIR) {
                c.editor.inspectorView.inspect(predicate, { c.markResourceDirty(path) })
            }
        })

        if (provider.supportAdd()) {
            container.setOnMenu({ c, m ->
                m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", { menu: TreeBuilder.Menu ->
                    for (holder in MBDRegistries.PATTERN_PREDICATES) {
                        val name = holder.annotation().name
                        if (name == "any" || name == "air") continue
                        menu.leaf(name, { c.addNewResource(holder.value().get()) })
                    }
                })
            })
        }
        return container
    }
}
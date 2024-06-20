package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.IRendererResourceContainer;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class PredicateResource extends Resource<SimplePredicate> {
    public final static String RESOURCE_NAME = "mbd2.gui.editor.group.predicate";

    public PredicateResource() {
        data.put("any", SimplePredicate.ANY);
        data.put("air", SimplePredicate.AIR);
    }

    @Override
    public void buildDefault() {
    }

    @Override
    public String name() {
        return RESOURCE_NAME;
    }

    @Override
    public ResourceContainer<SimplePredicate, ? extends Widget> createContainer(ResourcePanel resourcePanel) {
        return new IRendererResourceContainer(this, resourcePanel);
    }

    @Nullable
    @Override
    public Tag serialize(SimplePredicate renderer) {
        if (renderer instanceof ISerializableRenderer serializableRenderer) {
            return ISerializableRenderer.serializeWrapper(serializableRenderer);
        }
        return null;
    }

    @Override
    public SimplePredicate deserialize(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            return ISerializableRenderer.deserializeWrapper(compoundTag);
        }
        return IRenderer.EMPTY;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.clear();
        data.put("any", SimplePredicate.ANY);
        data.put("air", SimplePredicate.AIR);
        for (String key : nbt.getAllKeys()) {
            data.put(key, deserialize(nbt.get(key)));
        }
    }
}

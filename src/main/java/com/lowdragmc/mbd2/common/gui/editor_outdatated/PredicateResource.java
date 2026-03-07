//package com.lowdragmc.mbd2.common.gui.editor_outdatated;
//
//import com.lowdragmc.lowdraglib2.LDLib2;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.LDLRegister;
//import com.lowdragmc.lowdraglib2.gui.editor.data.resource.Resource;
//import com.lowdragmc.lowdraglib2.gui.editor.ui.ResourcePanel;
//import com.lowdragmc.lowdraglib2.gui.editor.ui.resource.ResourceContainer;
//import com.lowdragmc.lowdraglib2.gui.widget.Widget;
//import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.Tag;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.File;
//
//import static com.lowdragmc.mbd2.common.gui.editor_outdatated.PredicateResource.RESOURCE_NAME;
//
//
//@LDLRegister(name = RESOURCE_NAME, group = "resource")
//public class PredicateResource extends Resource<PatternPredicate> {
//    public final static String RESOURCE_NAME = "mbd2.gui.editor.group.predicate";
//
//    public PredicateResource() {
//        super(new File(LDLib2.getLDLibDir(), "assets/resources/predicates"));
//        addBuiltinResource("any", PatternPredicate.ANY);
//        addBuiltinResource("air", PatternPredicate.AIR);
//    }
//
//    @Override
//    public String name() {
//        return RESOURCE_NAME;
//    }
//
//    @Override
//    public ResourceContainer<PatternPredicate, ? extends Widget> createContainer(ResourcePanel resourcePanel) {
//        return new PredicateResourceContainer(this, resourcePanel);
//    }
//
//    @Nullable
//    @Override
//    public Tag serialize(PatternPredicate predicate, HolderLookup.Provider provider) {
//        return PatternPredicate.serializeWrapper(provider, predicate);
//    }
//
//    @Override
//    public PatternPredicate deserialize(Tag tag, HolderLookup.Provider provider) {
//        if (tag instanceof CompoundTag compoundTag) {
//            return PatternPredicate.deserializeWrapper(provider, compoundTag);
//        }
//        return PatternPredicate.ANY;
//    }
//
//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        getBuiltinResources().clear();
//        addBuiltinResource("any", PatternPredicate.ANY);
//        addBuiltinResource("air", PatternPredicate.AIR);
//        for (String key : nbt.getAllKeys()) {
//            addBuiltinResource(key, deserialize(nbt.get(key)));
//        }
//    }
//}

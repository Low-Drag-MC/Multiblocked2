package com.lowdragmc.mbd2.integration.ldlib;

import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.mark.MarkStrategies;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicatePartialState;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.item.LargeItemStackTransferStrategy;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalStorageStrategy;
import mekanism.api.chemical.ChemicalStack;

public class MBDSyncedFieldAccessors {

    public static void init() {
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(MBDRecipeType.class)
                .codec(MBDRegistries.RECIPE_TYPES.codec())
                .streamCodec(MBDRegistries.RECIPE_TYPES.streamCodec())
                .build());

        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(RecipeCondition.class)
                .codec(RecipeCondition.CODEC)
                .streamCodec(RecipeCondition.STREAM_CODEC)
                .copyMark(RecipeCondition::copy)
                .build());

        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(TraitDefinition.class)
                .codec(TraitDefinition.CODEC)
                .streamCodec(TraitDefinition.STREAM_CODEC)
                .codecMark()
                .build());

        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(PredicatePartialState.PropertyRequirement.class)
                .codec(PredicatePartialState.PropertyRequirement.CODEC)
                .streamCodec(PredicatePartialState.PropertyRequirement.STREAM_CODEC)
                .copyMark(data -> new PredicatePartialState.PropertyRequirement(data.property(), data.value()))
                .build());

        MarkStrategies.register(new LargeItemStackTransferStrategy(), 1000);

        if (MBD2.isMekanismLoaded()) {
            AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(ChemicalStack.class)
                    .codec(ChemicalStack.OPTIONAL_CODEC)
                    .streamCodec(ChemicalStack.OPTIONAL_STREAM_CODEC)
                    .copyMark(ChemicalStack::copy)
                    .build());
            MarkStrategies.register(new ChemicalStorageStrategy(), 1000);
        }
    }
}

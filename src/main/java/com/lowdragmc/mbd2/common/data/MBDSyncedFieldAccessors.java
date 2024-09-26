package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.StringPayload;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.syncdata.ChemicalStackAccessor;
import com.lowdragmc.mbd2.syncdata.MBDRecipeAccessor;
import com.lowdragmc.mbd2.syncdata.MBDRecipeTypeAccessor;

import static com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries.register;

public class MBDSyncedFieldAccessors {
    public static final IAccessor MBD_RECIPE_ACCESSOR = new MBDRecipeAccessor();
    public static final IAccessor MBD_RECIPE_TYPE_ACCESSOR = new MBDRecipeTypeAccessor();
    public static IAccessor CHEMICAL_STACK_ACCESSOR;

    public static void init() {
        register(NbtTagPayload.class, NbtTagPayload::new, MBD_RECIPE_ACCESSOR, 1000);
        register(StringPayload.class, StringPayload::new, MBD_RECIPE_TYPE_ACCESSOR, 1000);
        if (MBD2.isMekanismLoaded()) {
            CHEMICAL_STACK_ACCESSOR = new ChemicalStackAccessor();
            register(NbtTagPayload.class, NbtTagPayload::new, CHEMICAL_STACK_ACCESSOR, 1000);
        }
    }
}

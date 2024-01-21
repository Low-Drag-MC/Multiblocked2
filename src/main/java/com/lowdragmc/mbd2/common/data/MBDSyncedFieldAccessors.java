package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import com.lowdragmc.mbd2.syncdata.MBDRecipeAccessor;
import com.lowdragmc.mbd2.syncdata.MBDRecipeTypeAccessor;

import static com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries.register;

public class MBDSyncedFieldAccessors {
    public static final IAccessor MBD_RECIPE_ACCESSOR = new MBDRecipeAccessor();
    public static final IAccessor MBD_RECIPE_TYPE_ACCESSOR = new MBDRecipeTypeAccessor();

    public static void init() {
        register(FriendlyBufPayload.class, FriendlyBufPayload::new, MBD_RECIPE_ACCESSOR, 1000);
        register(FriendlyBufPayload.class, FriendlyBufPayload::new, MBD_RECIPE_TYPE_ACCESSOR, 1000);
    }
}

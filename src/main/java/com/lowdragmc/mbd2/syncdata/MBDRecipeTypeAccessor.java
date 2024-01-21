package com.lowdragmc.mbd2.syncdata;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.CustomObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public class MBDRecipeTypeAccessor extends CustomObjectAccessor<MBDRecipeType> {

    public MBDRecipeTypeAccessor() {
        super(MBDRecipeType.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp accessorOp, MBDRecipeType recipeType) {
        FriendlyByteBuf serializedHolder = new FriendlyByteBuf(Unpooled.buffer());
        serializedHolder.writeResourceLocation(recipeType.registryName);
        return FriendlyBufPayload.of(serializedHolder);
    }

    @Override
    public MBDRecipeType deserialize(AccessorOp accessorOp, ITypedPayload<?> payload) {
        if (payload instanceof FriendlyBufPayload buffer) {
            var id = buffer.getPayload().readResourceLocation();
            return MBDRegistries.RECIPE_TYPES.get(id);
        }
        return null;
    }
}

package com.lowdragmc.mbd2.syncdata;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.CustomObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class MBDRecipeAccessor extends CustomObjectAccessor<MBDRecipe> {

    public MBDRecipeAccessor() {
        super(MBDRecipe.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp accessorOp, MBDRecipe MBDRecipe) {
        FriendlyByteBuf serializedHolder = new FriendlyByteBuf(Unpooled.buffer());
        serializedHolder.writeUtf(MBDRecipe.id.toString());
        MBDRecipeSerializer.SERIALIZER.toNetwork(serializedHolder, MBDRecipe);
        return FriendlyBufPayload.of(serializedHolder);
    }

    @Override
    public MBDRecipe deserialize(AccessorOp accessorOp, ITypedPayload<?> payload) {
        if (payload instanceof FriendlyBufPayload buffer) {
            var id = new ResourceLocation(buffer.getPayload().readUtf());
            return MBDRecipeSerializer.SERIALIZER.fromNetwork(id, buffer.getPayload());
        }
        return null;
    }
}

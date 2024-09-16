package com.lowdragmc.mbd2.syncdata;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.CustomObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class MBDRecipeAccessor extends CustomObjectAccessor<MBDRecipe> {

    public MBDRecipeAccessor() {
        super(MBDRecipe.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp accessorOp, MBDRecipe MBDRecipe) {
        NbtTagPayload nbtTagPayload = new NbtTagPayload();
        var tag = new CompoundTag();
        tag.putString("id", MBDRecipe.id.toString());
        tag.put("recipe", MBDRecipeSerializer.SERIALIZER.toNBT(MBDRecipe));
        return nbtTagPayload.setPayload(tag);
    }

    @Override
    public MBDRecipe deserialize(AccessorOp accessorOp, ITypedPayload<?> payload) {
        if (payload instanceof NbtTagPayload nbtTagPayload && nbtTagPayload.getPayload() instanceof CompoundTag tag) {
            var id = new ResourceLocation(tag.getString("id"));
            return MBDRecipeSerializer.SERIALIZER.fromNBT(id, tag.getCompound("recipe"));
        }
        return null;
    }
}

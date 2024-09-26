package com.lowdragmc.mbd2.syncdata;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.CustomObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.nbt.CompoundTag;

public class ChemicalStackAccessor extends CustomObjectAccessor<ChemicalStack> {

    public ChemicalStackAccessor() {
        super(ChemicalStack.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp op, ChemicalStack value) {
        var tag = new CompoundTag();
        ChemicalType.getTypeFor(value.getType()).write(tag);
        value.write(tag);
        return NbtTagPayload.of(tag);
    }

    @Override
    public ChemicalStack<?> deserialize(AccessorOp op, ITypedPayload payload) {
        if (payload instanceof NbtTagPayload nbtPayload && nbtPayload.getPayload() instanceof CompoundTag tag) {
            var type = ChemicalType.fromNBT(tag);
            if (type == ChemicalType.GAS) {
                return GasStack.readFromNBT(tag);
            } else if (type == ChemicalType.INFUSION) {
                return InfusionStack.readFromNBT(tag);
            } else if (type == ChemicalType.PIGMENT) {
                return PigmentStack.readFromNBT(tag);
            } else if (type == ChemicalType.SLURRY) {
                return SlurryStack.readFromNBT(tag);
            }
        }
        return null;
    }
}

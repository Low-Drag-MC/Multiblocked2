package com.lowdragmc.mbd2.integration.pneumaticcraft.trait;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PressureAir;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PNCPressureAirHandlerTrait extends RecipeCapabilityTrait<PressureAir> implements ICapabilityProviderTrait<IAirHandler> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PNCPressureAirHandlerTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableAirHandler handler;

    public PNCPressureAirHandlerTrait(MBDMachine machine, PNCPressureAirHandlerTraitDefinition definition) {
        super(machine, definition);
        handler = createHandler();
        handler.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public PNCPressureAirHandlerTraitDefinition getDefinition() {
        return (PNCPressureAirHandlerTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        handler.addAir((int) (handler.maxPressure() / 2  * handler.getBaseVolume()));
    }

    protected CopiableAirHandler createHandler() {
        return new CopiableAirHandler(getDefinition().getVolume(), getDefinition().getMaxPressure());
    }

    @Override
    public List<PressureAir> handleRecipeInner(IO io, MBDRecipe recipe, List<PressureAir> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        var handler = simulate ? this.handler.copy() : this.handler;
        if (io == IO.IN) {
            var iterator = left.iterator();
            while (iterator.hasNext()) {
                var pressureAir = iterator.next();
                var pressure = pressureAir.value();
                if (pressureAir.isAir()) {
                    pressure = pressure / handler.getBaseVolume();
                }
                var leftPressure = handler.maxPressure() - handler.getPressure();
                if (pressure > leftPressure) {
                    // can't fit all the air in
                    continue;
                }
                var air = (int) (pressure * handler.getBaseVolume());
                handler.addAir(air);
                iterator.remove();
            }
        } else if (io == IO.OUT) {
            var iterator = left.iterator();
            while (iterator.hasNext()) {
                var pressureAir = iterator.next();
                var air = pressureAir.value();
                if (!pressureAir.isAir()) {
                    air = handler.baseVolume * air;
                }
                var leftAir = handler.getAir();
                if (air > leftAir) {
                    // can't drain all the air out
                    continue;
                }
                handler.addAir((int) -air);
                iterator.remove();
            }
        }
        return left.isEmpty() ? null : left;
    }

    @Override
    public IO getCapabilityIO(@Nullable Direction side) {
        return IO.BOTH;
    }

    @Override
    public Capability<? super IAirHandler> getCapability() {
        return PNCCapabilities.AIR_HANDLER_CAPABILITY;
    }

    @Override
    public IAirHandler getCapContent(IO capbilityIO) {
        return handler;
    }

    @Override
    public IAirHandler mergeContents(List<IAirHandler> contents) {
        return contents.get(0);
    }
}

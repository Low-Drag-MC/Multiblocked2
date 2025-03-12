package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PressureAir;
import lombok.Getter;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.core.Direction;
import net.neoforged.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PNCPressureAirHandlerTrait extends RecipeCapabilityTrait implements IRecipeHandlerTrait<PressureAir> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PNCPressureAirHandlerTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableAirHandler handler;
    private final AirHandlerMachineCap airHandlerMachineCap = new AirHandlerMachineCap();
    private final AirHandlerCap airHandlerCap = new AirHandlerCap();
    @Nullable
    private Direction lastFront = null;

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
        handler.addAir((int) (handler.maxPressure() / 2  * handler.getVolume()));
    }

    protected CopiableAirHandler createHandler() {
        return new CopiableAirHandler(getDefinition().getPressureTier(), getDefinition().getVolume(), getDefinition().getMaxPressure());
    }

    protected void updateHullAirHandlers() {
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        if (lastFront == front) return;
        var list = new ArrayList<Direction>();
        for (Direction side : DirectionUtil.VALUES) {
            if (getDefinition().getConnectionIO().getConnection(front, side)) {
                list.add(side);
            }
        }
        handler.setConnectedFaces(list);
        lastFront = front;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        updateHullAirHandlers();
        handler.tick(getMachine().getHolder());
    }

    @Override
    public void clientTick() {
        super.clientTick();
        updateHullAirHandlers();
        handler.tick(getMachine().getHolder());
    }

    @Override
    public List<PressureAir> handleRecipeInner(IO io, MBDRecipe recipe, List<PressureAir> left, @Nullable String slotName, boolean simulate) {
        if (!compatibleWith(io)) return left;
        var handler = simulate ? this.handler.copy() : this.handler;
        if (io == IO.IN) {
            var iterator = left.iterator();
            while (iterator.hasNext()) {
                var pressureAir = iterator.next();
                var air = pressureAir.value();
                if (!pressureAir.isAir()) {
                    air = handler.getVolume() * air;
                }
                var leftAir = handler.getAir();
                if (air > leftAir) {
                    // can't drain all the air out
                    continue;
                }
                handler.addAir((int) -air);
                iterator.remove();
            }
        } else if (io == IO.OUT) {
            var iterator = left.iterator();
            while (iterator.hasNext()) {
                var pressureAir = iterator.next();
                var pressure = pressureAir.value();
                if (pressureAir.isAir()) {
                    pressure = pressure / handler.getVolume();
                }
                var leftPressure = handler.maxPressure() - handler.getPressure();
                if (pressure > leftPressure) {
                    // can't fit all the air in
                    continue;
                }
                var air = (int) (pressure * handler.getVolume());
                handler.addAir(air);
                iterator.remove();
            }
        }
        return left.isEmpty() ? null : left;
    }

    @Override
    public RecipeCapability<PressureAir> getRecipeCapability() {
        return PNCPressureAirRecipeCapability.CAP;
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(this);
    }

    @Override
    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return List.of(airHandlerMachineCap, airHandlerCap);
    }

    public class AirHandlerMachineCap implements ICapabilityProviderTrait<IAirHandlerMachine>{

        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return IO.BOTH;
        }

        @Override
        public Capability<? super IAirHandlerMachine> getCapability() {
            return PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY;
        }

        @Override
        public IAirHandlerMachine getCapContent(IO capbilityIO) {
            return handler;
        }

        @Override
        public IAirHandlerMachine mergeContents(List<IAirHandlerMachine> contents) {
            return handler;
        }
    }

    public class AirHandlerCap implements ICapabilityProviderTrait<IAirHandler>{

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
            return handler;
        }
    }

}

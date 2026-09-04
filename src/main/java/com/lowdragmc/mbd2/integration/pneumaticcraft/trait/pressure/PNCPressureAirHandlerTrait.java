package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeConnectedIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PressureAir;
import lombok.Getter;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PNCPressureAirHandlerTrait extends SimpleCapabilityTrait<IAirHandlerMachine, @Nullable Direction> implements IAutoIOTrait {
    @Persisted
    public final CopiableAirHandler handler;

    private final PressureRecipeHandler recipeHandler = new PressureRecipeHandler();

    // per-machine overrides of the values authored on the definition
    // @Getter(NONE): see ForgeEnergyCapabilityTrait — the class @Getter would republish getAutoIO()
    // with a different type and without the old "null when disabled" contract.
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeAutoIO autoIO =
            new RuntimeAutoIO(runtimeValues, "auto_io", () -> getDefinition().getAutoIO());
    // @Getter(NONE): would otherwise shadow the definition's getConnectionIO(): ConnectedIO.
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeConnectedIO connectionIO =
            new RuntimeConnectedIO(runtimeValues, "connection_io", () -> getDefinition().getConnectionIO(),
                    // updateHullAirHandlers() only recomputes when the facing changed, so without this an
                    // override would not reach handler.setConnectableFaces until the machine was rotated
                    // or a neighbour update reset lastFront — air would keep flowing through a side the
                    // override just closed.
                    () -> lastFront = null);
    // The four below are @Getter(NONE) for a milder version of the same reason as autoIO above: the
    // handler this trait hands out already has getVolume(), maxPressure(), getDangerPressure() and
    // getCriticalPressure(), all returning the number. A generated twin on the trait returning the slot
    // instead is exactly the kind of pair a caller picks the wrong one of.
    /**
     * The air volume this machine's tank holds.
     * <p>
     * Shrinking it scales the stored air down with it, the way PneumaticCraft's own
     * {@code setVolumeUpgrades} does: the alternative — keeping the air and letting the pressure rise —
     * turns "give this machine a smaller tank" into "make this machine explode".
     */
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeValue<Integer> volume =
            runtimeValues.ofInt("volume", () -> getDefinition().getVolume())
                    .onChanged(this::onVolumeChanged);
    /** The pressure this machine bursts at. @see PNCPressureAirHandlerTraitDefinition#getMaxPressure() */
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeValue<Float> maxPressure =
            runtimeValues.ofFloat("max_pressure", () -> getDefinition().getMaxPressure())
                    .onChanged(() -> getHandler().setMaxPressure(this.maxPressure.get()));
    /** Zero means "same as max pressure", exactly as on the definition. */
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeValue<Float> dangerPressure =
            runtimeValues.ofFloat("danger_pressure", () -> getDefinition().getDangerPressure());
    /** Zero means "same as the danger pressure", exactly as on the definition. */
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeValue<Float> criticalPressure =
            runtimeValues.ofFloat("critical_pressure", () -> getDefinition().getCriticalPressure());
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IAirHandlerMachine, Direction>>> nearbyCache = new HashMap<>();
    @Nullable
    private Direction lastFront = null;

    public PNCPressureAirHandlerTrait(MBDMachine machine, PNCPressureAirHandlerTraitDefinition definition) {
        super(machine, definition);
        // a tier that reads this trait's slots rather than the definition's fields: MachineAirHandler
        // stores the tier once and calls through it on every query, so this is what makes the two
        // pressure thresholds overridable at all
        handler = new CopiableAirHandler(new PressureTier() {
            @Override
            public float getDangerPressure() {
                return realDangerPressure();
            }

            @Override
            public float getCriticalPressure() {
                return realCriticalPressure();
            }
        }, volume.get(), maxPressure.get());
        handler.setOnContentsChanged(this::notifyListeners);
    }

    /** @see PNCPressureAirHandlerTraitDefinition#getRealDangerPressure() */
    public float realDangerPressure() {
        var danger = dangerPressure.get();
        return danger == 0 ? maxPressure.get() : danger;
    }

    /** @see PNCPressureAirHandlerTraitDefinition#getRealCriticalPressure() */
    public float realCriticalPressure() {
        var critical = criticalPressure.get();
        return critical == 0 ? realDangerPressure() : critical;
    }

    private void onVolumeChanged() {
        var handler = getHandler();
        var newVolume = Math.max(1, volume.get());
        var oldVolume = handler.getVolume();
        if (oldVolume == newVolume) return;
        if (newVolume < oldVolume) {
            var scaled = (int) (handler.getAir() * (float) newVolume / oldVolume);
            handler.addAir(scaled - handler.getAir());
        }
        handler.setBaseVolume(newVolume);
        notifyListeners();
    }

    @Override
    public PNCPressureAirHandlerTraitDefinition getDefinition() {
        return (PNCPressureAirHandlerTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        handler.addAir((int) (handler.maxPressure() / 2 * handler.getVolume()));
    }

    @Override
    public IAirHandlerMachine getCapContent(IO capabilityIO) {
        return handler;
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public RuntimeAutoIO getRuntimeAutoIO() {
        return autoIO;
    }

    protected void updateHullAirHandlers() {
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        if (lastFront == front) return;
        var list = new ArrayList<Direction>();
        for (Direction side : Direction.values()) {
            if (connectionIO.getConnection(front, side)) {
                list.add(side);
            }
        }
        handler.setConnectableFaces(list);
        lastFront = front;
    }

    @Override
    public void serverTick() {
        IAutoIOTrait.super.serverTick();
        updateHullAirHandlers();
        var holder = getMachine().getHolder();
        if (holder != null) handler.tick(holder);
    }

    @Override
    public void clientTick() {
        updateHullAirHandlers();
        var holder = getMachine().getHolder();
        if (holder != null) handler.tick(holder);
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        lastFront = null;
        updateHullAirHandlers();
    }

    @NotNull
    public BlockCapabilityCache<IAirHandlerMachine, Direction> getNearbyCache(ServerLevel serverLevel, BlockPos pos, Direction side) {
        return nearbyCache.computeIfAbsent(pos, p -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(
                        PNCCapabilities.AIR_HANDLER_MACHINE, serverLevel, pos, direction));
    }

    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (!(getMachine().getLevel() instanceof ServerLevel serverLevel)) return;
        var neighbor = getNearbyCache(serverLevel, port, side).getCapability();
        if (neighbor == null) return;
        if (io.support(IO.OUT)) {
            transferAir(handler, neighbor);
        }
        if (io.support(IO.IN)) {
            transferAir(neighbor, handler);
        }
    }

    private static void transferAir(IAirHandlerMachine source, IAirHandlerMachine target) {
        float srcPressure = source.getPressure();
        float tgtPressure = target.getPressure();
        if (srcPressure <= tgtPressure) return;
        int delta = (int) ((srcPressure - tgtPressure) * Math.min(source.getVolume(), target.getVolume()) / 2);
        if (delta <= 0) return;
        delta = Math.min(delta, source.getAir());
        source.addAir(-delta);
        target.addAir(delta);
    }

    public class PressureRecipeHandler extends RecipeHandlerTrait<PressureAir> {
        protected PressureRecipeHandler() {
            super(PNCPressureAirHandlerTrait.this, PNCPressureAirRecipeCapability.CAP);
        }

        @Override
        public List<PressureAir> handleRecipeInner(IO io, MBDRecipe recipe, List<PressureAir> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var target = simulate ? handler.copy() : handler;
            var remaining = new ArrayList<>(left);
            if (io == IO.IN) {
                var iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    var pressureAir = iterator.next();
                    float air = pressureAir.value();
                    if (!pressureAir.isAir()) {
                        air = target.getVolume() * air;
                    }
                    int leftAir = target.getAir();
                    if (air > leftAir) {
                        continue;
                    }
                    target.addAir((int) -air);
                    iterator.remove();
                }
            } else if (io == IO.OUT) {
                var iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    var pressureAir = iterator.next();
                    float pressure = pressureAir.value();
                    if (pressureAir.isAir()) {
                        pressure = pressure / target.getVolume();
                    }
                    float leftPressure = target.maxPressure() - target.getPressure();
                    if (pressure > leftPressure) {
                        continue;
                    }
                    int air = (int) (pressure * target.getVolume());
                    target.addAir(air);
                    iterator.remove();
                }
            }
            return remaining.isEmpty() ? null : remaining;
        }
    }
}

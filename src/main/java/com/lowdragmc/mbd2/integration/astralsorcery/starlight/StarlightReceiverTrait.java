package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.lib.DataAS;
import hellfirepvp.astralsorcery.common.starlight.StarlightNetworkLevelHelper;
import hellfirepvp.astralsorcery.common.starlight.StarlightNetworkTickHelper;
import hellfirepvp.astralsorcery.common.starlight.api.ITransmissionTickable;
import hellfirepvp.astralsorcery.common.starlight.api.TransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.StarlightTransmissionLevelHelper;
import hellfirepvp.astralsorcery.common.starlight.transmission.StarlightTransmissionPacket;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Buffers the starlight lenses deliver to the machine, which stands on the starlight network as a
 * {@link MachineStarlightReceiverNode}.
 */
@Getter
public class StarlightReceiverTrait extends RecipeCapabilityTrait {
    @Persisted
    public final StarlightBuffer buffer;
    private final StarlightRecipeHandler recipeHandler = new StarlightRecipeHandler();

    public final RuntimeValue<Float> capacity =
            runtimeValues.ofFloat("capacity", () -> getDefinition().getCapacity())
                    .onChanged(() -> {
                        if (getBuffer().setCapacity(this.capacity.get())) {
                            notifyListeners();
                        }
                    });
    /** Constellation ids whose starlight is kept; empty keeps all. */
    public final RuntimeValue<List<String>> constellations =
            runtimeValues.ofStringList("constellations", () -> getDefinition().getConstellations().stream()
                            .map(ResourceLocation::toString).toList())
                    .onChanged(() -> getBuffer().setFilter(resolveFilter()));

    public StarlightReceiverTrait(MBDMachine machine, StarlightReceiverTraitDefinition definition) {
        super(machine, definition);
        buffer = new StarlightBuffer(definition.getCapacity(), resolveFilter());
        buffer.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public StarlightReceiverTraitDefinition getDefinition() {
        return (StarlightReceiverTraitDefinition) super.getDefinition();
    }

    public List<ResourceLocation> resolveFilter() {
        return constellations.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .toList();
    }

    public void receive(StarlightTransmissionPacket packet) {
        var id = AstralSorceryIntegration.constellationId(packet.constellation());
        buffer.receive(id, packet.amount(), false);
    }

    @Override
    public void onLoadingTraitInPreview() {
        var filter = resolveFilter();
        buffer.setAmount(filter.isEmpty() ? AstralSorceryIntegration.defaultConstellationId() : filter.getFirst(),
                buffer.getCapacity() / 2);
    }

    @Override
    public void serverTick() {
        if (getMachine().getLevel() instanceof ServerLevel level
                && getMachine().getOffsetTimer() % AstralSorceryIntegration.NODE_SYNC_INTERVAL == 0) {
            syncNetworkNode(level);
        }
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        // see LumenStorage#deserializeNBT
        if (buffer.setCapacity(capacity.get())) {
            notifyListeners();
        }
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        if (getMachine().getLevel() instanceof ServerLevel level) {
            var pos = getMachine().getPos();
            StarlightNetworkLevelHelper.get(level).getNode(pos).ifPresent(node -> {
                if (node instanceof MachineStarlightReceiverNode) {
                    DataAS.DOMAIN_AS.getData(level, DataAS.KEY_STARLIGHT_NETWORK_DATA).removeTransmissionNode(pos);
                    notifyNodeChange(level, node);
                }
            });
        }
    }

    /**
     * The handler has to be notified after adding the node: a lens already linked to this block cached it as a
     * plain endpoint and only rebuilds its chain on {@code notifyNodeChange}.
     */
    public void syncNetworkNode(ServerLevel level) {
        var pos = getMachine().getPos();
        var existing = StarlightNetworkLevelHelper.get(level).getNode(pos).orElse(null);
        if (existing instanceof MachineStarlightReceiverNode) return;
        var data = DataAS.DOMAIN_AS.getData(level, DataAS.KEY_STARLIGHT_NETWORK_DATA);
        if (existing != null) {
            // stale node of a block that stood here before; removed as StarlightNetworkLevelHelper#removeNetworkNode does
            data.removeTransmissionNode(pos);
            if (existing instanceof ITransmissionTickable tickable) {
                StarlightNetworkTickHelper.getInstance().removeNodeUpdate(level, tickable);
            }
            notifyNodeChange(level, existing);
        }
        var node = new MachineStarlightReceiverNode(pos);
        data.addTransmissionNode(node);
        notifyNodeChange(level, node);
    }

    private static void notifyNodeChange(ServerLevel level, TransmissionNode node) {
        StarlightTransmissionLevelHelper.getInstance().getHandler(level)
                .ifPresent(handler -> handler.notifyNodeChange(node));
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    public class StarlightRecipeHandler extends RecipeHandlerTrait<StarlightAmount> {
        protected StarlightRecipeHandler() {
            super(StarlightReceiverTrait.this, AstralStarlightRecipeCapability.CAP);
        }

        @Override
        public List<StarlightAmount> handleRecipeInner(IO io, MBDRecipe recipe, List<StarlightAmount> left, @Nullable String slotName, boolean simulate) {
            if (io != IO.IN || !compatibleWith(io)) return left;
            var target = simulate ? buffer.copy() : buffer;
            var remaining = new ArrayList<StarlightAmount>();
            for (var content : left) {
                var missing = content.amount() - target.consume(content, false);
                if (missing > StarlightBuffer.EPSILON) {
                    remaining.add(content.withAmount(missing));
                }
            }
            return remaining.isEmpty() ? null : remaining;
        }
    }
}

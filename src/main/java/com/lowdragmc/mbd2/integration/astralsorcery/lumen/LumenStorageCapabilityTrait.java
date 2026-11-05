package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenNetworkHelper;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenNode;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenRequestHelper;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A lumen buffer that can join Astral Sorcery's pull-only lumen network: as a {@code SOURCE} node advertising what
 * it holds ({@link #provideToNetwork}), and as a requester topping itself up like an altar ({@link #drawFromNetwork}).
 * Recipes only ever see the buffer.
 */
@Getter
public class LumenStorageCapabilityTrait extends SimpleCapabilityTrait<ILumenHandler, @Nullable Direction> {
    @Persisted
    public final LumenStorage storage;
    private final LumenRecipeHandler recipeHandler = new LumenRecipeHandler();

    public final RuntimeValue<Integer> capacity =
            runtimeValues.ofInt("capacity", () -> getDefinition().getCapacity())
                    .onChanged(() -> {
                        if (getStorage().setCapacity(this.capacity.get())) {
                            notifyListeners();
                        }
                    });
    /** Lumen ids that may be put in, and the ones {@link #drawFromNetwork} asks for. */
    public final RuntimeValue<List<String>> lumenTypes =
            runtimeValues.ofStringList("lumen_types", () -> getDefinition().getLumenTypes().stream()
                            .map(ResourceLocation::toString).toList())
                    .onChanged(() -> getStorage().setFilter(resolveFilter()));
    public final RuntimeValue<Integer> maxReceive =
            runtimeValues.ofInt("max_receive", () -> getDefinition().getMaxReceive())
                    .onChanged(() -> {
                        // baked into the wrapper handed out at capability-resolution time
                        getMachine().invalidateCapabilities();
                        getMachine().notifyBlockUpdate();
                    });
    public final RuntimeValue<Integer> maxExtract =
            runtimeValues.ofInt("max_extract", () -> getDefinition().getMaxExtract())
                    .onChanged(() -> {
                        getMachine().invalidateCapabilities();
                        getMachine().notifyBlockUpdate();
                    });
    public final RuntimeValue<Boolean> provideToNetwork =
            runtimeValues.ofBool("provide_to_network", () -> getDefinition().isProvideToNetwork());
    public final RuntimeValue<Boolean> drawFromNetwork =
            runtimeValues.ofBool("draw_from_network", () -> getDefinition().isDrawFromNetwork());
    public final RuntimeValue<Integer> drawInterval =
            runtimeValues.ofInt("draw_interval", () -> getDefinition().getDrawInterval());

    public LumenStorageCapabilityTrait(MBDMachine machine, LumenStorageCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = new LumenStorage(definition.getCapacity(), resolveFilter());
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public LumenStorageCapabilityTraitDefinition getDefinition() {
        return (LumenStorageCapabilityTraitDefinition) super.getDefinition();
    }

    /** Unknown ids are dropped; empty means any type. */
    public List<Lumen> resolveFilter() {
        return lumenTypes.get().stream()
                .map(ResourceLocation::tryParse)
                .map(AstralSorceryIntegration::lumen)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public ILumenHandler getCapContent(IO capabilityIO) {
        return new LumenHandlerWrapper(storage, capabilityIO, maxReceive.get(), maxExtract.get());
    }

    @Override
    public void onLoadingTraitInPreview() {
        var filter = resolveFilter();
        var lumen = filter.isEmpty() ? AstralSorceryIntegration.lumen(AstralSorceryIntegration.defaultLumenId()) : filter.getFirst();
        if (lumen != null) {
            storage.setAmount(lumen, storage.getCapacity() / 2);
        }
    }

    @Override
    public void serverTick() {
        if (!(getMachine().getLevel() instanceof ServerLevel level)) return;
        var timer = getMachine().getOffsetTimer();
        if (timer % AstralSorceryIntegration.NODE_SYNC_INTERVAL == 0) {
            syncNetworkNode(level);
        }
        if (drawFromNetwork.get() && timer % Math.max(1, drawInterval.get()) == 0) {
            pullFromNetwork(level);
        }
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        // see LumenStorage#deserializeNBT
        if (storage.setCapacity(capacity.get())) {
            notifyListeners();
        }
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        if (getMachine().getLevel() instanceof ServerLevel level) {
            LumenNetworkHelper.getNode(level, getMachine().getPos())
                    .ifPresent(node -> LumenNetworkHelper.removeNode(level, node));
        }
    }

    /**
     * Keep the machine's node in line with {@link #provideToNetwork} and the buffer. The node outlives a chunk
     * unload, as Astral Sorcery's own tiles' nodes do.
     */
    public void syncNetworkNode(ServerLevel level) {
        var pos = getMachine().getPos();
        var existing = LumenNetworkHelper.getNode(level, pos).orElse(null);
        if (!provideToNetwork.get()) {
            if (existing != null) {
                LumenNetworkHelper.removeNode(level, existing);
            }
            return;
        }
        var node = existing != null ? existing
                : LumenNetworkHelper.createNode(level, pos, pos.getCenter(), LumenNode.ConnectionType.SOURCE).orElse(null);
        if (node == null || node.getConnectionType() != LumenNode.ConnectionType.SOURCE) return;
        // requesters drain with a null side, so only advertise what the internal IO gives out; and never offer
        // a type this machine draws, or two drawing machines would keep taking each other's buffer
        Set<Lumen> provided = new HashSet<>();
        if (getCapabilityIO(null).support(IO.OUT)) {
            provided.addAll(storage.getStoredTypes());
            if (drawFromNetwork.get()) {
                provided.removeAll(storage.getFilter());
            }
        }
        if (!provided.equals(node.getProvidedLumenTypes())) {
            LumenNetworkHelper.setNodeProvidedLumenTypes(level, node, provided);
        }
    }

    /**
     * Request each filtered type once, like an altar does.
     *
     * <p>A machine with its own node must request <em>as</em> that node: requested from its position, Astral
     * Sorcery's line-of-sight check from a position to a node standing on it throws ({@code Set.of(pos, pos)}) on
     * the server thread.</p>
     *
     * <p>Not named {@code drawFromNetwork()}, which would clash with the runtime value field under KubeJS.</p>
     */
    public void pullFromNetwork(ServerLevel level) {
        var pos = getMachine().getPos();
        var ownNode = LumenNetworkHelper.getNode(level, pos).orElse(null);
        Predicate<LumenNode> notSelf = node -> !node.getPos().equals(pos);
        for (var lumen : storage.getFilter()) {
            var free = storage.getCapacity(lumen) - storage.getAmount(lumen);
            var want = Math.min(free, maxReceive.get());
            if (want <= 0) continue;
            var request = lumen.stack(want);
            (ownNode != null
                    ? LumenRequestHelper.requestRelayed(level, ownNode, request, notSelf)
                    : LumenRequestHelper.requestRelayed(level, pos, request, notSelf))
                    .ifPresent(chain -> {
                        var source = level.getCapability(ILumenHandler.BLOCK, chain.getEndNode().getPos(), null);
                        if (source == null) return;
                        var drained = source.drain(lumen, want, ILumenHandler.Action.EXECUTE);
                        if (drained.isEmpty()) return;
                        storage.fill(lumen, drained.getAmount(), false);
                        if (getDefinition().isTransferParticles()) {
                            chain.playTransferEffect(level, lumen);
                        }
                    });
        }
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    public class LumenRecipeHandler extends RecipeHandlerTrait<LumenAmount> {
        protected LumenRecipeHandler() {
            super(LumenStorageCapabilityTrait.this, AstralLumenRecipeCapability.CAP);
        }

        /** Simulates on a mutated copy, so two contents of the same type are paid from one amount. */
        @Override
        public List<LumenAmount> handleRecipeInner(IO io, MBDRecipe recipe, List<LumenAmount> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var target = simulate ? storage.copy() : storage;
            var remaining = new ArrayList<LumenAmount>();
            for (var content : left) {
                if (content.amount() <= 0) continue;
                var lumen = content.resolve();
                var moved = lumen == null ? 0 : io == IO.IN
                        ? target.drain(lumen, content.amount(), false)
                        : target.fill(lumen, content.amount(), false);
                if (moved < content.amount()) {
                    remaining.add(content.withAmount(content.amount() - moved));
                }
            }
            return remaining.isEmpty() ? null : remaining;
        }
    }
}

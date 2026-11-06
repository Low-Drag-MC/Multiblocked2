package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.arsnouveau.ArsSourceRecipeCapability;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A machine that holds Source of its own.
 *
 * <p>Structurally the Ars Nouveau twin of
 * {@link com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTrait} — a buffer, a
 * capability handed out per side, auto-IO, and a recipe handler — plus one thing energy has no
 * equivalent of: {@link MachineSourceProvider}, which is what makes the rest of Ars Nouveau see the
 * machine at all. Read that class before changing anything about the two-audiences split.</p>
 */
@Getter
public class SourceStorageCapabilityTrait extends SimpleCapabilityTrait<ISourceCap, @Nullable Direction> implements IAutoIOTrait {
    @Persisted
    @DescSynced
    @ConditionalSynced(methodName = "shouldSyncStorage")
    public final CopiableSourceStorage storage;
    private final ArsSourceRecipeHandler recipeHandler = new ArsSourceRecipeHandler();
    private final MachineSourceProvider sourceProvider = new MachineSourceProvider(this);
    /** Whether {@link #sourceProvider} has been handed to {@link SourceManager} for this level yet. */
    private boolean providerRegistered;

    // per-machine overrides of the values authored on the definition
    // @Getter(NONE): the class carries a Lombok @Getter, which would otherwise republish getAutoIO()
    // returning RuntimeAutoIO — the name of the old @Nullable AutoIO method, with a different type and
    // without its "null when disabled" contract. getRuntimeAutoIO() is the only door.
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeAutoIO autoIO =
            new RuntimeAutoIO(runtimeValues, "auto_io", () -> getDefinition().getAutoIO());
    public final RuntimeValue<Integer> maxReceive =
            runtimeValues.ofInt("max_receive", () -> getDefinition().getMaxReceive())
                    .onChanged(() -> {
                        // the value is baked into the wrapper handed out at capability-resolution time, and a
                        // neighbour's BlockCapabilityCache keeps that wrapper until the position is invalidated
                        getMachine().invalidateCapabilities();
                        getMachine().notifyBlockUpdate();
                    });
    public final RuntimeValue<Integer> maxExtract =
            runtimeValues.ofInt("max_extract", () -> getDefinition().getMaxExtract())
                    .onChanged(() -> {
                        getMachine().invalidateCapabilities();
                        getMachine().notifyBlockUpdate();
                    });
    /**
     * Buffer size, for a machine whose tier should hold more than its definition says.
     * <p>
     * Not read live, unlike its two siblings: {@link CopiableSourceStorage} owns it, and the stored
     * source has to be clamped when it shrinks. The hook resizes the storage instead, which is also why
     * {@link #createStorage} still reads the definition — at construction time there is no override to
     * read yet, and the hook applies one the moment NBT restores it.
     */
    public final RuntimeValue<Integer> capacity =
            runtimeValues.ofInt("capacity", () -> getDefinition().getCapacity())
                    .onChanged(() -> {
                        if (getStorage().setCapacity(this.capacity.get())) {
                            notifyListeners();
                            getMachine().invalidateCapabilities();
                            getMachine().notifyBlockUpdate();
                        }
                    });
    /**
     * Whether Ars Nouveau's own devices may treat this machine as a Source Jar.
     * <p>
     * Registering is one-way — {@link SourceManager} has no removal — so turning it off works by the
     * provider reporting itself invalid, which the manager sweeps out on its next 60-tick pass. Turning
     * it back on re-registers.
     */
    public final RuntimeValue<Boolean> exposeToDevices =
            runtimeValues.ofBool("expose_to_devices", () -> getDefinition().isExposeToDevices())
                    .onChanged(this::syncProviderRegistration);

    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<ISourceCap, @Nullable Direction>>> nearbyCache = new HashMap<>();

    public SourceStorageCapabilityTrait(MBDMachine machine, SourceStorageCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorage();
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public SourceStorageCapabilityTraitDefinition getDefinition() {
        return (SourceStorageCapabilityTraitDefinition) super.getDefinition();
    }

    /**
     * The buffer is built without transfer limits of its own; {@link #getCapContent} applies the
     * author's {@code maxReceive}/{@code maxExtract} on the way out instead. The machine's own recipes
     * and the {@link MachineSourceProvider} are not "transfers" and should not be rate-limited — the
     * same split {@code CopiableEnergyStorage} makes.
     */
    protected CopiableSourceStorage createStorage() {
        var capacity = getDefinition().getCapacity();
        return new CopiableSourceStorage(capacity, capacity, capacity);
    }

    @Override
    public ISourceCap getCapContent(IO capabilityIO) {
        return new SourceCapWrapper(storage, capabilityIO, maxReceive.get(), maxExtract.get());
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.receiveSource(getDefinition().getCapacity() / 2, false);
    }

    /**
     * The effective toggle — {@link MachineSourceProvider#isValid()} reads it every prune pass.
     * <p>
     * Not named {@code exposeToDevices()}: that would collide with the {@link #exposeToDevices} slot
     * field, and KubeJS/Rhino resolves {@code trait.exposeToDevices} to one of the two — see the note on
     * {@code AuraHandlerTrait.radiusBlocks()}.
     */
    public boolean exposedToDevices() {
        return exposeToDevices.get();
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        providerRegistered = false;
        syncProviderRegistration();
    }

    @Override
    public void onMachineUnLoad() {
        super.onMachineUnLoad();
        sourceProvider.onUnload();
        providerRegistered = false;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        sourceProvider.onUnload();
        providerRegistered = false;
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        sourceProvider.onUnload();
        providerRegistered = false;
    }

    /**
     * Put the provider into {@link SourceManager} if it belongs there and is not already in.
     * <p>
     * Server-side only: the manager's map is a plain global keyed by dimension id, so a client level
     * would register a second, unreachable machine under the same key.
     */
    private void syncProviderRegistration() {
        if (!exposedToDevices()) {
            // Forget that we ever registered. The provider reports itself invalid from here on and
            // SourceManager drops it on its next 60-tick sweep, so turning the toggle back on has to be free to
            // re-add it — leaving this true would leave a pruned provider that nothing ever re-registers.
            providerRegistered = false;
            return;
        }
        if (providerRegistered) return;
        if (getMachine().getLevel() instanceof ServerLevel serverLevel) {
            sourceProvider.onLoad();
            // a HashSet of one instance: re-adding a provider that was never pruned is a no-op
            SourceManager.INSTANCE.addInterface(serverLevel, sourceProvider);
            providerRegistered = true;
        }
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public RuntimeAutoIO getRuntimeAutoIO() {
        return autoIO;
    }

    @Nonnull
    public BlockCapabilityCache<ISourceCap, @Nullable Direction> getNearbyCache(ServerLevel serverLevel,
                                                                               BlockPos pos,
                                                                               @Nonnull Direction side) {
        return nearbyCache.computeIfAbsent(pos, blockPos -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(CapabilityRegistry.SOURCE_CAPABILITY,
                        serverLevel,
                        pos, direction
                ));
    }

    /**
     * Push/pull with whatever exposes {@code ars_nouveau:source} next door — a Source Jar, a relay, or
     * another MBD2 machine. Ars Nouveau has no source pipes, so this is the only way two adjacent
     * blocks move source without a player linking them with a Dominion Wand.
     *
     * <p>{@code port} is the machine's own block, not the neighbour's — {@link IAutoIOTrait#serverTick()}
     * passes {@code getMachine().getPos()} and {@code IProxyAutoIOTrait.handleProxyAutoIO} passes the
     * proxying block. The neighbour is {@code port.relative(side)}, asked about the face pointing back at
     * us, exactly as the item and fluid traits do it. Getting this wrong resolves the machine's own
     * capability and moves source from the buffer into itself, which is a silent no-op.</p>
     */
    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (getMachine().getLevel() instanceof ServerLevel serverLevel) {
            var neighbour = getNearbyCache(serverLevel, port.relative(side), side.getOpposite()).getCapability();
            if (neighbour == null) return;
            if (io.support(IO.IN)) {
                neighbour.extractSource(
                        storage.receiveSource(neighbour.extractSource(maxReceive.get(), true),
                                false),
                        false);
            }
            if (io.support(IO.OUT)) {
                neighbour.receiveSource(
                        storage.extractSource(neighbour.receiveSource(maxExtract.get(), true),
                                false),
                        false);
            }
        }
    }

    public boolean shouldSyncStorage(CopiableSourceStorage value) {
        return getDefinition().getFancyRendererSettings().isEnable();
    }

    public class ArsSourceRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected ArsSourceRecipeHandler() {
            super(SourceStorageCapabilityTrait.this, ArsSourceRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            int required = left.stream().reduce(0, Integer::sum);
            var capability = simulate ? storage.copy() : storage;
            if (io == IO.IN) {
                required -= capability.extractSource(required, simulate);
            } else {
                required -= capability.receiveSource(required, simulate);
            }
            return required > 0 ? List.of(required) : null;
        }
    }
}

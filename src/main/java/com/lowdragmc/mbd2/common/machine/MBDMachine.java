package com.lowdragmc.mbd2.common.machine;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.MultiManagedStorage;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.IAnimationSource;
import com.lowdragmc.mbd2.api.capability.recipe.*;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.client.MachineSound;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintInstance;
import com.lowdragmc.mbd2.common.gui.MBDBindingIDs;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import com.lowdragmc.mbd2.common.machine.fx.IMachineFXManager;
import com.lowdragmc.mbd2.common.machine.fx.MachineFXManagers;
import com.lowdragmc.mbd2.common.runtime.IRuntimeValueHolder;
import com.lowdragmc.mbd2.common.runtime.RuntimeSignalConnection;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.runtime.RuntimeValueStorage;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.emi.MBDEMIPlugin;
import com.lowdragmc.mbd2.integration.geckolib.AnimatableBlock;
import com.lowdragmc.mbd2.integration.geckolib.GeckolibRenderer;
import com.lowdragmc.mbd2.integration.jei.MBDJEIPlugin;
import com.lowdragmc.mbd2.integration.rei.MBDREIPlugin;
import com.lowdragmc.mbd2.utils.RendererUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.List;

@Getter
public class MBDMachine implements IMachine, IAnimationSource, IBlockEntityManaged, IRuntimeValueHolder, BlockUIMenuType.BlockUI {
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    private final MBDMachineDefinition definition;
    private final IMachineBlockEntity machineHolder;

    @Getter
    @Setter
    @Persisted
    @DescSynced
    private Component customName = null;
    @Persisted
    @DescSynced
    @UpdateListener(methodName = "updateCustomData")
    @Setter
    private CompoundTag customData = new CompoundTag();
    @Persisted
    @DescSynced
    private final RecipeLogic recipeLogic;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> recipeCapabilitiesProxy;
    @Nonnull
    @Persisted
    @DescSynced
    @UpdateListener(methodName = "updateState")
    private String machineState;
    @Getter
    private final List<ITrait> additionalTraits = new ArrayList<>();
    private final Map<IRenderer, Object> animatableCache = new HashMap<>(); // see IAnimationSource
    /**
     * @deprecated superseded by the {@link #machineLevel} runtime value. Still read from NBT so worlds
     *             saved before the runtime value system keep their tier — see
     *             {@link #migrateLegacyRuntimeOverrides()} — but never written to again, so it now
     *             always reads {@code -1}. Scripts that used {@code dynamicMachineLevel >= 0} to ask
     *             "is the tier overridden?" want
     *             {@code getRuntimeValues().isOverridden("machine_level")} instead.
     */
    @Deprecated
    @Persisted
    @Getter
    private int dynamicMachineLevel = -1;
    /**
     * Per-machine overrides of values authored on the shared definition. Declared before any
     * {@link RuntimeValue} field below, because field initialisers run in declaration order.
     * <p>
     * Persisted with the block entity and never synced — see {@link RuntimeValueStorage}.
     * <p>
     * {@code @LazyManaged} only removes the field from LDLib's per-tick dirty sweep; it is still in
     * {@code getPersistedFields()} and still saved. {@link RuntimeValue#set} marks it dirty itself.
     */
    @Persisted
    @LazyManaged
    @Getter
    protected final RuntimeValueStorage runtimeValues = new RuntimeValueStorage(this);
    /**
     * Kept private on purpose: {@code getMachineLevel()} already exists as a no-arg bean property, so a
     * public {@code machineLevel} field would shadow it in KubeJS/Rhino and hand scripts the slot where
     * they expect the int. Scripts go through {@link #setMachineLevel}/{@link #clearMachineLevel} or
     * {@code runtimeValues.set("machine_level", ...)}.
     */
    private final RuntimeValue<Integer> machineLevel =
            runtimeValues.ofInt("machine_level", () -> getDefinition().machineSettings().machineLevel());
    /*
     * Deliberately NOT runtime values: hasUI and showUIOnlyFormed gate shouldOpenUI, which
     * MBDMachineBlock.useWithoutItem calls on BOTH sides, and openUI answers sidedSuccess(isClientSide).
     * Overrides are never transmitted, so a server-side override would make the client read the
     * definition, claim SUCCESS and swing the arm while the server returned PASS and ran the default
     * block/item interaction instead. A value that gates a two-sided interaction has to be one both sides
     * can agree on, which under a persistence-only design means the definition.
     *
     * showUIWhenClickStructure is fine as a runtime value: its only reader is CommonEventListener, which
     * is gated on ServerLevel.
     */
    public final RuntimeValue<Boolean> dropMachineItem =
            runtimeValues.ofBool("drop_machine_item", () -> getDefinition().machineSettings().dropMachineItem());
    public final RuntimeSignalConnection signalConnection =
            new RuntimeSignalConnection(runtimeValues, "signal_connection",
                    () -> getDefinition().machineSettings().signalConnection());
    public final RuntimeValue<Boolean> recipeLogicEnabled =
            runtimeValues.ofBool("recipe_logic.enable", () -> getDefinition().recipeLogicSettings().isEnable());
    /** Private for the same reason as {@link #machineLevel}: {@code getRecipeDampingValue()} already exists. */
    private final RuntimeValue<Integer> recipeDampingValue =
            runtimeValues.ofInt("recipe_logic.damping", () -> getDefinition().recipeLogicSettings().recipeDampingValue());
    public final RuntimeValue<Boolean> alwaysSearchRecipe =
            runtimeValues.ofBool("recipe_logic.always_search", () -> getDefinition().recipeLogicSettings().alwaysSearchRecipe());
    public final RuntimeValue<Boolean> alwaysModifyRecipe =
            runtimeValues.ofBool("recipe_logic.always_modify", () -> getDefinition().recipeLogicSettings().alwaysModifyRecipe());
    // redstone signal
    @Getter
    @Persisted
    @DescSynced
    private final byte[] outputSignal = new byte[6];
    @Getter
    @Persisted
    @DescSynced
    private final byte[] outputDirectSignal = new byte[6];
    @Getter
    @Persisted
    @DescSynced
    private byte analogOutputSignal = 0;
    @Nullable
    @OnlyIn(Dist.CLIENT)
    private MachineSound currentSound;
    /**
     * The blueprints attached to this machine, one per enabled
     * {@link com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding},
     * in the order the definition lists them. Lazily built on first event so a machine that never
     * receives one — or a definition with no blueprints at all — pays nothing.
     */
    @Nullable
    private List<MachineBlueprintInstance> blueprintInstances;
    /**
     * This machine's live Photon effects. Client-side only in practice — the factory hands back
     * {@link IMachineFXManager#NOOP} on a server or without Photon — and built lazily so a machine
     * that never plays one costs nothing.
     *
     * <p>Grouped down here with {@link #currentSound} and {@link #blueprintInstances} rather than up
     * with the managed fields: like those two it is unmanaged per-instance runtime state, rebuilt on
     * load and thrown away on unload, and none of it belongs in the persisted/synced block above.</p>
     *
     * @see #getFXManager()
     */
    @Nullable
    private IMachineFXManager fxManager;
    /**
     * The machine state whose effects are currently playing, or {@code null} before the first sync.
     *
     * @see #syncStateFX()
     */
    @Nullable
    private String fxSyncedState;

    public MBDMachine(IMachineBlockEntity machineHolder, MBDMachineDefinition definition, Object... args) {
        this.machineHolder = machineHolder;
        this.definition = definition;
        // bind sync storage
        if (machineHolder.getRootStorage() instanceof MultiManagedStorage multiManagedStorage) {
            multiManagedStorage.attach(getSyncStorage());
        } else {
            throw new RuntimeException("Root storage of MBDMachine's holder must be MultiManagedStorage");
        }
        recipeCapabilitiesProxy = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);;
        machineState = definition.stateMachine().getRootState().name();
        // trait initialization
        recipeLogic = createRecipeLogic(args);
        // additional traits initialization
        loadAdditionalTraits();
    }

    public Component getMachineName() {
        var customName = getCustomName();
        if (customName == null) return getDefinition().block().getName();
        return customName;
    }

    @Override
    public BlockEntity asBlockEntity() {
        return machineHolder.getSelf();
    }

    @Override
    public void onChunkUnloaded() {
        IMachine.super.onChunkUnloaded();
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onChunkUnloaded();
        }
        releaseFX();
    }

    @Override
    public void onUnload() {
        IMachine.super.onUnload();
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onMachineUnLoad();
        }
        releaseBlueprints();
        releaseFX();
    }

    /**
     * Post an event to this machine's blueprints.
     *
     * <p>Called from {@link MachineEvent#postCustomEvent()}, i.e. from every machine event there is —
     * including the per-tick ones. The empty-definition case therefore has to be free, which is why the
     * instance list is built lazily and the per-blueprint entry lookup is a map hit on the event's exact
     * class rather than a scan.</p>
     *
     * <p>Blueprints run in list order and all of them run: for a cancelable event that makes cancelling
     * a union (any blueprint may cancel), and for a value-modifying event it makes the list a pipeline,
     * since each blueprint reads the event fields the previous one wrote.</p>
     */
    public void postBlueprintEvent(MachineEvent event) {
        var instances = blueprintInstances;
        if (instances == null) {
            instances = blueprintInstances = createBlueprintInstances();
        }
        if (instances.isEmpty()) return;
        var eventClass = event.getClass();
        for (var instance : instances) {
            if (instance.handles(eventClass)) {
                instance.post(event);
            }
        }
    }

    protected List<MachineBlueprintInstance> createBlueprintInstances() {
        var bindings = getDefinition().machineSettings().blueprints();
        if (bindings.isEmpty()) return List.of();
        var instances = new ArrayList<MachineBlueprintInstance>(bindings.size());
        for (var binding : bindings) {
            if (binding.isEnabled() && binding.hasBlueprint()) {
                instances.add(new MachineBlueprintInstance(this, binding));
            }
        }
        return instances;
    }

    /**
     * Drop the blueprint executors and their caches.
     *
     * <p>Set back to {@code null} rather than emptied so a machine that unloads and reloads rebuilds
     * from the definition — which also picks up an edited blueprint without a game restart.</p>
     */
    protected void releaseBlueprints() {
        if (blueprintInstances == null) return;
        for (var instance : blueprintInstances) {
            instance.release();
        }
        blueprintInstances = null;
    }

    //////////////////////////////////////
    //*******    PHOTON FX     *********//
    //////////////////////////////////////

    /**
     * Identifier prefix for the effects a machine state owns.
     *
     * <p>Keeps the two authoring routes from colliding: a state's effects live under
     * {@code state:<fx name>}, while the named library on
     * {@link ConfigMachineSettings#photonFXs()} — and anything a blueprint emits ad hoc — uses the
     * bare name. Without the split, a library entry and a state entry called {@code smoke} would
     * silently fight over one slot.</p>
     */
    public static final String STATE_FX_PREFIX = "state:";

    /**
     * This machine's live effects. Never null; a server machine, or a client without Photon, gets
     * {@link IMachineFXManager#NOOP}.
     */
    public IMachineFXManager getFXManager() {
        if (fxManager == null) {
            fxManager = MachineFXManagers.create(this);
        }
        return fxManager;
    }

    /**
     * Start the current state's effects and stop the previous state's.
     *
     * <p>Driven from {@link #clientTick()} rather than from {@link #updateState}, which is the whole
     * reason state effects work at all for a machine that was already running before the player got
     * there: {@code updateState} only fires on a <em>change</em>, so a furnace that has been smelting
     * since before the chunk was sent would never have started its smoke. The block ticker runs as
     * soon as the chunk loads client-side and {@code machineState} is {@code @DescSynced}, so
     * comparing against {@link #fxSyncedState} covers first sight, state changes and chunk reloads
     * with one idempotent path — which the editor preview then gets for free.</p>
     */
    @OnlyIn(Dist.CLIENT)
    protected void syncStateFX() {
        if (machineState.equals(fxSyncedState)) return;
        fxSyncedState = machineState;
        var fxs = getMachineState().getRealMachineFXs();
        // Read the field, not getFXManager(): a machine whose definition configures no effects — the
        // overwhelming majority in any world — must never allocate a manager at all. The getter is
        // still what the RPC entry points use, so an ad-hoc emit on such a machine still works.
        if (fxs.isEmpty() && fxManager == null) return;
        stopStateFX();
        for (var config : fxs) {
            getFXManager().play(config, STATE_FX_PREFIX + config.getName());
        }
    }

    /**
     * Stop every effect owned by a machine state, leaving library and ad-hoc ones alone.
     *
     * <p>Never forced: a state's effects should trail off as the machine stops, the way its sound
     * does. An effect that wants to vanish the instant its state ends says so with its own
     * {@code forcedDeath}.</p>
     */
    @OnlyIn(Dist.CLIENT)
    protected void stopStateFX() {
        if (fxManager != null) {
            fxManager.stopAllWithPrefix(STATE_FX_PREFIX, false);
        }
    }

    /**
     * Drop every live effect and forget which state they came from, so a machine that unloads and
     * reloads re-reads its definition — which also picks up an edited effect without a restart.
     */
    protected void releaseFX() {
        if (fxManager != null) {
            fxManager.stopAll(true);
            fxManager = null;
        }
        fxSyncedState = null;
    }

    /**
     * on machine valid in the chunk.
     */
    @Override
    public void onLoad() {
        IMachine.super.onLoad();
        migrateLegacyRuntimeOverrides();
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onMachineLoad();
        }
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> NeoForge.EVENT_BUS.post(new MachineOnLoadEvent(this).postCustomEvent())));
        }
    }

    @Override
    public MBDMachine runtimeValueMachine() {
        return this;
    }

    /**
     * Fold overrides saved before the runtime value system into their slots. Runs from {@link #onLoad()},
     * which vanilla drives from {@link net.minecraft.world.level.block.entity.BlockEntity#clearRemoved()}
     * — after the block entity NBT has been read, on the server thread.
     * <p>
     * Not hooked into {@code IMachine#loadCustomPersistedData}: LDLib's {@code IPersistManagedHolder}
     * writes that data under {@code managed.custom} but reads it back from the root {@code custom}, so it
     * does not round-trip.
     */
    private void migrateLegacyRuntimeOverrides() {
        if (!(getLevel() instanceof ServerLevel)) return;
        if (dynamicMachineLevel >= 0 && !machineLevel.isOverridden()) {
            machineLevel.set(dynamicMachineLevel);
        }
        dynamicMachineLevel = -1;
    }

    /**
     * Detach the {@link IManagedStorage} of all traits.
     * <br>
     * Have to call this method while changing the machine instance. e.g. {@link com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity#setMachine(IMachine)}
     */
    public void detach() {
        releaseBlueprints();
        releaseFX();
        if (machineHolder.getRootStorage() instanceof MultiManagedStorage multiManagedStorage) {
            multiManagedStorage.detach(getSyncStorage());
            for (ITrait trait : additionalTraits) {
                if (trait instanceof IManaged managed) {
                    multiManagedStorage.detach(managed.getSyncStorage());
                }
            }
        }
    }

    protected RecipeLogic createRecipeLogic(Object... args) {
        return new RecipeLogic(this);
    }

    /**
     * Whether disable all rendering.
     */
    public boolean isDisableRendering() {
        return false;
    }

    /**
     * Update the machine state from the {@link MBDMachineDefinition#stateMachine()} by the given state name. if no such state found, it will do nothing.
     */
    public void setMachineState(String newState) {
        if (machineState.equals(newState)) return;
        if (definition.stateMachine().hasState(newState)) {
            var event = new MachineStateChangedEvent(this, machineState, newState);
            NeoForge.EVENT_BUS.post(event.postCustomEvent());
            if (!event.isCanceled()) {
                var oldState = machineState;
                machineState = newState;
                notifyBlockUpdate();
                updateState(newState, oldState);
            }
        }
    }

    public void updateCustomData(CompoundTag newValue, CompoundTag oldValue) {
        NeoForge.EVENT_BUS.post(new MachineCustomDataUpdateEvent(this, newValue, oldValue).postCustomEvent());
    }

    public void updateState(String newValue, String oldValue) {
        var hasLightChanged = definition.stateMachine().getState(newValue).getLightLevel() != definition.stateMachine().getState(oldValue).getLightLevel();
        // notify the light engine to update the light value
        if (hasLightChanged) {
            // TODO it doesnt save the light value to the chunk?
            var profilerfiller = getLevel().getProfiler();
            var level = getLevel();
            var pos = getPos();
            int j = pos.getX() & 15;
            int k = pos.getY() & 15;
            int l = pos.getZ() & 15;
            profilerfiller.push("updateSkyLightSources");
            var levelChunk = level.getChunkAt(getPos());
            levelChunk.getSkyLightSources().update(level, j, pos.getY(), l);
            profilerfiller.popPush("queueCheckLight");
            level.getChunkSource().getLightEngine().checkBlock(pos);
            profilerfiller.pop();
        }
        // update sound and renderer
        if (isRemote()) {
            playStateSound(newValue);
            scheduleRenderUpdate();
        }
    }

    /**
     * Load additional traits from the {@link ConfigMachineSettings#traitDefinitions()}.
     * <br>
     * It will attach the {@link IManagedStorage} of all traits for sync/persisted data management.
     * <br>
     * You don't have to call this method manually, it will be called automatically when the machine is created.
     */
    public void loadAdditionalTraits() {
        if (machineHolder.getRootStorage() instanceof MultiManagedStorage multiManagedStorage) {
            for (ITrait trait : additionalTraits) {
                if (trait instanceof IManaged managed) {
                    multiManagedStorage.detach(managed.getSyncStorage());
                }
            }
            additionalTraits.clear();
            var acceptedDefinitions = new ArrayList<TraitDefinition>();
            definition.machineSettings().traitDefinitions().stream().sorted((a, b) -> b.getPriority() - a.getPriority()).forEach(traitDefinition -> {
                if (!traitDefinition.canBeAddedTo(acceptedDefinitions)) return;
                var trait = traitDefinition.createTrait(this);
                if (trait == null) return;
                acceptedDefinitions.add(traitDefinition);
                additionalTraits.add(trait);
                if (trait instanceof IManaged managed) {
                    for (var ref : managed.getSyncStorage().getPersistedFields()) {
                        ref.setPersistedPrefixName("trait." + traitDefinition.getName());
                    }
                    multiManagedStorage.attach(managed.getSyncStorage());
                }
            });
            initCapabilitiesProxy();
        }
    }

    /**
     * Initialize the capabilities proxy for recipe logic. see {@link IRecipeCapabilityHolder#getRecipeCapabilitiesProxy()}
     */
    public void initCapabilitiesProxy() {
        recipeCapabilitiesProxy.clear();
        for (var trait : additionalTraits) {
            for (var recipeHandlerTrait : trait.getRecipeHandlerTraits()) {
                if (!recipeCapabilitiesProxy.contains(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability())) {
                    recipeCapabilitiesProxy.put(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability(), new ArrayList<>());
                }
                recipeCapabilitiesProxy.get(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability()).add(recipeHandlerTrait);
            }
        }
    }

    /**
     * All traits the recipe logic can see: this machine's own traits plus, when it is a formed
     * multiblock controller, the traits of every part.
     * <br>
     * Recipe conditions have to look here instead of {@link #getAdditionalTraits()}: on a
     * multiblock the trait they inspect (rotation, pressure, heat, ...) usually sits on a part,
     * so a controller-only lookup never matches.
     */
    public List<ITrait> getRecipeLogicTraits() {
        if (this instanceof IMultiController controller && controller.isFormed()) {
            var traits = new ArrayList<>(additionalTraits);
            for (var part : controller.getParts()) {
                if (part instanceof MBDMachine partMachine) {
                    traits.addAll(partMachine.getAdditionalTraits());
                }
            }
            return traits;
        }
        return additionalTraits;
    }

    /**
     * Get the Trait Instance by the given trait definition.
     */
    @Nullable
    public ITrait getTraitByDefinition(TraitDefinition traitDefinition) {
        for (var trait : additionalTraits) {
            if (traitDefinition == trait.getDefinition()) {
                return trait;
            }
        }
        return null;
    }

    @Nullable
    public ITrait getTraitByName(String name) {
        for (var trait : additionalTraits) {
            if (trait.getDefinition().getName().equals(name)) {
                return trait;
            }
        }
        return null;
    }

    public <T> T getTraitByName(Class<T> clazz, String name) {
        for (var trait : additionalTraits) {
            if (trait.getDefinition().getName().equals(name) && clazz.isInstance(trait)) {
                return (T) trait;
            }
        }
        return null;
    }

    /**
     * Get the block entity holder.
     */
    @Override
    public BlockEntity getHolder() {
        return machineHolder.getSelf();
    }

    /**
     * Get the random offset.
     */
    @Override
    public long getOffset() {
        return machineHolder.getOffset();
    }

    /**
     * Get the front facing of the machine.
     */
    @Override
    public Optional<Direction> getFrontFacing() {
        return getDefinition().blockProperties().rotationState().property.flatMap(property -> getBlockState().getOptionalValue(property));
    }

    /**
     * Is the facing valid for setup.
     */
    @Override
    public boolean isFacingValid(Direction facing) {
        return getDefinition().blockProperties().rotationState().test(facing);
    }

    /**
     * Set the front facing of the machine.
     */
    @Override
    public void setFrontFacing(Direction facing) {
        var blockState = getBlockState();
        var property = getDefinition().blockProperties().rotationState().property;
        if (property.isPresent() && blockState.hasProperty(property.get()) && isFacingValid(facing)) {
            getLevel().setBlockAndUpdate(getPos(), blockState.setValue(property.get(), facing));
        }
    }

    /**
     * Get the recipe type. which is defined in the {@link com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings#getRecipeType()}.
     */
    @NotNull
    @Override
    public MBDRecipeType getRecipeType() {
        return definition.recipeLogicSettings().getRecipeType();
    }

    /**
     * Called when recipe logic status changed.
     * <br>
     * By default, We will update the machine state to match the recipe logic status.
     */
    @Override
    public void notifyRecipeStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        switch (newStatus) {
            case WORKING -> setMachineState("working");
            case IDLE -> setMachineState(definition.stateMachine().getRootState().name());
            case WAITING -> setMachineState("waiting");
            case SUSPEND -> setMachineState("suspend");
        }
        NeoForge.EVENT_BUS.post(new MachineRecipeStatusChangedEvent(this, oldStatus, newStatus).postCustomEvent());
    }

    /**
     * Get the machine level. it will be used for recipe condition {@link com.lowdragmc.mbd2.common.recipe.MachineLevelCondition} an so on.
     */
    @Override
    public int getMachineLevel() {
        // Clamped because the by-name API is a second door to this slot and does not share
        // setMachineLevel's "negative means clear" rule: runtimeValues.set("machine_level", -1) stores
        // -1. The definition's own field is @ConfigNumber(range = {0, MAX}), so negative is out of
        // contract either way, and MachineLevelCondition and friends should never see one.
        return Math.max(0, machineLevel.get());
    }

    /**
     * Set the machine level dynamically. A negative level clears the override, putting the machine back
     * on the level authored in its definition.
     */
    public void setMachineLevel(int level) {
        if (level < 0) {
            machineLevel.clear();
        } else {
            machineLevel.set(level);
        }
    }

    /**
     * Put the machine back on the level authored in its definition.
     */
    public void clearMachineLevel() {
        machineLevel.clear();
    }

    /**
     * re-render the chunk.
     */
    @Override
    public void scheduleRenderUpdate() {
        IMachine.super.scheduleRenderUpdate();
    }

    public MachineState getMachineState() {
        return definition.getState(machineState);
    }

    public String getMachineStateName() {
        return machineState;
    }

    //////////////////////////////////////
    //****   ANIMATION SOURCE    *******//
    //////////////////////////////////////

    @Override
    public BlockPos getAnimationPos() {
        return getPos();
    }

    @Override
    public Direction getAnimationFacing() {
        return getFrontFacing().orElse(Direction.NORTH);
    }

    @Override
    public String getAnimationState() {
        return getMachineStateName();
    }

    @Override
    public MBDMachine getAnimationEventTarget() {
        return this;
    }

    /**
     * @deprecated since 21.0.12, renamed to {@link #getAnimatableCache()}: it holds the animation
     * instance of whatever renderer animates this block, not a GeckoLib machine animatable.
     */
    @Deprecated(since = "21.0.12", forRemoval = true)
    public Map<IRenderer, Object> getAnimatableMachine() {
        return getAnimatableCache();
    }

    //////////////////////////////////////
    //********       MISC      *********//
    //////////////////////////////////////

    /**
     * RPC, use this method to send custom data to player (client).
     */
    protected void rpcToPlayer(ServerPlayer player, String methodName, Object... args) {
        machineHolder.rpcToPlayer(this, player, methodName, args);
    }

    /**
     * RPC, use this method to send custom data to all players (client) tracking this machine.
     */
    protected void rpcToTracking(String methodName, Object... args) {
        machineHolder.rpcToTracking(this, methodName, args);
    }

    /**
     * Server tick. will be called on server side per tick.
     */
    public void serverTick() {
        var event = new MachineTickEvent(this);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        if (!event.isCanceled()) {
            internalServerTick();
        }
    }

    protected void internalServerTick() {
        if (runRecipeLogic()) {
            recipeLogic.serverTick();
        }
        for (ITrait trait : additionalTraits) {
            trait.serverTick();
        }
    }

    /**
     * Shall we run the recipe logic during the server tick?
     * <br>
     * if the machine has no recipe logic or using the {@link MBDRecipeType#DUMMY}, it will return false.
     */
    public boolean runRecipeLogic() {
        return recipeLogicEnabled.get() && IMachine.super.runRecipeLogic();
    }

    @Override
    public @Nullable MBDRecipe modifyFuelRecipe(MBDRecipe recipe) {
        var event = new MachineFuelRecipeModifyEvent(this, recipe);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        if (event.isCanceled()) return null;
        return event.getRecipe();
    }

    @Override
    public void onFuelBurningFinish(@Nullable MBDRecipe recipe) {
        NeoForge.EVENT_BUS.post(new MachineFuelBurningFinishEvent(this, recipe));
    }

    @Nullable
    @Override
    public MBDRecipe doModifyRecipe(@NotNull MBDRecipe recipe) {
        var before = new MachineRecipeModifyEvent.Before(this, recipe);
        NeoForge.EVENT_BUS.post(before.postCustomEvent());
        recipe = before.getRecipe();
        if (before.isCanceled() || recipe == null) {
            return recipe;
        }
        recipe = IMachine.super.doModifyRecipe(recipe);
        var after = new MachineRecipeModifyEvent.After(this, recipe);
        NeoForge.EVENT_BUS.post(after.postCustomEvent());
        return after.getRecipe();
    }

    /**
     * Override it to modify recipe on the fly e.g. applying overclock, change chance, etc
     * @param recipe recipe from detected from MBDRecipe
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    @Nullable
    @Override
    public MBDRecipe getModifiedRecipe(@Nonnull MBDRecipe recipe) {
        return getDefinition().recipeLogicSettings().recipeModifiers().applyModifiers(getRecipeLogic(), recipe);
    }

    @Override
    public ContentModifier getMaxParallel(@Nonnull MBDRecipe recipe) {
        return getDefinition().recipeLogicSettings().recipeModifiers().getMaxParallel(getRecipeLogic(), recipe);
    }

    /**
     * Always try {@link #doModifyRecipe(MBDRecipe)} before setting up recipe.
     * @return true - will map {@link RecipeLogic#getLastOriginRecipe()} to the latest recipe for next round when finishing.
     * false - keep using the {@link RecipeLogic#getLastRecipe()}, which is already modified.
     */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return !getDefinition().recipeLogicSettings().recipeModifiers().recipeModifiers.isEmpty() || alwaysModifyRecipe.get();
    }

    /**
     * Always re-search recipe when the recipe is finished.
     * @return true - will re-search recipe when the last recipe is finished.
     */
    @Override
    public boolean alwaysReSearchRecipe() {
        return alwaysSearchRecipe.get();
    }

    /**
     * if the recipe handling is waiting, damping value is the decreased ticks of the current progress.
     * @return damping value in tick.
     */
    @Override
    public int getRecipeDampingValue() {
        return recipeDampingValue.get();
    }

    @Override
    public boolean beforeWorking(MBDRecipe recipe) {
        var event = new MachineBeforeRecipeWorkingEvent(this, recipe);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        if (event.isCanceled()) {
            return true;
        }
        return IMachine.super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        var event = new MachineOnRecipeWorkingEvent(this, recipeLogic.getLastRecipe(), recipeLogic.getProgress());
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        if (event.isCanceled()) {
            return true;
        }
        return IMachine.super.onWorking();
    }

    @Override
    public void onWaiting() {
        NeoForge.EVENT_BUS.post(new MachineOnRecipeWaitingEvent(this, recipeLogic.getLastRecipe()).postCustomEvent());
        IMachine.super.onWaiting();
    }

    @Override
    public void afterWorking() {
        NeoForge.EVENT_BUS.post(new MachineAfterRecipeWorkingEvent(this, recipeLogic.getLastRecipe()).postCustomEvent());
        IMachine.super.afterWorking();
    }

    /**
     * The recipe has finished and its outputs have been produced.
     *
     * <p>The last of the four points {@link com.lowdragmc.mbd2.api.recipe.RecipeLogic#onRecipeFinish()}
     * passes through, and the one a blueprint wants for "when a craft completes" — {@code afterWorking}
     * fires <em>before</em> the outputs exist, so a bonus product added there lands in a slot the
     * recipe's own output is about to want.</p>
     */
    @Override
    public void onRecipeFinish() {
        NeoForge.EVENT_BUS.post(
                new MachineOnRecipeFinishEvent(this, recipeLogic.getLastRecipe()).postCustomEvent());
        IMachine.super.onRecipeFinish();
    }

    /** Inputs were consumed after working rather than before — see {@code consumeInputsAfterWorking}. */
    @Override
    public void onConsumeInputsAfterWorking() {
        NeoForge.EVENT_BUS.post(
                new MachineOnConsumeInputsAfterWorkingEvent(this, recipeLogic.getLastRecipe()).postCustomEvent());
        IMachine.super.onConsumeInputsAfterWorking();
    }

    /**
     * Client tick. will be called on client side per tick.
     */
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        NeoForge.EVENT_BUS.post(new MachineClientTickEvent(this).postCustomEvent());
        for (ITrait trait : additionalTraits) {
            trait.clientTick();
        }
        syncStateFX();
        if (currentSound != null && currentSound.loop && currentSound.loopWithShuffle &&
                !Minecraft.getInstance().getSoundManager().isActive(currentSound)) {
            if (currentSound.predicate.getAsBoolean()) {
                currentSound.play();
            } else {
                currentSound = null;
            }
        }
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    public void animateTick(RandomSource random) {
    }

    /**
     * Called when neighbors changed.
     */
    public void onNeighborChanged(net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        NeoForge.EVENT_BUS.post(new MachineNeighborChangedEvent(this, block, fromPos).postCustomEvent());
        for (ITrait trait : additionalTraits) {
            trait.onNeighborChanged(block, fromPos, isMoving);
        }
    }

    /**
     * Called when machine placed by (if exist) an entity with item.
     * it won't be called when machine added by {@link Level#setBlock(BlockPos, BlockState, int, int)}
     */
    public void onMachinePlaced(LivingEntity player, ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            setCustomName(stack.getHoverName());
        }
        NeoForge.EVENT_BUS.post(new MachinePlacedEvent(this, player, stack).postCustomEvent());
    }

    /**
     * Returns the {@link BlockState} that this state reports to look like on the given side for querying by other mods.
     */
    public BlockState getAppearance(BlockState state, Direction side, BlockState queryState, BlockPos queryPos) {
        return state;
    }

    /**
     * Get the shape of this block, as well as collision boxes, it's used for interaction and selection.
     */
    public VoxelShape getShape(CollisionContext pContext) {
        return getMachineState().getShape(getFrontFacing().orElse(Direction.NORTH));
    }

    /**
     * Set output signal.
     */
    public void setOutputSignal(int signal, Direction side) {
        if (!isRemote()) {
            var sig = (byte) Mth.clamp(signal, 0, 15);
            if (outputSignal[side.ordinal()] != sig) {
                outputSignal[side.ordinal()] = sig;
                updateSignal();
            }
        }
    }

    /**
     * Set output direct signal.
     */
    public void setOutputDirectSignal(int signal, Direction side) {
        if (!isRemote()) {
            var sig = (byte) Mth.clamp(signal, 0, 15);
            if (outputDirectSignal[side.ordinal()] != sig) {
                outputDirectSignal[side.ordinal()] = sig;
                updateSignal();
            }
        }
    }

    /**
     * Set output analog signal.
     */
    public void setAnalogOutputSignal(int signal) {
        if (!isRemote()) {
            var sig = (byte) Mth.clamp(signal, 0, 15);
            if (analogOutputSignal != sig) {
                analogOutputSignal = sig;
                updateSignal();
            }
        }
    }

    /**
     * Whether the machine can connect to the redstone from given side
     */
    public boolean canConnectRedstone(Direction direction) {
        if (getOutputSignal(direction) > 0) return true;
        return signalConnection.getConnection(getFrontFacing().orElse(Direction.NORTH), direction);
    }

    /**
     * Get the output signal for the given side.
     */
    public int getOutputSignal(Direction direction) {
        return outputSignal[direction.ordinal()];
    }

    /**
     * Get the direct signal for the given side.
     */
    public int getOutputDirectSignal(Direction direction) {
        return outputDirectSignal[direction.ordinal()];
    }

    /**
     * Call to update output signal.
     * also see {@link #getOutputSignal(Direction)} and
     * {@link #getOutputDirectSignal(Direction)}
     */
     public void updateSignal() {
        // Null-checked: this is reached from a runtime value hook, which fires from
        // RuntimeValueStorage#deserializeNBT — and vanilla loads a block entity's NBT before attaching
        // it to its level, so a machine with a saved signal_connection override has no level here.
        var level = getLevel();
        if (level != null && !level.isClientSide) {
            notifyBlockUpdate();
        }
    }

    /**
     * On machine removed.
     */
    public void onMachineRemoved() {
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onMachineRemoved();
        }
        NeoForge.EVENT_BUS.post(new MachineRemovedEvent(this).postCustomEvent());
    }

    /**
     * Get the drop item when the machine is broken.
     */
    public ItemStack getDropItem() {
        var item = getDefinition().asStack();
        if (customName != null) {
            item.set(DataComponents.CUSTOM_NAME, customName);
        }
        return item;
    }

    /**
     * On machine broken and drops items.
     */
    public void onDrops(Entity entity, List<ItemStack> drops) {
        if (dropMachineItem.get()) {
            var drop = getDropItem();
            if (!drop.isEmpty()) {
                drops.add(drop);
            }
        }
        for (ITrait trait : getAdditionalTraits()) {
            trait.onMachineDrop(entity, drops);
        }
        NeoForge.EVENT_BUS.post(new MachineDropsEvent(this, entity, drops).postCustomEvent());
    }

    /**
     * On use item on the machine.
     */
    public ItemInteractionResult useItemOn(ItemStack item, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var event = new MachineUseItemOnEvent(this, player, hand, hit);
        event.setItemInteractionResult(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        return event.getItemInteractionResult();
    }

    /**
     * On hand is using on the machine.
     */
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var event = new MachineUseWithoutItemEvent(this, player, hit);
        event.setInteractionResult(InteractionResult.PASS);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        return event.getInteractionResult();
    }

    /**
     * Should open UI.
     */
    public boolean shouldOpenUI(BlockHitResult hit) {
        return getDefinition().machineSettings().hasUI();
    }

    /**
     * Try to open UI.
     */
    public InteractionResult openUI(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var event = new MachineOpenUIEvent(this, player);
            NeoForge.EVENT_BUS.post(event.postCustomEvent());
            if (event.isCanceled()) {
                return InteractionResult.PASS;
            }
            BlockUIMenuType.openUI(serverPlayer, getPos());
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }


    /**
     * Create Modular UI.
     */
    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        var ui = getDefinition().machineSettings().uiTemplate().createUI();
        bindMachineUI(ui);
        var event = new MachineUIEvent(this, ui, holder.player);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        ui = event.getUi();
        if (ui == null) {
            return null;
        }
        return new ModularUI(ui, holder.player);
    }

    /**
     * Binds the user interface (UI) to the machine, allowing the UI elements to reflect
     * the machine's current state and characteristics. It initializes various UI components
     * such as text, progress bars, fuel bars, and buttons with machine-specific data.
     * Additionally, it integrates trait-based UI configurations and manages proxy-controller
     * specific UI setups for part machines, if applicable.
     *
     * @param ui The UI instance that represents the user interface for the machine.
     */
    protected void bindMachineUI(UI ui) {
        ui.selectId(MBDBindingIDs.MACHINE_NAME, TextElement.class).forEach(text -> text.setText(getMachineName()));
        ui.selectId(MBDBindingIDs.PROGRESS_BAR, ProgressBar.class).forEach(progressBar -> {
            progressBar.bind(DataBindingBuilder.floatValS2C(() -> getRecipeLogic().getProgressPercent()).build());
            progressBar.label.bindDataSource(SupplierDataSource.of(() -> Component.literal(String.format("%.2f%%", getRecipeLogic().getProgressPercent() * 100))));
        });
        ui.selectId(MBDBindingIDs.FUEL_BAR, ProgressBar.class).forEach(progressBar ->
                progressBar.bind(DataBindingBuilder.floatValS2C(() -> getRecipeLogic().getFuelProgressPercent()).build())
        );
        ui.selectId(MBDBindingIDs.XEI_LOOKUP, Button.class).forEach(button -> button.setOnClick(event -> {
            var recipeType = getRecipeType();
            if (recipeType != MBDRecipeType.DUMMY && recipeType.isXEIVisible()) {
                if (LDLib2.isReiLoaded()) {
                    MBDREIPlugin.lookupRecipeType(recipeType);
                } else if (LDLib2.isJeiLoaded()) {
                    MBDJEIPlugin.lookupRecipeType(recipeType);
                } else if (LDLib2.isEmiLoaded()) {
                    MBDEMIPlugin.lookupRecipeType(recipeType);
                }
            }
        }));

        for (var trait : getAdditionalTraits()) {
            if (trait.getDefinition() instanceof IUIProviderTrait provider) {
                provider.initTraitUI(trait, ui);
            }
        }
    }

    @Override
    public boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        return BlockUIMenuType.BlockUI.super.stillValid(holder) && !isInValid();
    }

    @Override
    public Component getUIDisplayName(BlockUIMenuType.BlockUIHolder holder) {
        return customName == null ? BlockUIMenuType.BlockUI.super.getUIDisplayName(holder) : customName;
    }

    public boolean isRemote() {
        var level = getLevel();
        return level == null ? LDLib2.isRemote() : level.isClientSide;
    }

    /**
     * It's used to define a visible box for BlockEntityRenderer in the world.
     * @return null, use the default bounding box based on the shape.
     */
    @Nullable
    public AABB getRenderBoundingBox() {
        var aabb = getMachineState().getRenderingBox(getFrontFacing().orElse(Direction.NORTH));
        if (aabb != null) {
            // offset the box to the block position
            aabb = aabb.move(getPos());
            return aabb;
        }
        return null;
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public MachineSound getCurrentSound() {
        return currentSound;
    }

    /**
     * Play the sound by the given state.
     */
    @OnlyIn(Dist.CLIENT)
    public void playStateSound(String state) {
        if (getDefinition().stateMachine().hasState(state)) {
            currentSound = definition.stateMachine().getState(state).createMachineSound(getPos(), () -> IMachine
                    .ofMachine(getLevel(), getPos())
                    .map(m -> m == this && ((MBDMachine) m).machineState.equals(state))
                    .orElse(false));
            if (currentSound != null) {
                currentSound.play();
            }
        }
    }

    public void triggerGeckolibAnim(String animName, float speed) {
        triggerGeckolibAnim("", animName, speed);
    }

    /**
     * Trigger the geckolib animation by name.
     * <br>
     * It's safe to call this method on both side.
     */
    @RPCMethod
    public void triggerGeckolibAnim(String controllerName, String animName, float speed) {
        if (!MBD2.isGeckolibLoaded()) {
            return;
        }
        if (isRemote()) {
            if (controllerName == null || controllerName.isEmpty()) {
                controllerName = AnimatableBlock.DEFAULT_CONTROLLER;
            }
            // renderers picked from the editor's resource panel are wrapped in a UIResourceRenderer,
            // so resolve the wrapper before checking for the geckolib one.
            if (RendererUtils.resolve(getMachineState().getRealRenderer()) instanceof GeckolibRenderer renderer) {
                var controller = renderer.getAnimatable(this).getAnimatableInstanceCache()
                        .getManagerForId(0)
                        .getAnimationControllers()
                        .get(controllerName);
                if (controller != null) {
                    controller.setAnimationSpeed(Math.max(speed, 0));
                    controller.tryTriggerAnimation(animName);
                }
            }
        } else {
            rpcToTracking("triggerGeckolibAnim", controllerName, animName, speed);
        }
    }

    /**
     * Play one of the machine's named effects — an entry in
     * {@link ConfigMachineSettings#photonFXs()}.
     *
     * <p>Safe from either side, like {@link #triggerGeckolibAnim}: called on the server it is relayed
     * to every tracking client, called on the client it plays locally. Unknown names do nothing.</p>
     *
     * <p>Only reaches players who are tracking the chunk <em>now</em> — which is why an effect that
     * should be visible to whoever turns up later belongs on a machine state instead. See
     * {@link #syncStateFX()}.</p>
     */
    @RPCMethod
    public void playMachineFX(String name) {
        if (isRemote()) {
            var config = getDefinition().machineSettings().findFX(name);
            if (config != null) {
                getFXManager().play(config, name);
            }
        } else {
            rpcToTracking("playMachineFX", name);
        }
    }

    /**
     * Stop the effect playing under {@code identifier}, whatever started it — a named library entry
     * via {@link #playMachineFX} or an ad-hoc {@link #emitPhotonFx}, which share one slot namespace.
     *
     * @param forcedDeath drop the remaining particles immediately instead of letting them drain.
     */
    @RPCMethod
    public void stopMachineFX(String identifier, boolean forcedDeath) {
        if (isRemote()) {
            getFXManager().stop(identifier, forcedDeath);
        } else {
            rpcToTracking("stopMachineFX", identifier, forcedDeath);
        }
    }

    /**
     * Play an effect that is not in the machine's library, described inline.
     *
     * <p>For a blueprint that computes its effect rather than picking one — otherwise prefer
     * {@link #playMachineFX}, which is authorable and previewable in the editor.</p>
     */
    @RPCMethod
    public void emitPhotonFx(String identifier, ResourceLocation fxLocation, Vector3f offset,
                             Vector3f rotation, Vector3f scale, int delay, boolean forcedDeath,
                             boolean replaceExisting) {
        if (isRemote()) {
            var config = new MachineFXConfig(identifier, fxLocation);
            config.setOffset(offset == null ? new Vector3f() : offset);
            config.setRotation(rotation == null ? new Vector3f() : rotation);
            config.setScale(scale == null ? new Vector3f(1, 1, 1) : scale);
            config.setDelay(delay);
            config.setForcedDeath(forcedDeath);
            config.setReplaceExisting(replaceExisting);
            getFXManager().play(config, identifier);
        } else {
            rpcToTracking("emitPhotonFx", identifier, fxLocation, offset, rotation, scale, delay,
                    forcedDeath, replaceExisting);
        }
    }
}

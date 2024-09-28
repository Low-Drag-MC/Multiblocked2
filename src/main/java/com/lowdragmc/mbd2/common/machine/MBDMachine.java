package com.lowdragmc.mbd2.common.machine;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.*;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.*;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.gui.factory.MachineUIFactory;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.geckolib.GeckolibRenderer;
import com.lowdragmc.mbd2.integration.photon.MachineFX;
import com.lowdragmc.photon.client.fx.FXHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.*;

@Getter
public class MBDMachine implements IMachine, IEnhancedManaged, ICapabilityProvider, IUIHolder {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MBDMachine.class);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        this.markDirty();
    }

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    private final MBDMachineDefinition definition;
    private final IMachineBlockEntity machineHolder;

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
    @Getter
    private Map<IRenderer, Object> animatableMachine = new HashMap<>(); // it's used for Geckolib
    @Getter
    private Map<String, Object> photonFXs = new HashMap<>(); // it's used for Photon

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

    @Override
    public void onUnload() {
        IMachine.super.onUnload();
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onMachineLoad();
        }
    }

    /**
     * on machine valid in the chunk.
     */
    @Override
    public void onLoad() {
        IMachine.super.onLoad();
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onMachineLoad();
        }
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> MinecraftForge.EVENT_BUS.post(new MachineOnLoadEvent(this).postGraphEvent())));
        }
    }

    /**
     * Detach the {@link com.lowdragmc.lowdraglib.syncdata.IManagedStorage} of all traits.
     * <br>
     * Have to call this method while changing the machine instance. e.g. {@link com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity#setMachine(IMachine)}
     */
    public void detach() {
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
     * @param newState
     */
    public void setMachineState(String newState) {
        if (machineState.equals(newState)) return;
        if (definition.stateMachine().hasState(newState)) {
            var event = new MachineStateChangedEvent(this, machineState, newState).postGraphEvent();
            MinecraftForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                var oldState = machineState;
                machineState = newState;
                notifyBlockUpdate();
                updateState(newState, oldState);
            }
        }
    }

    public void updateCustomData(CompoundTag newValue, CompoundTag oldValue) {
        MinecraftForge.EVENT_BUS.post(new MachineCustomDataUpdateEvent(this, newValue, oldValue).postGraphEvent());
    }

    public void updateState(String newValue, String oldValue) {
        var hasLightChanged = definition.stateMachine().getState(newValue).getLightLevel() != definition.stateMachine().getState(oldValue).getLightLevel();
        // notify the light engine to update the light value
        if (hasLightChanged) {
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
        scheduleRenderUpdate();
    }

    /**
     * Load additional traits from the {@link ConfigMachineSettings#traitDefinitions()}.
     * <br>
     * It will attach the {@link com.lowdragmc.lowdraglib.syncdata.IManagedStorage} of all traits for sync/persisted data management.
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
            definition.machineSettings().traitDefinitions().stream().sorted((a, b) -> b.getPriority() - a.getPriority()).forEach(traitDefinition -> {
                var trait = traitDefinition.createTrait(this);
                additionalTraits.add(trait);
                if (trait instanceof IManaged managed) {
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
            if (trait instanceof IRecipeHandlerTrait<?> recipeHandlerTrait) {
                if (!recipeCapabilitiesProxy.contains(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability())) {
                    recipeCapabilitiesProxy.put(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability(), new ArrayList<>());
                }
                recipeCapabilitiesProxy.get(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability()).add(recipeHandlerTrait);
            }
        }
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
     * Get the recipe type. which is defined in the {@link ConfigMachineSettings#getRecipeType()}.
     */
    @NotNull
    @Override
    public MBDRecipeType getRecipeType() {
        return definition.machineSettings().getRecipeType();
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
        MinecraftForge.EVENT_BUS.post(new MachineRecipeStatusChangedEvent(this, oldStatus, newStatus).postGraphEvent());
    }

    /**
     * Get the machine level. it will be used for recipe condition {@link com.lowdragmc.mbd2.common.recipe.MachineLevelCondition} an so on.
     */
    @Override
    public int getMachineLevel() {
        return getDefinition().machineSettings().machineLevel();
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

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        List<T> results = new ArrayList<>();
        for (var trait : additionalTraits) {
            if (trait instanceof ICapabilityProviderTrait<?> capabilityProviderTrait && capabilityProviderTrait.getCapability() == cap) {
                var io = capabilityProviderTrait.getCapabilityIO(side);
                if (io != IO.NONE) {
                    results.add((T) capabilityProviderTrait.getCapContent(io));
                }
            }
        }
        if (results.isEmpty()) {
            return LazyOptional.empty();
        } else {
            if (results.size() == 1) {
                return LazyOptional.of(() -> results.get(0));
            } else {
                for (var trait : additionalTraits) {
                    if (trait instanceof ICapabilityProviderTrait capabilityProviderTrait && capabilityProviderTrait.getCapability() == cap) {
                        return LazyOptional.of(() -> (T) capabilityProviderTrait.mergeContents(results));
                    }
                }
            }
        }
        return cap.orEmpty(cap, LazyOptional.of(() -> results.get(0)));
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
        var event = new MachineTickEvent(this).postGraphEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            if (runRecipeLogic()) {
                recipeLogic.serverTick();
            }
            for (ITrait trait : additionalTraits) {
                trait.serverTick();
            }
        }
    }

    /**
     * Shall we run the recipe logic during the server tick?
     * <br>
     * if the machine has no recipe logic or using the {@link MBDRecipeType#DUMMY}, it will return false.
     */
    public boolean runRecipeLogic() {
        return getDefinition().machineSettings().hasRecipeLogic() && getRecipeType() != MBDRecipeType.DUMMY;
    }

    @Nullable
    @Override
    public MBDRecipe doModifyRecipe(MBDRecipe recipe) {
        return applyParallel(IMachine.super.doModifyRecipe(recipe));
    }

    public MBDRecipe applyParallel(MBDRecipe recipe) {
        if (recipe != null) {
            // apply parallel here
            var maxParallel = getDefinition().machineSettings().maxParallel();
            if (maxParallel.isEnable()) {
                var parallel = maxParallel.getMaxParallel();
                if (parallel > 1) {
                    var result = MBDRecipe.accurateParallel(this, recipe, parallel, maxParallel.isModifyDuration());
                    return result.getFirst();
                }
            }
        }
        return recipe;
    }

    @Override
    public boolean beforeWorking(MBDRecipe recipe) {
        var event = new MachineBeforeRecipeWorkingEvent(this, recipe);
        MinecraftForge.EVENT_BUS.post(event.postGraphEvent());
        if (event.isCanceled()) {
            return false;
        }
        return IMachine.super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        var event = new MachineOnRecipeWorkingEvent(this, recipeLogic.getLastRecipe(), recipeLogic.getProgress());
        MinecraftForge.EVENT_BUS.post(event.postGraphEvent());
        if (event.isCanceled()) {
            return true;
        }
        return IMachine.super.onWorking();
    }

    @Override
    public void onWaiting() {
        MinecraftForge.EVENT_BUS.post(new MachineOnRecipeWaitingEvent(this, recipeLogic.getLastRecipe()).postGraphEvent());
        IMachine.super.onWaiting();
    }

    @Override
    public void afterWorking() {
        MinecraftForge.EVENT_BUS.post(new MachineAfterRecipeWorkingEvent(this, recipeLogic.getLastRecipe()).postGraphEvent());
        IMachine.super.afterWorking();
    }

    /**
     * Client tick. will be called on client side per tick.
     */
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        MinecraftForge.EVENT_BUS.post(new MachineClientTickEvent(this).postGraphEvent());
        for (ITrait trait : additionalTraits) {
            trait.clientTick();
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
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        MinecraftForge.EVENT_BUS.post(new MachineNeighborChangedEvent(this, block, fromPos).postGraphEvent());
    }

    /**
     * Called when machine placed by (if exist) an entity with item.
     * it won't be called when machine added by {@link Level#setBlock(BlockPos, BlockState, int, int)}
     */
    public void onMachinePlaced(LivingEntity player, ItemStack stack) {
        MinecraftForge.EVENT_BUS.post(new MachinePlacedEvent(this, player, stack).postGraphEvent());
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
        return getDefinition().getState(machineState).getShape(getFrontFacing().orElse(Direction.NORTH));
    }

    /**
     * On machine removed.
     */
    public void onMachineRemoved() {
        for (ITrait additionalTrait : additionalTraits) {
            additionalTrait.onMachineRemoved();
        }
        MinecraftForge.EVENT_BUS.post(new MachineRemovedEvent(this).postGraphEvent());
    }

    /**
     * On machine broken and drops items.
     */
    public void onDrops(Entity entity, List<ItemStack> drops) {
        MinecraftForge.EVENT_BUS.post(new MachineDropsEvent(this, entity, drops).postGraphEvent());
    }

    /**
     * On hand is using on the machine.
     */
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var event = new MachineRightClickEvent(this, player, hand, hit);
        event.setInteractionResult(InteractionResult.PASS);
        MinecraftForge.EVENT_BUS.post(event.postGraphEvent());
        return event.getInteractionResult();
    }

    /**
     * Should open UI.
     */
    public boolean shouldOpenUI(InteractionHand hand, BlockHitResult hit) {
        return getDefinition().machineSettings().hasUI();
    }

    /**
     * Try to open UI.
     */
    public InteractionResult openUI(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var event = new MachineOpenUIEvent(this, player);
            MinecraftForge.EVENT_BUS.post(event.postGraphEvent());
            if (event.isCanceled()) {
                return InteractionResult.PASS;
            }
            MachineUIFactory.INSTANCE.openUI(this, serverPlayer);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /**
     * Create Modular UI.
     */
    public ModularUI createUI(Player entityPlayer) {
        var ui = getDefinition().uiCreator().apply(this);
        return new ModularUI(ui, this, entityPlayer);
    }

    @Override
    public boolean isInvalid() {
        return isInValid();
    }

    @Override
    public boolean isRemote() {
        var level = getLevel();
        return level == null ? LDLib.isRemote() : level.isClientSide;
    }

    @Override
    public void markAsDirty() {
        this.markDirty();
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


    public void triggerGeckolibAnim(String animName){
        triggerGeckolibAnim("", animName);
    }

    /**
     * Trigger the geckolib animation by name.
     * <br>
     * It's safe to call this method on both side.
     */
    @RPCMethod
    public void triggerGeckolibAnim(String controllerName, String animName){
        if (MBD2.isGeckolibLoaded()) {
            if (isRemote()) {
                if (controllerName.isEmpty()) {
                    controllerName = "base_controller";
                }
                if (getMachineState().getRenderer() instanceof GeckolibRenderer renderer) {
                    renderer.getAnimatableFromMachine(this).getAnimatableInstanceCache()
                            .getManagerForId(0)
                            .tryTriggerAnimation(controllerName, animName);
                }
            } else {
                rpcToTracking("triggerGeckolibAnim", controllerName, animName);
            }
        }
    }

    /**
     * Emit the photon fx.
     */
    @RPCMethod
    public void emitPhotonFx(String identifier, ResourceLocation fxLocation, Vector3f offset, Vector3f rotation, int delay, boolean forcedDeath){
        if (MBD2.isPhotonLoaded()) {
            if (isRemote()) {
                var fx = FXHelper.getFX(fxLocation);
                if (fx != null) {
                    var machineFX = new MachineFX(fx, identifier, this);
                    machineFX.setOffset(offset.x, offset.y, offset.z);
                    machineFX.setRotation(rotation.x, rotation.y, rotation.z);
                    machineFX.setDelay(delay);
                    machineFX.setForcedDeath(forcedDeath);
                    machineFX.start();
                }
            } else {
                rpcToTracking("emitPhotonFx", identifier, fxLocation, offset, rotation, delay, forcedDeath);
            }
        }
    }

    /**
     * Kill the photon fx.
     */
    @RPCMethod
    public void killPhotonFx(String identifier, boolean forcedDeath) {
        if (MBD2.isPhotonLoaded()) {
            if (isRemote()) {
                if (photonFXs.get(identifier) instanceof MachineFX machineFX) {
                    machineFX.kill(forcedDeath);
                    photonFXs.remove(identifier);
                }
            } else {
                rpcToTracking("killPhotonFx", identifier, forcedDeath);
            }
        }
    }
}

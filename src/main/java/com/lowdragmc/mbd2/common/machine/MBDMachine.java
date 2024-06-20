package com.lowdragmc.mbd2.common.machine;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.gui.factory.MachineUIFactory;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final RecipeLogic recipeLogic;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> capabilitiesProxy;
    @Nonnull
    @Persisted
    @DescSynced
    @RequireRerender
    private String machineState;
    @Getter
    private final List<ITrait> additionalTraits = new ArrayList<>();

    public MBDMachine(IMachineBlockEntity machineHolder, MBDMachineDefinition definition, Object... args) {
        this.machineHolder = machineHolder;
        this.definition = definition;
        // bind sync storage
        if (machineHolder.getRootStorage() instanceof MultiManagedStorage multiManagedStorage) {
            multiManagedStorage.attach(getSyncStorage());
        } else {
            throw new RuntimeException("Root storage of MBDMachine's holder must be MultiManagedStorage");
        }
        capabilitiesProxy = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);;
        machineState = definition.stateMachine().getRootState().name();
        // trait initialization
        recipeLogic = createRecipeLogic(args);
        // additional traits initialization
        loadAdditionalTraits();
    }

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

    public void setMachineState(String newState) {
        if (machineState.equals(newState)) return;
        if (definition.stateMachine().hasState(newState)) {
            machineState = newState;
            notifyBlockUpdate();
        }
    }

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

    public void initCapabilitiesProxy() {
        capabilitiesProxy.clear();
        for (var trait : additionalTraits) {
            if (trait instanceof IRecipeHandlerTrait<?> recipeHandlerTrait) {
                if (!capabilitiesProxy.contains(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability())) {
                    capabilitiesProxy.put(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability(), new ArrayList<>());
                }
                capabilitiesProxy.get(recipeHandlerTrait.getHandlerIO(), recipeHandlerTrait.getRecipeCapability()).add(recipeHandlerTrait);
            }
        }
    }

    @Nullable
    public ITrait getTraitByDefinition(TraitDefinition traitDefinition) {
        for (var trait : additionalTraits) {
            if (traitDefinition == trait.getDefinition()) {
                return trait;
            }
        }
        return null;
    }

    @Override
    public BlockEntity getHolder() {
        return machineHolder.getSelf();
    }

    @Override
    public long getOffset() {
        return machineHolder.getOffset();
    }

    @Override
    public Optional<Direction> getFrontFacing() {
        return getDefinition().blockProperties().rotationState().property.flatMap(property -> getBlockState().getOptionalValue(property));
    }

    @Override
    public boolean isFacingValid(Direction facing) {
        return getDefinition().blockProperties().rotationState().test(facing);
    }

    @Override
    public void setFrontFacing(Direction facing) {
        var blockState = getBlockState();
        var property = getDefinition().blockProperties().rotationState().property;
        if (property.isPresent() && blockState.hasProperty(property.get()) && isFacingValid(facing)) {
            getLevel().setBlockAndUpdate(getPos(), blockState.setValue(property.get(), facing));
        }
    }

    @NotNull
    @Override
    public MBDRecipeType getRecipeType() {
        return definition.machineSettings().getRecipeType();
    }

    @Override
    public void notifyStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        switch (newStatus) {
            case WORKING -> setMachineState("working");
            case IDLE -> setMachineState(definition.stateMachine().getRootState().name());
            case WAITING -> setMachineState("waiting");
            case SUSPEND -> setMachineState("suspend");
        }
    }

    @Override
    public void scheduleRenderUpdate() {
        IMachine.super.scheduleRenderUpdate();
    }

    public MachineState getMachineState() {
        return definition.getState(machineState);
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        List<T> results = new ArrayList<>();
        for (var trait : additionalTraits) {
            if (trait instanceof ICapabilityProviderTrait<?> capabilityProviderTrait && capabilityProviderTrait.getCapability() == cap) {
                results.add((T) capabilityProviderTrait.getCapContent(side));
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
                        return cap.orEmpty(cap, LazyOptional.of(() -> (T) capabilityProviderTrait.mergeContents(results)));
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
     * Server tick.
     */
    public void serverTick() {
        if (recipeLogic != null) {
            recipeLogic.serverTick();
        }
    }

    /**
     * Client tick.
     */
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    public void animateTick(RandomSource random) {
        getDefinition().animateTick(this, random);
    }

    /**
     * Called when neighbors changed.
     */
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
    }

    /**
     * Called when machine placed by (if exist) an entity with item.
     * it won't be called when machine added by {@link Level#setBlock(BlockPos, BlockState, int, int)}
     */
    public void onMachinePlaced(LivingEntity player, ItemStack pStack) {
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
    }

    /**
     * On machine broken and drops items.
     */
    public void onDrops(Entity entity, List<ItemStack> drops) {

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
}

package com.lowdragmc.mbd2.common.machine;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.definition.MachineDefinition;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Getter
public class MBDMachine implements IMachine, IEnhancedManaged {
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
    private final MachineDefinition definition;
    private final IMachineBlockEntity machineHolder;

    @Persisted
    @DescSynced
    private final RecipeLogic recipeLogic;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> capabilitiesProxy;

    public MBDMachine(IMachineBlockEntity machineHolder, MachineDefinition definition, Object... args) {
        this.machineHolder = machineHolder;
        this.definition = definition;
        // bind sync storage
        if (machineHolder.getRootStorage() instanceof MultiManagedStorage multiManagedStorage) {
            multiManagedStorage.attach(getSyncStorage());
        } else {
            throw new RuntimeException("Root storage of MBDMachine's holder must be MultiManagedStorage");
        }
        // trait initialization
        recipeLogic = createRecipeLogic(args);
        capabilitiesProxy = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);;
    }

    protected RecipeLogic createRecipeLogic(Object... args) {
        return new RecipeLogic(this);
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
        return getBlockState().getOptionalValue(getDefinition().getRotationState().property);
    }

    @Override
    public boolean isFacingValid(Direction facing) {
        return getDefinition().getRotationState().test(facing);
    }

    @Override
    public void setFrontFacing(Direction facing) {
        var blockState = getBlockState();
        if (blockState.hasProperty(getDefinition().getRotationState().property) && isFacingValid(facing)) {
            getLevel().setBlockAndUpdate(getPos(), blockState.setValue(getDefinition().getRotationState().property, facing));
        }
    }

    @NotNull
    @Override
    public MBDRecipeType getRecipeType() {
        return definition.getRecipeType();
    }

    @Override
    public void scheduleRenderUpdate() {
        IMachine.super.scheduleRenderUpdate();
    }

    public void animateTick(RandomSource random) {
        getDefinition().animateTick(this, random);
    }

    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
    }
}

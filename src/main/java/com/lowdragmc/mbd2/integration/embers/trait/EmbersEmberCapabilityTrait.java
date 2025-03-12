package com.lowdragmc.mbd2.integration.embers.trait;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import com.lowdragmc.mbd2.integration.embers.EmbersEmberRecipeCapability;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Getter
public class EmbersEmberCapabilityTrait extends SimpleCapabilityTrait implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(EmbersEmberCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableEmberCapability storage;
    private final EmberRecipeHandler recipeHandler = new EmberRecipeHandler();
    private final EmberCap emberCap = new EmberCap();

    public EmbersEmberCapabilityTrait(MBDMachine machine, EmbersEmberCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages();
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public EmbersEmberCapabilityTraitDefinition getDefinition() {
        return (EmbersEmberCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.setEmber(storage.getEmberCapacity() / 2);
    }

    protected CopiableEmberCapability createStorages() {
        return new CopiableEmberCapability(getDefinition().getCapacity());
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return List.of(emberCap);
    }

    @Override
    public @Nullable AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    @Override
    public void handleAutoIO(BlockPos port, Direction side, IO io) {
        if (io == IO.IN) {
            Optional.ofNullable(getMachine().getLevel().getBlockEntity(port.relative(side)))
                    .flatMap(be -> be.getCapability(EmbersCapabilities.EMBER_CAPABILITY, side.getOpposite()).resolve())
                    .ifPresent(source -> source.removeAmount(
                            storage.addAmount(source.removeAmount(getDefinition().getCapacity(), false),
                                    true),
                            true));
        } else {
            Optional.ofNullable(getMachine().getLevel().getBlockEntity(port.relative(side)))
                    .flatMap(be -> be.getCapability(EmbersCapabilities.EMBER_CAPABILITY, side.getOpposite()).resolve())
                    .ifPresent(target -> target.addAmount(
                            storage.removeAmount(target.addAmount(getDefinition().getCapacity(), false),
                                    true),
                            true));
        }
    }

    public class EmberRecipeHandler extends RecipeHandlerTrait<Double> {
        protected EmberRecipeHandler() {
            super(EmbersEmberCapabilityTrait.this, EmbersEmberRecipeCapability.CAP);
        }

        @Override
        public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var required = left.stream().mapToDouble(Double::doubleValue).reduce(0, Double::sum);
            var capability = simulate ? storage.copy() : storage;
            if (io == IO.IN) {
                var extracted = capability.removeAmount(required, !simulate);
                required -= extracted;
            } else {
                var received = capability.addAmount(required, !simulate);
                required -= received;
            }
            return required > 0 ? List.of(required) : null;
        }
    }

    public class EmberCap implements ICapabilityProviderTrait<IEmberCapability> {
        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return EmbersEmberCapabilityTrait.this.getCapabilityIO(side);
        }

        @Override
        public Capability<IEmberCapability> getCapability() {
            return EmbersCapabilities.EMBER_CAPABILITY;
        }

        @Override
        public IEmberCapability getCapContent(IO capbilityIO) {
            return new EmberCapabilityWrapper(storage, capbilityIO);
        }

        @Override
        public IEmberCapability mergeContents(List<IEmberCapability> contents) {
            return contents.get(0);
        }
    }
}

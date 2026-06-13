//package com.lowdragmc.mbd2.integration.botania.trait;
//
//import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
//import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
//import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import com.lowdragmc.mbd2.common.trait.AutoIO;
//import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
//import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
//import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
//import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.server.level.ServerLevel;
//import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import vazkii.botania.api.mana.ManaPool;
//import vazkii.botania.api.mana.ManaReceiver;
//
//import java.util.EnumMap;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class BotaniaManaCapabilityTrait extends SimpleCapabilityTrait<ManaReceiver, Direction> implements IAutoIOTrait {
//    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BotaniaManaCapabilityTrait.class);
//
//    @Override
//    public ManagedFieldHolder getFieldHolder() {
//        return MANAGED_FIELD_HOLDER;
//    }
//
//    @Persisted
//    @DescSynced
//    public final CopiableManaPool storage;
//    private final ManaRecipeHandler recipeHandler = new ManaRecipeHandler();
//    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<ManaReceiver, Direction>>> nearbyCache = new HashMap<>();
//
//    public BotaniaManaCapabilityTrait(MBDMachine machine, BotaniaManaCapabilityTraitDefinition definition) {
//        super(machine, definition);
//        storage = createStorages(machine);
//        storage.setOnContentsChanged(this::notifyListeners);
//    }
//
//    @Override
//    public BotaniaManaCapabilityTraitDefinition getDefinition() {
//        return (BotaniaManaCapabilityTraitDefinition) super.getDefinition();
//    }
//
//    @Override
//    public ManaReceiver getCapContent(IO capabilityIO) {
//        return new ManaPoolWrapper(storage, capabilityIO);
//    }
//
//    @Override
//    public void onLoadingTraitInPreview() {
//        storage.receiveMana(getDefinition().getCapacity() / 2);
//    }
//
//    protected CopiableManaPool createStorages(MBDMachine machine) {
//        return new CopiableManaPool(machine, getDefinition().getCapacity(), getDefinition().isCanAttachSpark());
//    }
//
//    @Override
//    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
//        return List.of(recipeHandler);
//    }
//
//    @Override
//    public @Nullable AutoIO getAutoIO() {
//        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
//    }
//
//    public BlockCapabilityCache<ManaReceiver, Direction> getNearbyCache(ServerLevel serverLevel, BlockPos pos, @NotNull Direction side) {
//        return nearbyCache.computeIfAbsent(pos, blockPos -> new EnumMap<>(Direction.class))
//                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(BotaniaManaCapabilityTraitDefinition.manaReceiverCapability(),
//                        serverLevel,
//                        pos.relative(direction), direction.getOpposite()
//                ));
//    }
//
//    @Override
//    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
//        if (getMachine().getLevel() instanceof ServerLevel serverLevel) {
//            if (io.support(IO.IN)) {
//                var source = getNearbyCache(serverLevel, port, side).getCapability();
//                if (source != null) {
//                    var available = source.getCurrentMana();
//                    var cost = Math.min(available, storage.getMaxMana() - storage.getCurrentMana());
//                    storage.receiveMana(cost);
//                    source.receiveMana(-cost);
//                }
//            }
//            if (io.support(IO.OUT)) {
//                var target = getNearbyCache(serverLevel, port, side).getCapability();
//                if (target != null) {
//                    var available = storage.getCurrentMana();
//                    var cost = target instanceof ManaPool pool ? Math.min(available, pool.getMaxMana() - pool.getCurrentMana()) : available;
//                    storage.receiveMana(-cost);
//                    target.receiveMana(cost);
//                }
//            }
//        }
//    }
//
//    public class ManaRecipeHandler extends RecipeHandlerTrait<Integer> {
//        protected ManaRecipeHandler() {
//            super(BotaniaManaCapabilityTrait.this, BotaniaManaRecipeCapability.CAP);
//        }
//
//        @Override
//        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
//            if (!compatibleWith(io)) return left;
//            int required = left.stream().reduce(0, Integer::sum);
//            var capability = simulate ? storage.copy() : storage;
//            if (io == IO.IN) {
//                var cost = Math.min(required, capability.getCurrentMana());
//                capability.receiveMana(-cost);
//                required -= cost;
//            } else {
//                if (capability.isFull() || !capability.canReceiveManaFromBursts()) return left;
//                if (required > capability.getMaxMana() - capability.getCurrentMana()) {
//                    var received = capability.getMaxMana() - capability.getCurrentMana();
//                    capability.receiveMana(received);
//                    required -= received;
//                } else {
//                    capability.receiveMana(required);
//                    return null;
//                }
//            }
//            return required > 0 ? List.of(required) : null;
//        }
//    }
//}

//package com.lowdragmc.mbd2.integration.botania.trait;
//
//import com.google.common.base.Predicates;
//import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
//import com.lowdragmc.lowdraglib2.syncdata.ITagSerializable;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import lombok.Getter;
//import lombok.Setter;
//import net.minecraft.core.BlockPos;
//import net.minecraft.nbt.IntTag;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.item.DyeColor;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.AABB;
//import vazkii.botania.api.mana.ManaPool;
//import vazkii.botania.api.mana.spark.ManaSpark;
//import vazkii.botania.api.mana.spark.SparkAttachable;
//import vazkii.botania.common.block.BotaniaBlocks;
//
//import java.util.List;
//import java.util.Optional;
//
//public class CopiableManaPool implements ManaPool, ITagSerializable<IntTag>, IContentChangeAware, SparkAttachable {
//    @Getter
//    @Setter
//    public Runnable onContentsChanged = () -> {};
//
//    private final MBDMachine machine;
//    @Getter
//    protected final int maxMana;
//
//    protected int mana;
//    protected boolean canAttachSpark;
//
//    public CopiableManaPool(MBDMachine machine, int capacity, boolean canAttachSpark) {
//        this(machine, capacity, canAttachSpark, 0);
//    }
//
//    public CopiableManaPool(MBDMachine machine, int capacity, boolean canAttachSpark, int mana) {
//        this.machine = machine;
//        this.maxMana = capacity;
//        this.mana = mana;
//        this.canAttachSpark = canAttachSpark;
//    }
//
//    public CopiableManaPool copy() {
//        return new CopiableManaPool(machine, maxMana,canAttachSpark, mana);
//    }
//
//    @Override
//    public Level getManaReceiverLevel() {
//        return machine.getLevel();
//    }
//
//    @Override
//    public BlockPos getManaReceiverPos() {
//        return machine.getPos();
//    }
//
//    @Override
//    public int getCurrentMana() {
//        return mana;
//    }
//
//
//    @Override
//    public boolean isFull() {
//        return mana >= maxMana;
//    }
//
//    @Override
//    public void receiveMana(int mana) {
//        var old = this.mana;
//        this.mana = Math.max(0, Math.min(this.mana + mana, maxMana));
//        if (old != this.mana) onContentsChanged.run();
//    }
//
//    @Override
//    public boolean canReceiveManaFromBursts() {
//        return !isFull();
//    }
//
//    @Override
//    public IntTag serializeNBT() {
//        return IntTag.valueOf(mana);
//    }
//
//    @Override
//    public void deserializeNBT(IntTag nbt) {
//        mana = nbt.getAsInt();
//    }
//
//    @Override
//    public boolean isOutputtingPower() {
//        return false;
//    }
//
//    @Override
//    public Optional<DyeColor> getColor() {
//        return Optional.empty();
//    }
//
//    @Override
//    public void setColor(Optional<DyeColor> color) {
//
//    }
//
//    @Override
//    public boolean canAttachSpark(ItemStack stack) {
//        return canAttachSpark;
//    }
//
//    @Override
//    public int getAvailableSpaceForMana() {
//        int space = Math.max(0, getMaxMana() - getCurrentMana());
//        if (space > 0) {
//            return space;
//        } else if (machine.getLevel().getBlockState(machine.getPos().below()).is(BotaniaBlocks.manaVoid)) {
//            return getMaxMana();
//        } else {
//            return 0;
//        }
//    }
//
//    @Override
//    public ManaSpark getAttachedSpark() {
//        List<Entity> sparks =  machine.getLevel().getEntitiesOfClass(Entity.class,
//                new AABB(machine.getPos().above(), machine.getPos().above().offset(1, 1, 1)),
//                Predicates.instanceOf(ManaSpark.class));
//        if (sparks.size() == 1) {
//            Entity e = sparks.get(0);
//            return (ManaSpark) e;
//        }
//
//        return null;
//    }
//
//    @Override
//    public boolean areIncomingTranfersDone() {
//        return false;
//    }
//}

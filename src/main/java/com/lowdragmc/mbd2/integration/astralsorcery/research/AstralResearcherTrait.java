package com.lowdragmc.mbd2.integration.astralsorcery.research;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.Trait;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Binds the machine to the player who placed it, since MBD2 machines have no owner, and keeps a snapshot of their
 * Astral Sorcery progress for {@link AstralResearchCondition}. A machine placed without a player stays unbound
 * until {@link #bindTo} is called, e.g. from a script.
 */
@Getter
public class AstralResearcherTrait extends Trait {
    @Persisted
    public final ResearchSnapshot snapshot = new ResearchSnapshot();

    public AstralResearcherTrait(MBDMachine machine, AstralResearcherTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public AstralResearcherTraitDefinition getDefinition() {
        return (AstralResearcherTraitDefinition) super.getDefinition();
    }

    public void bindTo(ServerPlayer player) {
        snapshot.bind(player);
        changed();
    }

    @Nullable
    public ServerPlayer onlineOwner() {
        var owner = snapshot.getOwner();
        if (owner == null || !(getMachine().getLevel() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    /** Re-reads the owner's progress if they are online. Game thread only. */
    public void refresh() {
        var player = onlineOwner();
        if (player != null && snapshot.capture(player)) {
            changed();
        }
    }

    @Override
    public void serverTick() {
        if (getMachine().getOffsetTimer() % Math.max(1, getDefinition().getRefreshInterval()) == 0) {
            refresh();
        }
    }

    private void changed() {
        notifyListeners();
        getMachine().markDirty();
    }
}

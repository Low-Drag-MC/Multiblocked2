package com.lowdragmc.mbd2.integration.astralsorcery.research;

import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.research.ResearchManager;
import hellfirepvp.astralsorcery.common.research.ResearchTier;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The owner's last seen Astral Sorcery progress. A snapshot because Astral Sorcery only exposes progress for an
 * online {@link ServerPlayer}; it lets a condition keep answering after the owner logs off.
 */
@Getter
public class ResearchSnapshot implements INBTSerializable<CompoundTag>, IContentVersion {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    @Nullable
    private ResearchTier tier;
    @Nullable
    private ResourceLocation attunedConstellation;
    private Set<ResourceLocation> knownConstellations = Set.of();
    // only the game thread writes it, so the non-atomic increment is safe
    private volatile long contentVersion;

    public boolean isBound() {
        return owner != null;
    }

    public void bind(ServerPlayer player) {
        bind(player.getUUID(), player.getGameProfile().getName(), ResearchManager.getProgress(player, LogicalSide.SERVER));
    }

    public void bind(UUID owner, String ownerName, PlayerProgress progress) {
        this.owner = owner;
        capture(progress, ownerName);
        contentVersion++;
    }

    /** @return whether anything changed */
    public boolean capture(ServerPlayer player) {
        return capture(ResearchManager.getProgress(player, LogicalSide.SERVER), player.getGameProfile().getName());
    }

    /** @return whether anything changed */
    public boolean capture(PlayerProgress progress, String ownerName) {
        var newTier = progress.getTierReached();
        var newAttuned = progress.getAttunedConstellation().map(AstralSorceryIntegration::constellationId).orElse(null);
        var newKnown = progress.getKnownConstellations().stream()
                .map(AstralSorceryIntegration::constellationId)
                .collect(Collectors.toUnmodifiableSet());
        var changed = newTier != tier || !Objects.equals(newAttuned, attunedConstellation)
                || !newKnown.equals(knownConstellations) || !this.ownerName.equals(ownerName);
        tier = newTier;
        attunedConstellation = newAttuned;
        knownConstellations = newKnown;
        this.ownerName = ownerName;
        if (changed) contentVersion++;
        return changed;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        if (owner != null) {
            tag.putUUID("owner", owner);
            tag.putString("ownerName", ownerName);
        }
        if (tier != null) {
            tag.putString("tier", tier.name());
        }
        if (attunedConstellation != null) {
            tag.putString("attuned", attunedConstellation.toString());
        }
        var known = new ListTag();
        knownConstellations.stream().sorted().forEach(id -> known.add(StringTag.valueOf(id.toString())));
        tag.put("known", known);
        return tag;
    }

    public CompoundTag uiSnapshot() {
        var tag = new CompoundTag();
        if (owner != null) {
            tag.putString("ownerName", ownerName);
        }
        if (tier != null) {
            tag.putString("tier", tier.name());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName = tag.getString("ownerName");
        tier = null;
        if (tag.contains("tier")) {
            try {
                tier = ResearchTier.valueOf(tag.getString("tier"));
            } catch (IllegalArgumentException ignored) {
                // renamed by an Astral Sorcery update: unknown until the owner is seen again
            }
        }
        attunedConstellation = tag.contains("attuned") ? ResourceLocation.tryParse(tag.getString("attuned")) : null;
        knownConstellations = tag.getList("known", Tag.TAG_STRING).stream()
                .map(element -> ResourceLocation.tryParse(element.getAsString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        contentVersion++;
    }

    /** Counts changes to what {@link #serializeNBT} writes, so LDLib2's sync compares this instead of the tag. */
    @Override
    public long getContentVersion() {
        return contentVersion;
    }
}

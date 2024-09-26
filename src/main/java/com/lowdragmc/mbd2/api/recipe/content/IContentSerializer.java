package com.lowdragmc.mbd2.api.recipe.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.utils.NBTToJsonConverter;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.crafting.CraftingHelper;

public interface IContentSerializer<T> {

    default void toNetwork(FriendlyByteBuf buf, T content) {
        buf.writeUtf(LDLib.GSON.toJson(toJson(content)));
    }

    default T fromNetwork(FriendlyByteBuf buf) {
        return fromJson(LDLib.GSON.fromJson(buf.readUtf(), JsonElement.class));
    }

    default Tag toNBT(T content) {
        return CraftingHelper.getNBT(toJson(content));
    }

    default T fromNBT(Tag nbt) {
        return fromJson(NBTToJsonConverter.getObject(nbt));
    }

    T fromJson(JsonElement json);

    JsonElement toJson(T content);

    T of(Object o);

    /**
     * deep copy and modify the size attribute for those Content that have the size attribute.
     */
    T copyWithModifier(T content, ContentModifier modifier);

    /**
     * deep copy of this content. recipe need it for searching and such things
     */
    T copyInner(T content);

    default void toNetworkContent(FriendlyByteBuf buf, Content content) {
        T inner = (T) content.getContent();
        toNetwork(buf, inner);
        buf.writeBoolean(content.perTick);
        buf.writeFloat(content.chance);
        buf.writeFloat(content.tierChanceBoost);
        buf.writeBoolean(!content.slotName.isEmpty());
        if (!content.slotName.isEmpty()) {
            buf.writeUtf(content.slotName);
        }
        buf.writeBoolean(content.uiName != null);
        if (!content.uiName.isEmpty()) {
            buf.writeUtf(content.uiName);
        }
    }

    default Content fromNetworkContent(FriendlyByteBuf buf) {
        T inner = fromNetwork(buf);
        var perTick = buf.readBoolean();
        float chance = buf.readFloat();
        float tierChanceBoost = buf.readFloat();
        String slotName = null;
        if (buf.readBoolean()) {
            slotName = buf.readUtf();
        }
        String uiName = null;
        if (buf.readBoolean()) {
            uiName = buf.readUtf();
        }
        return new Content(inner, perTick, chance, tierChanceBoost, slotName, uiName);
    }

    @SuppressWarnings("unchecked")
    default JsonElement toJsonContent(Content content) {
        JsonObject json = new JsonObject();
        json.add("content", toJson((T) content.getContent()));
        json.addProperty("perTick", content.perTick);
        json.addProperty("chance", content.chance);
        json.addProperty("tierChanceBoost", content.tierChanceBoost);
        if (!content.slotName.isEmpty())
            json.addProperty("slotName", content.slotName);
        if (!content.uiName.isEmpty())
            json.addProperty("uiName", content.uiName);
        return json;
    }

    default Content fromJsonContent(JsonElement json) {
        JsonObject jsonObject = json.getAsJsonObject();
        T inner = fromJson(jsonObject.get("content"));
        var perTick = jsonObject.has("perTick") && jsonObject.get("perTick").getAsBoolean();
        float chance = jsonObject.has("chance") ? jsonObject.get("chance").getAsFloat() : 1;
        float tierChanceBoost = jsonObject.has("tierChanceBoost") ? jsonObject.get("tierChanceBoost").getAsFloat() : 0;
        String slotName = jsonObject.has("slotName") ? jsonObject.get("slotName").getAsString() : null;
        String uiName = jsonObject.has("uiName") ? jsonObject.get("uiName").getAsString() : null;
        return new Content(inner, perTick, chance, tierChanceBoost, slotName, uiName);
    }

    default Content fromNBT(CompoundTag tag) {
        T content = fromNBT(tag.get("content"));
        boolean perTick = tag.getBoolean("per_tick");
        float chance = tag.getFloat("chance");
        float tierChanceBoost = tag.getFloat("tier_chance_boost");
        String slotName = tag.getString("slot_name");
        String uiName = tag.getString("ui_name");
        return new Content(content, perTick, chance, tierChanceBoost, slotName, uiName);
    }

    default CompoundTag toNBT(Content content) {
        CompoundTag tag = new CompoundTag();
        tag.put("content", toNBT(of(content.content)));
        tag.putBoolean("per_tick", content.perTick);
        tag.putFloat("chance", content.chance);
        tag.putFloat("tier_chance_boost", content.tierChanceBoost);
        tag.putString("slot_name", content.slotName);
        tag.putString("ui_name", content.uiName);
        return tag;
    }
}

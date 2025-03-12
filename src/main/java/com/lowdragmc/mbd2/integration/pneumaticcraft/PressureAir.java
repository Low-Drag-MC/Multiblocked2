package com.lowdragmc.mbd2.integration.pneumaticcraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;

public record PressureAir(boolean isAir, float value) {

    public static class SerializerPressureAir implements IContentSerializer<PressureAir> {

        public static final IContentSerializer<PressureAir> INSTANCE = new SerializerPressureAir();

        @Override
        public PressureAir fromJson(JsonElement json) {
            var isAir = GsonHelper.getAsBoolean(json.getAsJsonObject(), "isAir");
            var value = GsonHelper.getAsFloat(json.getAsJsonObject(), "value");
            return new PressureAir(isAir, value);
        }

        @Override
        public JsonElement toJson(PressureAir content) {
            var json = new JsonObject();
            json.addProperty("isAir", content.isAir);
            json.addProperty("value", content.value);
            return json;
        }

        @Override
        public Tag toNBT(PressureAir content) {
            var tag = new CompoundTag();
            tag.putBoolean("isAir", content.isAir);
            tag.putFloat("value", content.value);
            return tag;
        }

        @Override
        public PressureAir fromNBT(Tag nbt) {
            var tag = (CompoundTag) nbt;
            return new PressureAir(tag.getBoolean("isAir"), tag.getFloat("value"));
        }

        @Override
        public void toNetwork(RegistryFriendlyByteBuf buf, PressureAir content) {
            buf.writeBoolean(content.isAir);
            buf.writeFloat(content.value);
        }

        @Override
        public PressureAir fromNetwork(RegistryFriendlyByteBuf buf) {
            return new PressureAir(buf.readBoolean(), buf.readFloat());
        }

        @Override
        public PressureAir of(Object o) {
            if (o instanceof PressureAir pressureAir) {
                return pressureAir;
            } else if (o instanceof Number number) {
                return new PressureAir(false, number.floatValue());
            } else if (o instanceof CharSequence) {
                var str = o.toString();
                var splits = str.split(":");
                if (splits.length == 2) {
                    try {
                        var isAir = Boolean.parseBoolean(splits[0]);
                        var value = Float.parseFloat(splits[1]);
                        return new PressureAir(isAir, value);
                    } catch (Exception ignored) {}
                }
            }
            return new PressureAir(false, 0);
        }

        @Override
        public PressureAir copyWithModifier(PressureAir content, ContentModifier modifier) {
            return new PressureAir(content.isAir, modifier.apply(content.value).floatValue());
        }

        @Override
        public PressureAir copyInner(PressureAir content) {
            return new PressureAir(content.isAir, content.value);
        }
    }
}

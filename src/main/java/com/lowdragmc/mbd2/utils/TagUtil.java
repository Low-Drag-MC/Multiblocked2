package com.lowdragmc.mbd2.utils;

import com.lowdragmc.mbd2.MBD2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * @author KilaBash
 * @date 2023/2/21
 * @implNote TagUtil
 */
public class TagUtil {

    public static <T> TagKey<T> optionalTag(Registry<T> registry, ResourceLocation id) {
        return TagKey.create(registry.key(), id);
    }

    public static <T> TagKey<T> optionalTag(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id) {
        return TagKey.create(registryKey, id);
    }

    public static <T> TagKey<T> createTag(Registry<T> registry, String path, boolean vanilla) {
        if (vanilla) return optionalTag(registry, new ResourceLocation("minecraft", path));
        return optionalTag(registry, new ResourceLocation("forge", path));
    }

    public static <T> TagKey<T> createTag(ResourceKey<? extends Registry<T>> registryKey, String path, boolean vanilla) {
        if (vanilla) return optionalTag(registryKey, new ResourceLocation("minecraft", path));
        return optionalTag(registryKey, new ResourceLocation("forge", path));
    }

    public static <T> TagKey<T> createModTag(Registry<T> registry, String path) {
        return optionalTag(registry, MBD2.id(path));
    }

    public static <T> TagKey<T> createModTag(ResourceKey<? extends Registry<T>> registryKey, String path) {
        return TagKey.create(registryKey, MBD2.id(path));
    }

    public static TagKey<Block> createBlockTag(String path) {
        return createTag(BuiltInRegistries.BLOCK, path, false);
    }

    public static TagKey<Block> createBlockTag(String path, boolean vanilla) {
        return createTag(BuiltInRegistries.BLOCK, path, vanilla);
    }

    public static TagKey<Block> createModBlockTag(String path) {
        return createModTag(BuiltInRegistries.BLOCK, path);
    }

    public static TagKey<Item> createItemTag(String path) {
        return createTag(BuiltInRegistries.ITEM, path, false);
    }

    public static TagKey<Item> createItemTag(String path, boolean vanilla) {
        return createTag(BuiltInRegistries.ITEM, path, vanilla);
    }

    public static TagKey<Item> createModItemTag(String path) {
        return createModTag(BuiltInRegistries.ITEM, path);
    }

    public static TagKey<Fluid> createFluidTag(String path) {
        return createTag(BuiltInRegistries.FLUID, path, false);
    }

}

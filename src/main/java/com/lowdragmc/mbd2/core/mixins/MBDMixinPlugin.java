package com.lowdragmc.mbd2.core.mixins;

import com.lowdragmc.lowdraglib.core.mixins.MixinPluginShared;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MBDMixinPlugin implements IMixinConfigPlugin, MixinPluginShared {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.create")) {
            return MixinPluginShared.isClassFound("com.simibubi.create.compat.Mods");
        }
//        if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.kjs") || mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.rhino")) {
//            return MixinPluginShared.isClassFound("dev.latvian.mods.kubejs.KubeJSPlugin");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.create")) {
//            return MixinPluginShared.isClassFound("com.simibubi.create.compat.Mods");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.rei")) {
//            return MixinPluginShared.isClassFound("me.shedaniel.rei.api.common.plugins.REIPlugin");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.fabric.core.mixins.kjs")) {
//            return MixinPluginShared.isClassFound("dev.latvian.mods.kubejs.fabric.KubeJSFabric");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.forge.core.mixins.kjs")) {
//            return MixinPluginShared.isClassFound("dev.latvian.mods.kubejs.forge.KubeJSForge");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.top")) {
//            return MixinPluginShared.isClassFound("mcjty.theoneprobe.api.ITheOneProbe");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.jei")) {
//            return MixinPluginShared.isClassFound("mezz.jei.api.IModPlugin");
//        } else if (mixinClassName.contains("com.lowdragmc.mbd2.core.mixins.emi")) {
//            return MixinPluginShared.isClassFound("dev.emi.emi.api.EmiPlugin");
//        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}

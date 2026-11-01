package com.jegvr.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Server mixins ({@code mixin.server}) need only JEG, since dedicated servers usually lack Vivecraft. The rest
 * (client) need both JEG and Vivecraft.
 */
public class JegVrMixinPlugin implements IMixinConfigPlugin {
    private static final String SERVER_PACKAGE = "com.jegvr.mixin.server.";

    private boolean hasJeg;
    private boolean hasVivecraft;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        hasJeg = mods != null && mods.getModFileById("jeg") != null;
        hasVivecraft = mods != null && mods.getModFileById("vivecraft") != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return hasJeg && (hasVivecraft || mixinClassName.startsWith(SERVER_PACKAGE));
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

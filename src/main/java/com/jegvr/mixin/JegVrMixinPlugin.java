package com.jegvr.mixin;

import com.jegvr.compat.Versions;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Decides which mixins apply from the installed versions ({@link Versions}):
 * <ul>
 *   <li>server mixins ({@code mixin.server}) need only JEG, since dedicated servers usually lack Vivecraft;</li>
 *   <li>the rest (client) need both JEG and Vivecraft;</li>
 *   <li>those in {@code mixin.vc12} and {@code mixin.vc13} are for one line of Vivecraft: their targets do not exist
 *   in the other.</li>
 * </ul>
 */
public class JegVrMixinPlugin implements IMixinConfigPlugin {
    private static final String SERVER_PACKAGE = "com.jegvr.mixin.server.";

    private boolean hasJeg;
    private boolean hasVivecraft;

    @Override
    public void onLoad(String mixinPackage) {
        hasJeg = Versions.hasJeg();
        hasVivecraft = Versions.hasVivecraft();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!hasJeg) {
            return false;
        }
        if (mixinClassName.startsWith(SERVER_PACKAGE)) {
            return true;
        }
        return hasVivecraft && Versions.mixinFitsVivecraft(mixinClassName);
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

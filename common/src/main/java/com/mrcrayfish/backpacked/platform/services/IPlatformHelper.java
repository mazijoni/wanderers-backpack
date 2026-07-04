package com.mrcrayfish.backpacked.platform.services;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Author: MrCrayfish
 */
public interface IPlatformHelper
{
    boolean isModLoaded(String modId);

    boolean isBuiltinOrModResourcePack(PackLocationInfo info);

    Predicate<ItemStack> getValidProjectiles(ItemStack weapon);

    boolean isRepairable(ItemStack stack);

    CreativeModeTab.Output createCreativeTabOutput(Consumer<ItemStack> consumer);

    /**
     * @return the fluid a filled bucket item contains, or empty if the stack isn't a bucket or is
     * the empty bucket. {@code BucketItem}'s {@code content} field is only public under NeoForge's
     * access transformer, so this can't be read directly from common code.
     */
    Optional<ResourceKey<Fluid>> getFluidFromBucket(ItemStack stack);

    /**
     * @return the bucket item associated with the given fluid (vanilla or modded), if one is
     * registered.
     */
    Optional<Item> getBucketForFluid(ResourceKey<Fluid> fluid);
}

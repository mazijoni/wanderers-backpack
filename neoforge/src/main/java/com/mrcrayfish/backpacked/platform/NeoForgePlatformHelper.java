package com.mrcrayfish.backpacked.platform;

import com.mrcrayfish.backpacked.platform.services.IPlatformHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Author: MrCrayfish
 */
public class NeoForgePlatformHelper implements IPlatformHelper
{
    @Override
    public boolean isModLoaded(String modId)
    {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isBuiltinOrModResourcePack(PackLocationInfo info)
    {
        if(info.source() == PackSource.BUILT_IN) return true;
        if(info.id().equals("mod_resources")) return true;
        if(info.id().equals("mod_data")) return true;
        if(info.knownPackInfo().stream().anyMatch(pack -> pack.namespace().equals("minecraft"))) return true;
        return false;
    }

    @Override
    public Predicate<ItemStack> getValidProjectiles(ItemStack weapon)
    {
        if(weapon.getItem() instanceof ProjectileWeaponItem item)
        {
            return item.getAllSupportedProjectiles(weapon);
        }
        return stack -> false;
    }

    @Override
    public boolean isRepairable(ItemStack stack)
    {
        return stack.isRepairable();
    }

    @Override
    public CreativeModeTab.Output createCreativeTabOutput(Consumer<ItemStack> consumer)
    {
        return (stack, visibility) -> consumer.accept(stack);
    }

    @Override
    public Optional<ResourceKey<Fluid>> getFluidFromBucket(ItemStack stack)
    {
        if(stack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY)
        {
            return BuiltInRegistries.FLUID.getResourceKey(bucket.content);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Item> getBucketForFluid(ResourceKey<Fluid> fluid)
    {
        Fluid value = BuiltInRegistries.FLUID.get(fluid);
        for(Item item : BuiltInRegistries.ITEM)
        {
            if(item instanceof BucketItem bucket && bucket.content == value)
            {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }
}

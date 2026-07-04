package com.mrcrayfish.backpacked.core;

import com.mrcrayfish.backpacked.Constants;
import com.mrcrayfish.backpacked.worldgen.FallenTreeFeature;
import com.mrcrayfish.backpacked.worldgen.MushroomShelfFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Author: MrCrayfish
 */
public class ModFeatures
{
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Constants.MOD_ID);

    public static final DeferredHolder<Feature<?>, MushroomShelfFeature> MUSHROOM_SHELF = FEATURES.register(
        "mushroom_shelf", () -> new MushroomShelfFeature(NoneFeatureConfiguration.CODEC)
    );

    public static final DeferredHolder<Feature<?>, FallenTreeFeature> FALLEN_TREE = FEATURES.register(
        "fallen_tree", () -> new FallenTreeFeature(FallenTreeFeature.Config.CODEC)
    );

    public static void register(IEventBus bus)
    {
        FEATURES.register(bus);
    }
}

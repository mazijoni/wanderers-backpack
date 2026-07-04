package com.mrcrayfish.backpacked.common.augment.impl;

import com.mojang.serialization.MapCodec;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.util.Utils;
import net.minecraft.network.codec.StreamCodec;

/**
 * Adds a button to the backpack screen that, at night, places a Hay Bed in front of the player and
 * puts them straight to sleep (see {@code AugmentHandler#useBedAugment}). Purely a presence check -
 * no persisted state of its own, like {@link EmptyAugment}.
 */
public record BedAugment() implements Augment<BedAugment>
{
    public static final BedAugment INSTANCE = new BedAugment();
    public static final AugmentType<BedAugment> TYPE = new AugmentType<>(
        Utils.rl("bed"),
        MapCodec.unit(INSTANCE),
        StreamCodec.unit(INSTANCE),
        () -> INSTANCE
    );

    @Override
    public AugmentType<BedAugment> type()
    {
        return TYPE;
    }
}

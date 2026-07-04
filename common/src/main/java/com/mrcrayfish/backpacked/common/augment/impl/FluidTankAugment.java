package com.mrcrayfish.backpacked.common.augment.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.util.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

/**
 * Stores a single type of fluid (filled/drained with vanilla-style buckets, see
 * BackpackContainerMenu's tank slot) up to {@link #CAPACITY} millibuckets.
 */
public record FluidTankAugment(Optional<ResourceKey<Fluid>> fluid, int amount) implements Augment<FluidTankAugment>
{
    public static final int CAPACITY = 8000;
    public static final int BUCKET_AMOUNT = 1000;

    public static final AugmentType<FluidTankAugment> TYPE = new AugmentType<>(
        Utils.rl("fluid_tank"),
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.FLUID).optionalFieldOf("fluid").forGetter(FluidTankAugment::fluid),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(FluidTankAugment::amount)
        ).apply(instance, FluidTankAugment::new)),
        StreamCodec.composite(
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.FLUID)), FluidTankAugment::fluid,
            ByteBufCodecs.VAR_INT, FluidTankAugment::amount,
            FluidTankAugment::new
        ),
        () -> new FluidTankAugment(Optional.empty(), 0)
    );

    @Override
    public AugmentType<FluidTankAugment> type()
    {
        return TYPE;
    }

    /**
     * @return true if this tank is empty or already holds the given fluid, and has enough spare
     * capacity for another {@link #BUCKET_AMOUNT}.
     */
    public boolean canAccept(ResourceKey<Fluid> fluid, int amount)
    {
        return (this.fluid.isEmpty() || this.fluid.get().equals(fluid)) && this.amount + amount <= CAPACITY;
    }

    public FluidTankAugment fill(ResourceKey<Fluid> fluid, int amount)
    {
        return new FluidTankAugment(Optional.of(fluid), this.amount + amount);
    }

    public FluidTankAugment drain(int amount)
    {
        int remaining = this.amount - amount;
        return remaining <= 0 ? new FluidTankAugment(Optional.empty(), 0) : new FluidTankAugment(this.fluid, remaining);
    }
}

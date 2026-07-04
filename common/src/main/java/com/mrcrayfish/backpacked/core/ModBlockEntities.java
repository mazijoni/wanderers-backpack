package com.mrcrayfish.backpacked.core;

import com.mrcrayfish.backpacked.blockentity.HayBedBlockEntity;
import com.mrcrayfish.backpacked.blockentity.ShelfBlockEntity;
import com.mrcrayfish.backpacked.util.Utils;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Author: MrCrayfish
 */
@RegistryContainer
public class ModBlockEntities
{
    public static final RegistryEntry<BlockEntityType<ShelfBlockEntity>> BACKPACK_SHELF = RegistryEntry.blockEntity(
        Utils.rl("shelf"),
        ShelfBlockEntity::new,
        () -> new Block[] {
            ModBlocks.OAK_BACKPACK_SHELF.get(),
            ModBlocks.SPRUCE_BACKPACK_SHELF.get(),
            ModBlocks.BIRCH_BACKPACK_SHELF.get(),
            ModBlocks.JUNGLE_BACKPACK_SHELF.get(),
            ModBlocks.DARK_OAK_BACKPACK_SHELF.get(),
            ModBlocks.ACACIA_BACKPACK_SHELF.get(),
            ModBlocks.CRIMSON_BACKPACK_SHELF.get(),
            ModBlocks.WARPED_BACKPACK_SHELF.get(),
            ModBlocks.CHERRY_BACKPACK_SHELF.get(),
            ModBlocks.MANGROVE_BACKPACK_SHELF.get(),
            ModBlocks.MUSHROOM_BACKPACK_SHELF.get(),
            ModBlocks.WARPED_MUSHROOM_BACKPACK_SHELF.get()
        });

    public static final RegistryEntry<BlockEntityType<HayBedBlockEntity>> HAY_BED = RegistryEntry.blockEntity(
        Utils.rl("hay_bed"),
        HayBedBlockEntity::new,
        () -> new Block[] { ModBlocks.HAY_BED.get() }
    );
}

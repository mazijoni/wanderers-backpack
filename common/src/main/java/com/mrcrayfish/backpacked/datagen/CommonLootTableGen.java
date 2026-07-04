package com.mrcrayfish.backpacked.datagen;

import com.mrcrayfish.backpacked.core.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class CommonLootTableGen
{
    public static void generate(Consumer<Block> dropSelf)
    {
        dropSelf.accept(ModBlocks.OAK_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.SPRUCE_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.BIRCH_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.JUNGLE_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.DARK_OAK_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.ACACIA_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.CRIMSON_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.WARPED_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.CHERRY_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.MANGROVE_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.MUSHROOM_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.WARPED_MUSHROOM_BACKPACK_SHELF.get());
        dropSelf.accept(ModBlocks.MUSHROOM_SHELF_SPROUT.get());
        dropSelf.accept(ModBlocks.WARPED_MUSHROOM_SHELF_SPROUT.get());
        dropSelf.accept(ModBlocks.HAY_BED.get());
    }
}

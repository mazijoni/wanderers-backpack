package com.mrcrayfish.backpacked.core;

import com.mrcrayfish.backpacked.block.HayBedBlock;
import com.mrcrayfish.backpacked.block.HollowLogBlock;
import com.mrcrayfish.backpacked.block.MushroomShelfSproutBlock;
import com.mrcrayfish.backpacked.block.ShelfBlock;
import com.mrcrayfish.backpacked.util.Utils;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Author: MrCrayfish
 */
@RegistryContainer
public final class ModBlocks
{
    public static final RegistryEntry<Block> OAK_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("oak_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final RegistryEntry<Block> SPRUCE_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("spruce_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS)));
    public static final RegistryEntry<Block> BIRCH_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("birch_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.BIRCH_PLANKS)));
    public static final RegistryEntry<Block> JUNGLE_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("jungle_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.JUNGLE_PLANKS)));
    public static final RegistryEntry<Block> DARK_OAK_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("dark_oak_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS)));
    public static final RegistryEntry<Block> ACACIA_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("acacia_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.ACACIA_PLANKS)));
    public static final RegistryEntry<Block> CRIMSON_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("crimson_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.CRIMSON_PLANKS)));
    public static final RegistryEntry<Block> WARPED_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("warped_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.WARPED_PLANKS)));
    public static final RegistryEntry<Block> CHERRY_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("cherry_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.CHERRY_PLANKS)));
    public static final RegistryEntry<Block> MANGROVE_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("mangrove_backpack_shelf"), () -> new ShelfBlock(Block.Properties.ofFullCopy(Blocks.MANGROVE_PLANKS)));

    private static BlockBehaviour.Properties mushroomShelfProperties()
    {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .strength(0.2F, 3.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY);
    }

    public static final RegistryEntry<Block> MUSHROOM_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("mushroom_backpack_shelf"), () -> new ShelfBlock(mushroomShelfProperties()));
    public static final RegistryEntry<Block> WARPED_MUSHROOM_BACKPACK_SHELF = RegistryEntry.blockWithItem(Utils.rl("warped_mushroom_backpack_shelf"), () -> new ShelfBlock(mushroomShelfProperties()));

    private static BlockBehaviour.Properties mushroomGrowthStageProperties()
    {
        return mushroomShelfProperties().randomTicks();
    }

    public static final RegistryEntry<Block> MUSHROOM_SHELF_MEDIUM = RegistryEntry.blockWithItem(Utils.rl("mushroom_shelf_medium"), () -> new MushroomShelfSproutBlock(
        mushroomGrowthStageProperties(), MUSHROOM_BACKPACK_SHELF::get, MushroomShelfSproutBlock::isRegularLogSupport, true
    ));
    public static final RegistryEntry<Block> WARPED_MUSHROOM_SHELF_MEDIUM = RegistryEntry.blockWithItem(Utils.rl("warped_mushroom_shelf_medium"), () -> new MushroomShelfSproutBlock(
        mushroomGrowthStageProperties(), WARPED_MUSHROOM_BACKPACK_SHELF::get, MushroomShelfSproutBlock::isWarpedLogSupport, true
    ));

    public static final RegistryEntry<Block> MUSHROOM_SHELF_SPROUT = RegistryEntry.blockWithItem(Utils.rl("mushroom_shelf_sprout"), () -> new MushroomShelfSproutBlock(
        mushroomGrowthStageProperties(), MUSHROOM_SHELF_MEDIUM::get, MushroomShelfSproutBlock::isRegularLogSupport, false
    ));
    public static final RegistryEntry<Block> WARPED_MUSHROOM_SHELF_SPROUT = RegistryEntry.blockWithItem(Utils.rl("warped_mushroom_shelf_sprout"), () -> new MushroomShelfSproutBlock(
        mushroomGrowthStageProperties(), WARPED_MUSHROOM_SHELF_MEDIUM::get, MushroomShelfSproutBlock::isWarpedLogSupport, false
    ));

    public static final RegistryEntry<Block> HAY_BED = RegistryEntry.blockWithItem(Utils.rl("hay_bed"), () -> new HayBedBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_YELLOW)
        .strength(0.2F)
        .sound(SoundType.WOOD)
        .noOcclusion()
        .pushReaction(PushReaction.DESTROY)
    ), block -> new BlockItem(block, new Item.Properties().stacksTo(16)));

    // A log with a tunnel carved through it - visually and mechanically inspired by SpiritGameStudios'
    // "Hollow" mod (MPL-2.0). One pair (regular + stripped) per wood type this mod already has a
    // backpack shelf variant for, so the set stays consistent with the rest of the mod's wood-type
    // coverage; crimson/warped use "stem" rather than "log" to match vanilla's own nether wood naming.
    private static RegistryEntry<Block> registerHollowLog(String name, Block baseLog)
    {
        return RegistryEntry.blockWithItem(Utils.rl(name), () -> new HollowLogBlock(BlockBehaviour.Properties.ofFullCopy(baseLog).noOcclusion()));
    }

    public static final RegistryEntry<Block> OAK_HOLLOW_LOG = registerHollowLog("oak_hollow_log", Blocks.OAK_LOG);
    public static final RegistryEntry<Block> STRIPPED_OAK_HOLLOW_LOG = registerHollowLog("stripped_oak_hollow_log", Blocks.STRIPPED_OAK_LOG);
    public static final RegistryEntry<Block> SPRUCE_HOLLOW_LOG = registerHollowLog("spruce_hollow_log", Blocks.SPRUCE_LOG);
    public static final RegistryEntry<Block> STRIPPED_SPRUCE_HOLLOW_LOG = registerHollowLog("stripped_spruce_hollow_log", Blocks.STRIPPED_SPRUCE_LOG);
    public static final RegistryEntry<Block> BIRCH_HOLLOW_LOG = registerHollowLog("birch_hollow_log", Blocks.BIRCH_LOG);
    public static final RegistryEntry<Block> STRIPPED_BIRCH_HOLLOW_LOG = registerHollowLog("stripped_birch_hollow_log", Blocks.STRIPPED_BIRCH_LOG);
    public static final RegistryEntry<Block> JUNGLE_HOLLOW_LOG = registerHollowLog("jungle_hollow_log", Blocks.JUNGLE_LOG);
    public static final RegistryEntry<Block> STRIPPED_JUNGLE_HOLLOW_LOG = registerHollowLog("stripped_jungle_hollow_log", Blocks.STRIPPED_JUNGLE_LOG);
    public static final RegistryEntry<Block> DARK_OAK_HOLLOW_LOG = registerHollowLog("dark_oak_hollow_log", Blocks.DARK_OAK_LOG);
    public static final RegistryEntry<Block> STRIPPED_DARK_OAK_HOLLOW_LOG = registerHollowLog("stripped_dark_oak_hollow_log", Blocks.STRIPPED_DARK_OAK_LOG);
    public static final RegistryEntry<Block> ACACIA_HOLLOW_LOG = registerHollowLog("acacia_hollow_log", Blocks.ACACIA_LOG);
    public static final RegistryEntry<Block> STRIPPED_ACACIA_HOLLOW_LOG = registerHollowLog("stripped_acacia_hollow_log", Blocks.STRIPPED_ACACIA_LOG);
    public static final RegistryEntry<Block> CHERRY_HOLLOW_LOG = registerHollowLog("cherry_hollow_log", Blocks.CHERRY_LOG);
    public static final RegistryEntry<Block> STRIPPED_CHERRY_HOLLOW_LOG = registerHollowLog("stripped_cherry_hollow_log", Blocks.STRIPPED_CHERRY_LOG);
    public static final RegistryEntry<Block> CRIMSON_HOLLOW_STEM = registerHollowLog("crimson_hollow_stem", Blocks.CRIMSON_STEM);
    public static final RegistryEntry<Block> STRIPPED_CRIMSON_HOLLOW_STEM = registerHollowLog("stripped_crimson_hollow_stem", Blocks.STRIPPED_CRIMSON_STEM);
    public static final RegistryEntry<Block> WARPED_HOLLOW_STEM = registerHollowLog("warped_hollow_stem", Blocks.WARPED_STEM);
    public static final RegistryEntry<Block> STRIPPED_WARPED_HOLLOW_STEM = registerHollowLog("stripped_warped_hollow_stem", Blocks.STRIPPED_WARPED_STEM);
    public static final RegistryEntry<Block> MANGROVE_HOLLOW_LOG = registerHollowLog("mangrove_hollow_log", Blocks.MANGROVE_LOG);
    public static final RegistryEntry<Block> STRIPPED_MANGROVE_HOLLOW_LOG = registerHollowLog("stripped_mangrove_hollow_log", Blocks.STRIPPED_MANGROVE_LOG);
}

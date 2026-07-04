package com.mrcrayfish.backpacked.worldgen;

import com.mojang.serialization.Codec;
import com.mrcrayfish.backpacked.block.MushroomShelfSproutBlock;
import com.mrcrayfish.backpacked.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Scans the area around a chunk's already-generated oak/birch/warped stem logs (from the biome's
 * normal tree/fungus feature, which runs earlier in the same decoration step) and has a small
 * chance to attach a mushroom backpack shelf/sprout to their sides - similar in spirit to vanilla's
 * {@link net.minecraft.world.level.levelgen.feature.treedecorators.CocoaDecorator}, but implemented
 * as a standalone feature so it only has to be added to the specific biomes that want it (forest,
 * birch forest, warped forest), rather than needing to clone/replace the shared vanilla tree
 * configured features.
 *
 * Author: MrCrayfish
 */
public class MushroomShelfFeature extends Feature<NoneFeatureConfiguration>
{
    private static final int RADIUS_HORIZONTAL = 6;
    private static final int RADIUS_DOWN = 2;
    private static final int RADIUS_UP = 8;
    private static final float LOG_CHANCE = 0.18F;
    private static final float DIRECTION_CHANCE = 0.25F;
    private static final float MATURE_CHANCE = 0.35F;
    private static final float MEDIUM_CHANCE = 0.25F;

    public MushroomShelfFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        return tryAttachNearby(context.level(), context.random(), context.origin());
    }

    /**
     * Scans the area around {@code origin} for oak/birch/warped stem logs and has a small chance to
     * attach a mushroom shelf/sprout to their sides. Shared by natural worldgen ({@link #place}) and
     * by the sapling-growth chance (see the mod's {@code BlockGrowFeatureEvent} listener), which
     * calls this a tick after a sapling grows into a tree so the logs already exist to scan for.
     */
    public static boolean tryAttachNearby(WorldGenLevel level, RandomSource random, BlockPos origin)
    {
        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Heightmap-based placement (used to anchor `origin` for the Overworld forest biomes)
        // doesn't mean anything useful in ceiling-having dimensions like the Nether - scanning from
        // the "top down" there hits netherrack/bedrock formations, not the Warped Forest floor, so
        // the anchored +-radius scan almost never lands near an actual warped stem. In those
        // dimensions, ignore the anchor's Y entirely and scan the whole column instead.
        boolean fullColumn = level.dimensionType().hasCeiling();
        int minY = fullColumn ? level.getMinBuildHeight() : origin.getY() - RADIUS_DOWN;
        int maxY = fullColumn ? level.getMaxBuildHeight() - 1 : origin.getY() + RADIUS_UP;

        for(int dx = -RADIUS_HORIZONTAL; dx <= RADIUS_HORIZONTAL; dx++)
        {
            for(int dz = -RADIUS_HORIZONTAL; dz <= RADIUS_HORIZONTAL; dz++)
            {
                for(int y = minY; y <= maxY; y++)
                {
                    cursor.set(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState logState = level.getBlockState(cursor);
                    boolean warped = logState.is(Blocks.WARPED_STEM) || logState.is(Blocks.WARPED_HYPHAE);
                    if(!warped && !logState.is(Blocks.OAK_LOG) && !logState.is(Blocks.BIRCH_LOG))
                        continue;

                    // Only the log/stem actually resting on the ground counts as "the bottom one" -
                    // fungi in particular branch and twist rather than growing as a straight vertical
                    // column, so checking "is there a log directly below" would (and did) wrongly
                    // exclude most of a warped fungus, not just its base.
                    if(isGroundBlock(level.getBlockState(cursor.below())))
                        continue;

                    if(random.nextFloat() >= LOG_CHANCE)
                        continue;

                    Block matureBlock = warped ? ModBlocks.WARPED_MUSHROOM_BACKPACK_SHELF.get() : ModBlocks.MUSHROOM_BACKPACK_SHELF.get();
                    Block mediumBlock = warped ? ModBlocks.WARPED_MUSHROOM_SHELF_MEDIUM.get() : ModBlocks.MUSHROOM_SHELF_MEDIUM.get();
                    Block sproutBlock = warped ? ModBlocks.WARPED_MUSHROOM_SHELF_SPROUT.get() : ModBlocks.MUSHROOM_SHELF_SPROUT.get();

                    for(Direction direction : Direction.Plane.HORIZONTAL)
                    {
                        if(random.nextFloat() > DIRECTION_CHANCE)
                            continue;

                        BlockPos target = cursor.relative(direction);
                        if(!level.isEmptyBlock(target))
                            continue;

                        float roll = random.nextFloat();
                        boolean mature = roll < MATURE_CHANCE;
                        boolean medium = !mature && roll < MATURE_CHANCE + MEDIUM_CHANCE;
                        Block toPlaceBlock = mature ? matureBlock : (medium ? mediumBlock : sproutBlock);
                        // The mature shelf (a ShelfBlock) faces away from its support, like the
                        // other wall-mounted shelves; the sprout/medium (a cocoa-style attached crop)
                        // faces towards its support - opposite conventions, so they need opposite
                        // FACING values here even though they're placed the same way.
                        Direction facing = mature ? direction : direction.getOpposite();
                        BlockState toPlace = toPlaceBlock.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
                        if(!mature)
                        {
                            toPlace = toPlace.setValue(MushroomShelfSproutBlock.AGE, MushroomShelfSproutBlock.randomStartAge(random));
                        }
                        level.setBlock(target, toPlace, 2);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }

    private static boolean isGroundBlock(BlockState state)
    {
        return state.is(BlockTags.DIRT) || state.is(Blocks.WARPED_NYLIUM) || state.is(Blocks.CRIMSON_NYLIUM) || state.is(Blocks.NETHERRACK);
    }
}

package com.mrcrayfish.backpacked.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrcrayfish.backpacked.block.HollowLogBlock;
import com.mrcrayfish.backpacked.block.MushroomShelfSproutBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lays a horizontal line of {@link HollowLogBlock} along a random horizontal axis, like a tree that
 * fell over - each segment has an independent chance (via {@link Config#topBlockProvider()}) of
 * growing something on top (updating the log's {@code LAYER} property to match) and another
 * independent chance (via {@link Config#sideBlockProvider()}) of a decoration - a vine, or (in
 * biomes that also get the mushroom shelf feature) a mushroom shelf sprout - on one side.
 * {@link Config#guaranteedSideProvider()}/{@link Config#guaranteedSideCount()} then top that up to
 * a guaranteed minimum (retroactively overwriting vine/empty side spots if the random rolls didn't
 * place enough on their own), so a mushroom-shelf-biome fallen tree reliably has a couple of sprouts
 * rather than sometimes generating with none at all.
 * <p>
 * Adapted from SpiritGameStudios' "Hollow" mod (MPL-2.0) - {@code FallenTreeFeature.java} -
 * translated from Fabric/Yarn mappings to NeoForge/Mojang mappings.
 *
 * Author: MrCrayfish
 */
public class FallenTreeFeature extends Feature<FallenTreeFeature.Config>
{
    public FallenTreeFeature(Codec<Config> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context)
    {
        WorldGenLevel level = context.level();
        BlockPos origin = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, context.origin());
        RandomSource random = context.random();
        Config config = context.config();

        int size = config.baseHeight() + random.nextInt(config.variance());
        Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        BlockState trunkState = trySetValue(config.stateProvider().getState(random, origin), HollowLogBlock.AXIS, axis);

        for(int i = 0; i < size; i++)
        {
            BlockPos pos = origin.relative(axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, i);
            if((!level.isEmptyBlock(pos) && !level.getBlockState(pos).canBeReplaced()) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
                return false;
        }

        List<SideCandidate> reclaimableSidePositions = new ArrayList<>();
        int guaranteedPlaced = 0;

        for(int i = 0; i < size; i++)
        {
            BlockPos pos = origin.relative(axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, i);
            level.setBlock(pos, trunkState, Block.UPDATE_ALL);

            BlockPos abovePos = pos.above();
            if(level.isEmptyBlock(abovePos))
            {
                BlockState top = config.topBlockProvider().getState(random, abovePos);
                if(!top.isAir())
                {
                    level.setBlock(abovePos, top, Block.UPDATE_ALL);
                    level.setBlock(pos, trunkState.setValue(HollowLogBlock.LAYER, HollowLogBlock.Layer.get(top)), Block.UPDATE_ALL);
                }
            }

            Direction direction = axis == Direction.Axis.X
                ? (random.nextBoolean() ? Direction.NORTH : Direction.SOUTH)
                : (random.nextBoolean() ? Direction.EAST : Direction.WEST);

            BlockPos sidePos = pos.relative(direction);
            BlockState sideState = level.getBlockState(sidePos);
            if(!sideState.isAir() && !sideState.canBeReplaced())
                continue;

            BlockState decoration = config.sideBlockProvider().getState(random, sidePos);
            if(decoration.isAir())
            {
                reclaimableSidePositions.add(new SideCandidate(sidePos, direction));
                continue;
            }

            level.setBlock(sidePos, orientSideDecoration(decoration, direction), Block.UPDATE_ALL);
            if(decoration.getBlock() instanceof MushroomShelfSproutBlock)
            {
                guaranteedPlaced++;
            }
            else
            {
                // Not air, but also not the guaranteed type (e.g. a vine) - still fair game to
                // overwrite below if the guarantee needs more spots than went to air.
                reclaimableSidePositions.add(new SideCandidate(sidePos, direction));
            }
        }

        if(config.guaranteedSideProvider().isPresent())
        {
            BlockStateProvider guaranteedProvider = config.guaranteedSideProvider().get();
            int needed = config.guaranteedSideCount() - guaranteedPlaced;
            while(needed > 0 && !reclaimableSidePositions.isEmpty())
            {
                SideCandidate candidate = reclaimableSidePositions.remove(random.nextInt(reclaimableSidePositions.size()));
                BlockState decoration = guaranteedProvider.getState(random, candidate.pos());
                level.setBlock(candidate.pos(), orientSideDecoration(decoration, candidate.direction()), Block.UPDATE_ALL);
                needed--;
            }
        }

        return true;
    }

    private static <T extends Comparable<T>> BlockState trySetValue(BlockState state, Property<T> property, T value)
    {
        return state.hasProperty(property) ? state.setValue(property, value) : state;
    }

    /**
     * Orients a side decoration so it looks attached to the trunk - a vine gets the boolean face
     * property facing back at the trunk (matching how vines report their support), anything else
     * with a horizontal-facing property (like our mushroom shelf sprout) faces back at the trunk too.
     */
    private static BlockState orientSideDecoration(BlockState state, Direction directionFromTrunk)
    {
        if(state.getBlock() instanceof VineBlock)
        {
            return state.setValue(VineBlock.getPropertyForFace(directionFromTrunk.getOpposite()), true);
        }
        return trySetValue(state, HorizontalDirectionalBlock.FACING, directionFromTrunk.getOpposite());
    }

    private record SideCandidate(BlockPos pos, Direction direction)
    {
    }

    public record Config(
        BlockStateProvider stateProvider,
        int baseHeight,
        int variance,
        BlockStateProvider topBlockProvider,
        BlockStateProvider sideBlockProvider,
        Optional<BlockStateProvider> guaranteedSideProvider,
        int guaranteedSideCount
    ) implements FeatureConfiguration
    {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(Config::stateProvider),
            Codec.INT.optionalFieldOf("base_height", 3).forGetter(Config::baseHeight),
            Codec.INT.optionalFieldOf("variance", 2).forGetter(Config::variance),
            BlockStateProvider.CODEC.fieldOf("top_block_provider").forGetter(Config::topBlockProvider),
            BlockStateProvider.CODEC.fieldOf("side_block_provider").forGetter(Config::sideBlockProvider),
            BlockStateProvider.CODEC.optionalFieldOf("guaranteed_side_provider").forGetter(Config::guaranteedSideProvider),
            Codec.INT.optionalFieldOf("guaranteed_side_count", 0).forGetter(Config::guaranteedSideCount)
        ).apply(instance, Config::new));
    }
}

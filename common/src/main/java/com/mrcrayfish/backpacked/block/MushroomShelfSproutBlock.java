package com.mrcrayfish.backpacked.block;

import com.mojang.serialization.MapCodec;
import com.mrcrayfish.backpacked.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A growth stage of a mushroom backpack shelf, attached to a supporting block like cocoa beans.
 * The same class backs two stages - a small "sprout" (the one you plant, a fresh
 * {@code AGE}-0 item) and a bigger "medium" it grows into - which then converts into the mature
 * {@code ShelfBlock} once fully grown; {@code medium} just picks which of the two hitbox/rotation
 * shape sets to use. Parametrized by which block it matures into and what counts as valid support,
 * so the same class backs both the regular (any overworld log or mushroom block) and Warped
 * (warped/crimson stem) variants too.
 * Uses a standard {@code BlockStateProperties} age property (rather than a custom one) so that
 * mods like Jade, which generically show growth progress for any {@link BonemealableBlock} with one
 * of those properties, display it automatically without needing any integration code here.
 *
 * Author: MrCrayfish
 */
public class MushroomShelfSproutBlock extends HorizontalDirectionalBlock implements BonemealableBlock
{
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    public static final int MAX_AGE = 5;
    public static final BooleanProperty SHEARED = BooleanProperty.create("sheared");

    /** 1 in this many random ticks advances the age by one */
    private static final int GROWTH_ODDS = 5;

    private static final VoxelShape SPROUT_NORTH_SHAPE = Block.box(5.5, 1.5, 12, 10.5, 3, 16);
    private static final VoxelShape SPROUT_SOUTH_SHAPE = Block.box(5.5, 1.5, 0, 10.5, 3, 4);
    private static final VoxelShape SPROUT_WEST_SHAPE = Block.box(12, 1.5, 5.5, 16, 3, 10.5);
    private static final VoxelShape SPROUT_EAST_SHAPE = Block.box(0, 1.5, 5.5, 4, 3, 10.5);

    private static final VoxelShape MEDIUM_NORTH_SHAPE = Block.box(4, 1.5, 10, 12, 4.5, 16);
    private static final VoxelShape MEDIUM_SOUTH_SHAPE = Block.box(4, 1.5, 0, 12, 4.5, 6);
    private static final VoxelShape MEDIUM_WEST_SHAPE = Block.box(10, 1.5, 4, 16, 4.5, 12);
    private static final VoxelShape MEDIUM_EAST_SHAPE = Block.box(0, 1.5, 4, 6, 4.5, 12);

    private final Supplier<Block> matureBlock;
    private final Predicate<BlockState> validSupport;
    private final boolean medium;

    public MushroomShelfSproutBlock(Properties properties, Supplier<Block> matureBlock, Predicate<BlockState> validSupport, boolean medium)
    {
        super(properties);
        this.matureBlock = matureBlock;
        this.validSupport = validSupport;
        this.medium = medium;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0).setValue(SHEARED, false));
    }

    // This block is only ever created via ModBlocks with specific matureBlock/validSupport/medium
    // parameters, which simpleCodec's Properties-only constructor shape can't express. The codec
    // isn't exercised by normal gameplay (nothing here is datapack-defined), so it just falls back
    // to the regular oak/birch sprout variant if something ever does call it, rather than throwing.
    private static final MapCodec<MushroomShelfSproutBlock> CODEC = simpleCodec(properties ->
        new MushroomShelfSproutBlock(properties, ModBlocks.MUSHROOM_BACKPACK_SHELF::get, MushroomShelfSproutBlock::isRegularLogSupport, false)
    );

    @Override
    protected MapCodec<MushroomShelfSproutBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return this.validSupport.test(level.getBlockState(pos.relative(state.getValue(FACING))));
    }

    public static boolean isRegularLogSupport(BlockState state)
    {
        return state.is(BlockTags.OVERWORLD_NATURAL_LOGS)
            || state.is(Blocks.MUSHROOM_STEM)
            || state.is(Blocks.RED_MUSHROOM_BLOCK)
            || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
            || state.is(ModBlocks.OAK_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_OAK_HOLLOW_LOG.get())
            || state.is(ModBlocks.SPRUCE_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_SPRUCE_HOLLOW_LOG.get())
            || state.is(ModBlocks.BIRCH_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_BIRCH_HOLLOW_LOG.get())
            || state.is(ModBlocks.JUNGLE_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_JUNGLE_HOLLOW_LOG.get())
            || state.is(ModBlocks.DARK_OAK_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_DARK_OAK_HOLLOW_LOG.get())
            || state.is(ModBlocks.ACACIA_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_ACACIA_HOLLOW_LOG.get())
            || state.is(ModBlocks.CHERRY_HOLLOW_LOG.get()) || state.is(ModBlocks.STRIPPED_CHERRY_HOLLOW_LOG.get());
    }

    public static boolean isWarpedLogSupport(BlockState state)
    {
        return state.is(Blocks.WARPED_STEM) || state.is(Blocks.WARPED_HYPHAE)
            || state.is(Blocks.CRIMSON_STEM) || state.is(Blocks.CRIMSON_HYPHAE)
            || state.is(ModBlocks.CRIMSON_HOLLOW_STEM.get()) || state.is(ModBlocks.STRIPPED_CRIMSON_HOLLOW_STEM.get())
            || state.is(ModBlocks.WARPED_HOLLOW_STEM.get()) || state.is(ModBlocks.STRIPPED_WARPED_HOLLOW_STEM.get());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state)
    {
        return !state.getValue(SHEARED) && state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if(!state.getValue(SHEARED) && state.getValue(AGE) < MAX_AGE && random.nextInt(GROWTH_ODDS) == 0)
        {
            this.grow(state, level, pos);
        }
    }

    private void grow(BlockState state, ServerLevel level, BlockPos pos)
    {
        int age = state.getValue(AGE) + 1;
        if(age >= MAX_AGE)
        {
            Block next = this.matureBlock.get();
            // The final ShelfBlock's support is at pos.relative(FACING.getOpposite()) - opposite
            // convention from this class' pos.relative(FACING) - so only that transition needs the
            // FACING flipped to keep pointing at the same support. Sprout -> medium stays the same
            // class (same convention), so no flip there.
            Direction nextFacing = next instanceof MushroomShelfSproutBlock ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
            level.setBlock(pos, next.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, nextFacing), Block.UPDATE_ALL);
        }
        else
        {
            level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    {
        return !state.getValue(SHEARED) && state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state)
    {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state)
    {
        this.grow(state, level, pos);
    }

    // Shearing either growth stage stops it from ever maturing further, letting a player keep a
    // small sprout or medium shelf as permanent decoration instead of it eventually turning into
    // the (bigger, backpack-holding) mature shelf.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result)
    {
        if(stack.is(Items.SHEARS) && !state.getValue(SHEARED))
        {
            if(!level.isClientSide())
            {
                level.setBlock(pos, state.setValue(SHEARED, true), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, result);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        // These constants are named after the rotation degree they represent (NORTH_SHAPE = the
        // model's own unrotated/0 degree geometry, SOUTH_SHAPE = 180 degrees, etc.), which no longer
        // matches the FACING enum they're named after now that the blockstate's rotation mapping is
        // (correctly) south=0/west=90/north=180/east=270 - see blockstates/mushroom_shelf_sprout.json.
        if(this.medium)
        {
            return switch(state.getValue(FACING))
            {
                case NORTH -> MEDIUM_SOUTH_SHAPE;
                case EAST -> MEDIUM_WEST_SHAPE;
                case WEST -> MEDIUM_EAST_SHAPE;
                default -> MEDIUM_NORTH_SHAPE;
            };
        }
        return switch(state.getValue(FACING))
        {
            case NORTH -> SPROUT_SOUTH_SHAPE;
            case EAST -> SPROUT_WEST_SHAPE;
            case WEST -> SPROUT_EAST_SHAPE;
            default -> SPROUT_NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = this.defaultBlockState().setValue(AGE, randomStartAge(context.getLevel().getRandom()));
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for(Direction direction : context.getNearestLookingDirections())
        {
            if(direction.getAxis().isHorizontal())
            {
                state = state.setValue(FACING, direction);
                if(state.canSurvive(level, pos))
                {
                    return state;
                }
            }
        }
        return null;
    }

    /**
     * Picks a random starting {@link #AGE} such that reaching {@link #MAX_AGE} (full growth) takes
     * anywhere from 1 to 5 more bonemeal applications/random-tick growths, varying per sprout.
     */
    public static int randomStartAge(RandomSource random)
    {
        int treatmentsNeeded = 1 + random.nextInt(5);
        return MAX_AGE - treatmentsNeeded;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        return direction == state.getValue(FACING) && !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, AGE, SHEARED);
    }
}

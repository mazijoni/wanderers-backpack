package com.mrcrayfish.backpacked.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A log with a square tunnel carved through its core, oriented like a normal pillar log. Its
 * {@link #LAYER} property mirrors whatever sits directly on top of it (moss/snow), purely cosmetic
 * (an overlay baked into the blockstate model) - kept in sync via {@link #updateShape} whenever the
 * block above changes, the same way a real log doesn't otherwise react to its neighbours.
 * <p>
 * Adapted from SpiritGameStudios' "Hollow" mod (MPL-2.0) - {@code HollowLogBlock.java} - translated
 * from Fabric/Yarn mappings to NeoForge/Mojang mappings, with the {@code pale_moss} layer dropped
 * since Minecraft 1.21.1 (this mod's target version) predates the Pale Garden update that added it.
 *
 * Author: MrCrayfish
 */
public class HollowLogBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock
{
    public static final MapCodec<HollowLogBlock> CODEC = simpleCodec(HollowLogBlock::new);

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Layer> LAYER = EnumProperty.create("layer", Layer.class);

    private static final VoxelShape SHAPE_X = Shapes.or(
        Block.box(0, 14, 0, 16, 16, 16),
        Block.box(0, 0, 2, 16, 2, 14),
        Block.box(0, 0, 0, 16, 14, 2),
        Block.box(0, 0, 14, 16, 14, 16)
    );

    private static final VoxelShape SHAPE_Y = Shapes.or(
        Block.box(0, 0, 0, 2, 16, 16),
        Block.box(14, 0, 2, 16, 16, 14),
        Block.box(2, 0, 0, 16, 16, 2),
        Block.box(2, 0, 14, 16, 16, 16)
    );

    private static final VoxelShape SHAPE_Z = Shapes.or(
        Block.box(0, 14, 0, 16, 16, 16),
        Block.box(2, 0, 0, 14, 2, 16),
        Block.box(0, 0, 0, 2, 14, 16),
        Block.box(14, 0, 0, 16, 14, 16)
    );

    public HollowLogBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(WATERLOGGED, false)
            .setValue(LAYER, Layer.NONE)
        );
    }

    @Override
    public MapCodec<HollowLogBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(AXIS, WATERLOGGED, LAYER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
            .setValue(AXIS, context.getClickedFace().getAxis())
            .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER)
            .setValue(LAYER, Layer.get(above));
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos)
    {
        if(state.getValue(WATERLOGGED))
        {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if(direction == Direction.UP)
        {
            return state.setValue(LAYER, Layer.get(neighbourState));
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch(state.getValue(AXIS))
        {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    public enum Layer implements StringRepresentable
    {
        NONE("none"),
        MOSS("moss"),
        SNOW("snow");

        private final String name;

        Layer(String name)
        {
            this.name = name;
        }

        public static Layer get(BlockState aboveState)
        {
            if(aboveState.is(Blocks.MOSS_BLOCK) || aboveState.is(Blocks.MOSS_CARPET))
                return MOSS;

            if(aboveState.is(Blocks.SNOW_BLOCK) || aboveState.is(Blocks.SNOW))
                return SNOW;

            return NONE;
        }

        @Override
        public String getSerializedName()
        {
            return this.name;
        }
    }
}

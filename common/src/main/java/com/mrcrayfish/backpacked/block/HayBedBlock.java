package com.mrcrayfish.backpacked.block;

import com.mojang.serialization.MapCodec;
import com.mrcrayfish.backpacked.blockentity.HayBedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A portable version of vanilla's {@code BedBlock} - it's a direct subclass, so it reuses vanilla's
 * head/foot placement, shape-sync, and {@code useWithoutItem} interaction (sleeping, the OCCUPIED
 * state, villager kick-out, the bad-spawn-point explosion in dimensions without beds) completely
 * unmodified. The only additions on top: placing it (without sneaking) immediately puts the placer
 * to sleep, it renders via a normal blockstate model instead of vanilla's per-color
 * {@code BedBlockEntity} renderer (see {@link #getRenderShape}), and it disappears back into the
 * placer's inventory once they get up (see the mod's {@code PlayerWakeUpEvent} listener, which uses
 * {@link HayBedBlockEntity#isSummoned()} to tell a manually-placed Hay Bed apart from one the Bed
 * augment conjured).
 *
 * Author: MrCrayfish
 */
public class HayBedBlock extends BedBlock
{
    // BedBlock.codec() is declared as returning MapCodec<BedBlock> (not a wildcard), so - unlike a
    // direct Block subclass - this override can't narrow the type to MapCodec<HayBedBlock>; generics
    // are invariant in Java even though HayBedBlock is-a BedBlock.
    public static final MapCodec<BedBlock> CODEC = simpleCodec(HayBedBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public HayBedBlock(Properties properties)
    {
        super(DyeColor.YELLOW, properties);
    }

    @Override
    public MapCodec<BedBlock> codec()
    {
        return CODEC;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if(!level.isClientSide() && placer instanceof Player player && !player.isCrouching())
        {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            tryEnterBed(level, headPos, player);
        }
    }

    // Inherited as-is from BedBlock except wrapped so it can't set the player's respawn point (see
    // withoutSettingRespawn) - this is a temporary camping bed, not a real one.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        return withoutSettingRespawn(player, () -> super.useWithoutItem(state, level, pos, player, hit));
    }

    // BedBlock's own playerWillDestroy only silently removes the partner half when the FOOT is the
    // one destroyed in creative mode - breaking the HEAD instead leaves the FOOT behind (it'd only
    // get cleaned up later, by a separate updateShape neighbour update). Handling the HEAD direction
    // here too means a creative-mode break of either half removes the whole bed at once, with
    // neither half ever dropping an item - this is a temporary camping bed, not a real placed block.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        if(!level.isClientSide() && player.isCreative() && state.getValue(PART) == BedPart.HEAD)
        {
            BlockPos footPos = pos.relative(state.getValue(FACING));
            BlockState footState = level.getBlockState(footPos);
            if(footState.is(this) && footState.getValue(PART) == BedPart.FOOT)
            {
                level.setBlock(footPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, footPos, Block.getId(footState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * @return whether it's currently valid to fall asleep - mirrors vanilla's bed rule (night, or a
     * thunderstorm). Used as a pre-flight check by the Bed augment before it summons a bed, so it can
     * show a message instead of placing one that would immediately fail to be sleepable.
     */
    public static boolean canSleepNow(Level level)
    {
        return !level.isDay() || level.isThundering();
    }

    public static InteractionResult tryEnterBed(Level level, BlockPos pos, Player player)
    {
        return withoutSettingRespawn(player, () -> {
            if(level.isClientSide())
                return InteractionResult.CONSUME;

            if(player.isSleeping() || !canSleepNow(level))
                return InteractionResult.PASS;

            player.startSleepInBed(pos);
            return InteractionResult.SUCCESS;
        });
    }

    /**
     * {@code ServerPlayer#startSleepInBed} sets the player's respawn point as a side effect of just
     * attempting to sleep (even if the attempt then fails, e.g. because it's daytime) - vanilla bed
     * behaviour, but wrong for this portable camping bed. Snapshot the previous respawn data before
     * running the (otherwise unmodified) vanilla sleep logic, then silently restore it after.
     */
    private static <T> T withoutSettingRespawn(Player player, Supplier<T> action)
    {
        if(!(player instanceof ServerPlayer serverPlayer))
            return action.get();

        ResourceKey<Level> dimension = serverPlayer.getRespawnDimension();
        BlockPos position = serverPlayer.getRespawnPosition();
        float angle = serverPlayer.getRespawnAngle();
        boolean forced = serverPlayer.isRespawnForced();

        T result = action.get();

        serverPlayer.setRespawnPosition(dimension, position, angle, forced, false);
        return result;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    // Both halves share this one Block's loot table (unlike vanilla's colored beds, which rely
    // entirely on the HEAD half always being silently removed - via playerWillDestroy in creative,
    // or updateShape's neighbour collapse in survival - before it could ever be harvested directly).
    // Restricting drops to the FOOT half guarantees a whole bed can only ever produce at most one
    // item no matter what happens to the HEAD half, instead of relying on that collapse always
    // winning the race against the player also mining it directly.
    //
    // The mod's own "give it back on wake-up" logic (see the PlayerWakeUpEvent listener) only ever
    // covers the sleep-then-wake flow. If a player instead just mines a summoned bed directly - the
    // Bed augment's temporary bed, never placed from an actual item - the generic loot table has no
    // idea it's summoned and would drop a real Hay Bed anyway. This is the actual per-block loot
    // entry point (unlike the static Block.getDrops helpers), so check the flag here too.
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        if(state.getValue(PART) == BedPart.HEAD)
        {
            return Collections.emptyList();
        }
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HayBedBlockEntity hayBedBlockEntity && hayBedBlockEntity.isSummoned())
        {
            return Collections.emptyList();
        }
        return super.getDrops(state, params);
    }

    // Our model (see hay_bed_head.json/hay_bed_feet.json) fits within each half's own block bounds,
    // unlike vanilla's, so it can render as a normal blockstate model instead of needing vanilla's
    // BedBlockEntity-driven per-color renderer.
    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new HayBedBlockEntity(pos, state);
    }
}

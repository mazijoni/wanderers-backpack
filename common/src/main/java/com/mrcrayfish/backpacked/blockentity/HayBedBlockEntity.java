package com.mrcrayfish.backpacked.blockentity;

import com.mrcrayfish.backpacked.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mostly holds no state of its own - it only exists so {@code HayBedBlock} (whose model is far
 * larger than one block) has a block entity to hang a renderer off, matching how
 * {@code ShelfBlockEntity} draws the backpack sitting on a backpack shelf. The one bit of state,
 * {@link #summoned}, distinguishes a real placed-from-inventory Hay Bed (returned to the player on
 * wake-up) from the temporary one the Bed augment conjures (which should just vanish, not drop or
 * return an item nobody put there).
 *
 * Author: MrCrayfish
 */
public class HayBedBlockEntity extends BlockEntity
{
    private boolean summoned;

    public HayBedBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.HAY_BED.get(), pos, state);
    }

    public boolean isSummoned()
    {
        return this.summoned;
    }

    public void setSummoned(boolean summoned)
    {
        this.summoned = summoned;
        this.setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        this.summoned = tag.getBoolean("Summoned");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Summoned", this.summoned);
    }
}

package com.mrcrayfish.backpacked.inventory.container.slot;

import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
public class BackpackSlot extends Slot
{
    public BackpackSlot(Container container, int index, int x, int y)
    {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return BackpackInventory.isAllowedItem(stack);
    }
}

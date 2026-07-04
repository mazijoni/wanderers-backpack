package com.mrcrayfish.backpacked.inventory;

import com.mrcrayfish.backpacked.blockentity.ShelfBlockEntity;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.inventory.container.UnlockableContainer;
import com.mrcrayfish.backpacked.item.BackpackItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The live, editable view of a backpack's storage grid while it's being browsed straight off a
 * shelf (sneak right-click), without taking it off. Deliberately does NOT extend
 * {@link BackpackInventory} - several network handlers in {@code ServerPlayHandler} (rename, sort,
 * augment toggle/settings) key off {@code instanceof BackpackInventory} to only run for an
 * equipped backpack (one that lives at a real {@code backpackIndex} on some player), since those
 * actions need a stable per-player index to route back to. A shelved backpack has no such index,
 * so it must stay a plain {@link UnlockableContainer} to keep those guards correctly disabling
 * themselves here instead of routing to the wrong backpack.
 *
 * Author: MrCrayfish
 */
public class ShelfBackpackContainer extends UnlockableContainer
{
    private final ShelfBlockEntity shelf;
    private final ItemStack stack;
    private boolean save;

    public ShelfBackpackContainer(int columns, int rows, ShelfBlockEntity shelf)
    {
        super(columns * rows);
        this.shelf = shelf;
        this.stack = shelf.getBackpack();
        ItemContainerContents contents = this.stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        contents.copyInto(this.items);
    }

    public ItemStack getBackpackStack()
    {
        return this.stack;
    }

    @Override
    protected UnlockableSlots getUnlockableSlots()
    {
        if(this.stack.getItem() instanceof BackpackItem item)
        {
            return item.getUnlockableSlots(this.stack);
        }
        return UnlockableSlots.ALL;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return !this.shelf.isRemoved() && this.shelf.getBackpack().equals(this.stack);
    }

    @Override
    public void setChanged()
    {
        this.save = true;
    }

    public void tick()
    {
        if(this.save)
        {
            this.saveItemsToStack();
            this.save = false;
        }
    }

    public void saveItemsToStack()
    {
        this.stack.set(DataComponents.CONTAINER, this.createContents());
    }
}

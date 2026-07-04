package com.mrcrayfish.backpacked.common;

import com.mrcrayfish.backpacked.Constants;
import com.mrcrayfish.backpacked.core.ModDataComponents;
import com.mrcrayfish.backpacked.item.BackpackItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Preserves a backpack's contents when it's crafted into a different tier (e.g. backpack ->
 * copper_backpack). Vanilla crafting has no concept of "upgrading" an ingredient - it just
 * consumes it and produces a fresh result stack - so without this, a tier-up recipe would
 * silently delete everything stored inside the old backpack.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public class BackpackUpgradeEvents
{
    @SubscribeEvent
    private static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        ItemStack result = event.getCrafting();
        if(!(result.getItem() instanceof BackpackItem))
            return;

        Container craftMatrix = event.getInventory();
        for(int i = 0; i < craftMatrix.getContainerSize(); i++)
        {
            ItemStack ingredient = craftMatrix.getItem(i);
            if(ingredient.getItem() instanceof BackpackItem)
            {
                transferContents(ingredient, result);
                break;
            }
        }
    }

    private static void transferContents(ItemStack from, ItemStack to)
    {
        if(from.has(DataComponents.CONTAINER))
        {
            to.set(DataComponents.CONTAINER, from.get(DataComponents.CONTAINER));
        }
        if(from.has(ModDataComponents.UNLOCKABLE_SLOTS.get()))
        {
            to.set(ModDataComponents.UNLOCKABLE_SLOTS.get(), from.get(ModDataComponents.UNLOCKABLE_SLOTS.get()));
        }
        if(from.has(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get()))
        {
            to.set(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get(), from.get(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get()));
        }
        if(from.has(ModDataComponents.AUGMENTS.get()))
        {
            to.set(ModDataComponents.AUGMENTS.get(), from.get(ModDataComponents.AUGMENTS.get()));
        }
    }
}

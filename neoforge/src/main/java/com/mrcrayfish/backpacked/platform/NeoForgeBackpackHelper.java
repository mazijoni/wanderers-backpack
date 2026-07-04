package com.mrcrayfish.backpacked.platform;

import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.mrcrayfish.backpacked.inventory.container.data.BackpackContainerData;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.platform.services.IBackpackHelper;
import com.mrcrayfish.framework.api.FrameworkAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Author: MrCrayfish
 */
public class NeoForgeBackpackHelper implements IBackpackHelper
{
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    @Override
    public boolean isBackpackVisible(Player player)
    {
        return true;
    }

    @Override
    public void openBackpackScreen(ServerPlayer openingPlayer, Container inventory, int ownerId, int backpackIndex, int cols, int rows, boolean owner, UnlockableSlots slots, Pagination pagination, Augments augments, Component title, UnlockableSlots bays, int augmentSlots)
    {
        FrameworkAPI.openMenuWithData(openingPlayer, new SimpleMenuProvider((id, playerInventory, entity) -> {
            return new BackpackContainerMenu(id, openingPlayer.getInventory(), inventory, ownerId, backpackIndex, cols, rows, owner, slots, pagination, augments, bays);
        }, title), new BackpackContainerData(backpackIndex, cols, rows, owner, slots, pagination, augments, bays, augmentSlots));
    }

    @Override
    public BackpackItem createBackpackItem(Item.Properties properties, int columns, int rows)
    {
        return new BackpackItem(properties, columns, rows);
    }

    @Override
    public BackpackItem createBackpackItem(Item.Properties properties, int columns, int rows, int maxAugmentBays)
    {
        return new BackpackItem(properties, columns, rows, maxAugmentBays);
    }

    @Override
    public ItemStack getCuriosBackpackStack(Player player)
    {
        return CURIOS_LOADED ? CuriosBackpackSupport.getStack(player) : ItemStack.EMPTY;
    }

    @Override
    public boolean setCuriosBackpackStack(Player player, ItemStack stack)
    {
        return CURIOS_LOADED && CuriosBackpackSupport.setStack(player, stack);
    }
}

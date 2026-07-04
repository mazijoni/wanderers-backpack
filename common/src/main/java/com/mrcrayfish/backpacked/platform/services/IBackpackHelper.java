package com.mrcrayfish.backpacked.platform.services;

import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.item.BackpackItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
public interface IBackpackHelper
{
    boolean isBackpackVisible(Player player);

    void openBackpackScreen(ServerPlayer openingPlayer, Container inventory, int ownerId, int backpackIndex, int cols, int rows, boolean owner, UnlockableSlots slots, Pagination pagination, Augments augments, Component title, UnlockableSlots bays, int augmentSlots);

    BackpackItem createBackpackItem(Item.Properties properties, int columns, int rows);

    BackpackItem createBackpackItem(Item.Properties properties, int columns, int rows, int maxAugmentBays);

    /** Reads the backpack currently equipped in the player's Curios "back" slot. Always empty if Curios is not installed. */
    ItemStack getCuriosBackpackStack(Player player);

    /** Writes the given stack into the player's Curios "back" slot. Returns false if Curios is not installed or has no back slot for this player. */
    boolean setCuriosBackpackStack(Player player, ItemStack stack);
}

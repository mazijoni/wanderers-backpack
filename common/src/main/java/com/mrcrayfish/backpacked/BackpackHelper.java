package com.mrcrayfish.backpacked;

import com.mrcrayfish.backpacked.common.InventoryAugmentSnapshot;
import com.mrcrayfish.backpacked.common.Navigate;
import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.core.ModSyncedDataKeys;
import com.mrcrayfish.backpacked.inventory.BackpackedInventoryAccess;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.platform.Services;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Backpacks can be equipped in two places at once: the player's vanilla chest armor slot
 * ({@link #INDEX_CHEST}) and, if Curios is installed, the Curios "back" slot ({@link #INDEX_CURIOS}).
 * Both are addressed through the same "backpack index" space used everywhere in this mod, which is
 * then extended with every ordinary player inventory slot ({@link #INDEX_INVENTORY_START} onwards)
 * so that the backpack screen's navigation arrows can page through every backpack the player is
 * carrying, not just the equipped ones. Methods that must stay restricted to "the backpack(s) you
 * have worn" (cosmetic rendering, pickpocketing, death handling) only ever look at indices 0 and 1;
 * methods driving the browse-arrows/last-opened behaviour scan the full index space.
 *
 * Author: MrCrayfish
 */
public class BackpackHelper
{
    public static final int INDEX_CHEST = 0;
    public static final int INDEX_CURIOS = 1;
    public static final int INDEX_INVENTORY_START = 2;
    private static final int[] EQUIPPED_INDICES = {INDEX_CHEST, INDEX_CURIOS};

    /**
     * @return the total number of addressable backpack indices for this player (chest + curios +
     * every main/hotbar inventory slot). Indices beyond this are always out of bounds.
     */
    public static int getTotalBackpackIndices(Player player)
    {
        return INDEX_INVENTORY_START + player.getInventory().items.size();
    }

    public static boolean isEquippedIndex(int index)
    {
        return index == INDEX_CHEST || index == INDEX_CURIOS;
    }

    /**
     * Finds the backpack the player should resume browsing (used when pressing the open-backpack
     * key with no explicit index). Prefers the last opened index if it still holds a backpack;
     * otherwise defaults to the Curios back slot, falling back to the chest armor slot, and finally
     * to the first backpack found anywhere else in the player's inventory.
     */
    public static int getSelectedBackpackIndex(Player player)
    {
        int remembered = ModSyncedDataKeys.SELECTED_BACKPACK.getValue(player);
        if(remembered >= 0 && !getBackpackStack(player, remembered).isEmpty())
            return remembered;

        if(!getBackpackStack(player, INDEX_CURIOS).isEmpty())
            return INDEX_CURIOS;
        if(!getBackpackStack(player, INDEX_CHEST).isEmpty())
            return INDEX_CHEST;

        return firstBrowsableBackpackIndex(player);
    }

    /**
     * Steps to the next/previous non-empty backpack index. When {@code fullScan} is true (the owner
     * is browsing their own backpacks), every inventory slot is considered; otherwise (e.g. a
     * pickpocketing session) only the equipped indices are considered, so a non-owner can never page
     * through the target's whole inventory.
     */
    public static int navigateBackpackIndex(Player player, int currentIndex, Navigate navigate, boolean fullScan)
    {
        int[] candidates = fullScan ? fullScanIndices(player) : EQUIPPED_INDICES;
        List<Integer> nonEmpty = new java.util.ArrayList<>();
        for(int index : candidates)
        {
            if(!getBackpackStack(player, index).isEmpty())
            {
                nonEmpty.add(index);
            }
        }
        if(nonEmpty.isEmpty())
            return currentIndex;

        int position = nonEmpty.indexOf(currentIndex);
        if(position == -1)
            return nonEmpty.get(0);

        int size = nonEmpty.size();
        int step = navigate == Navigate.NEXT ? 1 : -1;
        return nonEmpty.get(Math.floorMod(position + step, size));
    }

    private static int[] fullScanIndices(Player player)
    {
        int total = getTotalBackpackIndices(player);
        int[] indices = new int[total];
        for(int i = 0; i < total; i++)
        {
            indices[i] = i;
        }
        return indices;
    }

    private static int firstBrowsableBackpackIndex(Player player)
    {
        int total = getTotalBackpackIndices(player);
        for(int index = 0; index < total; index++)
        {
            if(!getBackpackStack(player, index).isEmpty())
                return index;
        }
        return -1;
    }

    /**
     * Finds the first backpack the player has equipped (chest or Curios back slot only). Used by
     * pickpocketing, where the opener must not be able to browse the target's full inventory.
     */
    public static int firstAvailableBackpackIndex(Player player)
    {
        for(int index : EQUIPPED_INDICES)
        {
            if(!getBackpackStack(player, index).isEmpty())
                return index;
        }
        return -1;
    }

    public static ItemStack getBackpackStack(Player player, int index)
    {
        if(index == INDEX_CHEST)
        {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            return stack.getItem() instanceof BackpackItem ? stack : ItemStack.EMPTY;
        }
        if(index == INDEX_CURIOS)
        {
            return Services.BACKPACK.getCuriosBackpackStack(player);
        }

        // Deliberately bounded by items.size() (the 36 main/hotbar slots), not getContainerSize()
        // (which also includes the 4 armor slots + offhand) - otherwise the chest slot's backpack
        // would be counted a second time here, since Inventory#getItem/#getContainerSize treat
        // armor/offhand as part of the same combined index range.
        int invIndex = index - INDEX_INVENTORY_START;
        Inventory inventory = player.getInventory();
        if(invIndex < 0 || invIndex >= inventory.items.size())
            return ItemStack.EMPTY;

        ItemStack stack = inventory.getItem(invIndex);
        return stack.getItem() instanceof BackpackItem ? stack : ItemStack.EMPTY;
    }

    public static boolean setBackpackStack(Player player, ItemStack stack, int index)
    {
        if(!stack.isEmpty() && !(stack.getItem() instanceof BackpackItem))
            return false;

        if(index == INDEX_CHEST)
        {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
            return true;
        }
        if(index == INDEX_CURIOS)
        {
            return Services.BACKPACK.setCuriosBackpackStack(player, stack);
        }

        int invIndex = index - INDEX_INVENTORY_START;
        Inventory inventory = player.getInventory();
        if(invIndex < 0 || invIndex >= inventory.items.size())
            return false;

        inventory.setItem(invIndex, stack);
        return true;
    }

    /**
     * @return the backpacks currently equipped (chest slot, then Curios back slot), for things like
     * death handling and grave-mod integration. Does not include backpacks merely carried in the
     * player's inventory.
     */
    public static NonNullList<ItemStack> getBackpacks(Player player)
    {
        NonNullList<ItemStack> list = NonNullList.create();
        for(int index : EQUIPPED_INDICES)
        {
            list.add(getBackpackStack(player, index));
        }
        return list;
    }

    public static UnlockableSlots getBackpackUnlockableSlots(Player player)
    {
        return UnlockableSlots.ALL;
    }

    public static UnlockableSlots unlockInitialSlots(UnlockableSlots slots, int initialCount)
    {
        return slots;
    }

    public static void setBackpackUnlockableSlots(Player player, UnlockableSlots slots)
    {
        // No-op: equip slots are no longer a purchasable/unlockable resource, there's exactly
        // one Curios "back" slot.
    }

    /**
     * Gets the first backpack the given player has equipped. If the player has no backpacks equipped,
     * this method will return an empty ItemStack. Keep in mind that this is not a copy, so changes
     * made to the ItemStack, like DataComponents, will be applied.
     *
     * @param player the player to get the backpack from
     * @return The ItemStack of the first equipped backpack, otherwise an empty ItemStack
     */
    public static ItemStack getFirstBackpackStack(Player player)
    {
        return getFirstBackpackStack(player, stack -> true);
    }

    /**
     * Gets the first backpack the given player has equipped and also matches the given filter. If
     * the player has no backpacks equipped, this method will return an empty ItemStack. Keep in
     * mind that this is not a copy, so changes made to the ItemStack, like DataComponents, will be
     * applied.
     *
     * @param player the player to get the backpack from
     * @param filter a predicate to test on the equipped backpack ItemStack
     * @return The ItemStack of the first equipped backpack, otherwise an empty ItemStack
     */
    public static ItemStack getFirstBackpackStack(Player player, Predicate<ItemStack> filter)
    {
        for(int index : EQUIPPED_INDICES)
        {
            ItemStack stack = getBackpackStack(player, index);
            if(!stack.isEmpty() && filter.test(stack))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Equips the given stack into the player's Curios back slot if Curios is installed and the slot
     * is empty. This only covers the Curios convenience slot - equipping into the vanilla chest slot
     * is handled by {@link BackpackItem#use} via the {@link net.minecraft.world.item.Equipable}
     * swap-with-equipment-slot flow, since that already correctly swaps out worn armor.
     *
     * @param player the player that is attempting to equip the stack
     * @param stack  the stack to equip to the player (must be a backpack item)
     * @return True if the backpack was successfully equipped into the Curios back slot
     */
    public static boolean equipBackpack(Player player, ItemStack stack)
    {
        if(!getBackpackStack(player, INDEX_CURIOS).isEmpty())
            return false;

        // Pass a copy so the original stack is only cleared once we know the equip actually succeeded
        if(setBackpackStack(player, stack.copy(), INDEX_CURIOS))
        {
            stack.shrink(stack.getCount());
            return true;
        }
        return false;
    }

    /**
     * Removes all equipped backpacks (chest slot and Curios back slot) from the player and returns a
     * NonNullList containing the removed backpacks. Backpacks merely carried in the inventory are not
     * touched, since they're dropped normally by vanilla like any other item.
     *
     * @param player the player to remove the backpacks from
     * @return a NonNullList of ItemStacks (which should be backpack items)
     */
    public static NonNullList<ItemStack> removeAllBackpacks(Player player)
    {
        NonNullList<ItemStack> removed = NonNullList.create();
        for(int index : EQUIPPED_INDICES)
        {
            ItemStack stack = getBackpackStack(player, index);
            setBackpackStack(player, ItemStack.EMPTY, index);
            removed.add(stack);
        }
        return removed;
    }

    /**
     * Creates pagination information used for displaying in the backpack inventory GUI's navigation
     * arrows. When {@code fullScan} is true (the owner browsing their own backpacks), every backpack
     * anywhere in the player's inventory is counted; otherwise only the equipped ones are (see
     * {@link #navigateBackpackIndex}).
     *
     * @param owner the player with the backpacks
     * @return A pagination with the current page and total pages
     */
    public static Pagination createPaginationInfo(Player owner, int backpackIndex, boolean fullScan)
    {
        int[] candidates = fullScan ? fullScanIndices(owner) : EQUIPPED_INDICES;
        int total = 0;
        int page = 0;
        for(int index : candidates)
        {
            if(!getBackpackStack(owner, index).isEmpty())
            {
                total++;
                if(index == backpackIndex)
                {
                    page = total;
                }
            }
        }
        return new Pagination(page, total);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends Augment<T>> T findAugment(ItemStack stack, AugmentType<T> type)
    {
        if(Config.getDisabledAugments().contains(type.id()))
            return null;
        Augments augments = Augments.get(stack);
        UnlockableSlots bays = getUnlockableAugmentBays(stack);
        if(bays.isUnlocked(0) && augments.firstState() && augments.firstAugment().type() == type)
            return (T) augments.firstAugment();
        if(bays.isUnlocked(1) && augments.secondState() && augments.secondAugment().type() == type)
            return (T) augments.secondAugment();
        if(bays.isUnlocked(2) && augments.thirdState() && augments.thirdAugment().type() == type)
            return (T) augments.thirdAugment();
        if(bays.isUnlocked(3) && augments.fourthState() && augments.fourthAugment().type() == type)
            return (T) augments.fourthAugment();
        return null;
    }

    public static UnlockableSlots getUnlockableAugmentBays(ItemStack stack)
    {
        if(stack.getItem() instanceof BackpackItem item)
        {
            return item.getUnlockableAugmentBays(stack);
        }
        return UnlockableSlots.NONE;
    }

    public static <T extends Augment<T>> List<InventoryAugmentSnapshot.One<T>> getBackpackInventoriesWithAugment(Player player, AugmentType<T> type)
    {
        BackpackedInventoryAccess access = (BackpackedInventoryAccess) player;
        return access.backpacked$streamNonNullBackpackInventories().map(inventory -> {
            T augment = findAugment(inventory.getBackpackStack(), type);
            return new InventoryAugmentSnapshot.One<>(inventory, augment);
        }).filter(result -> Objects.nonNull(result.augment())).toList();
    }

    public static <T extends Augment<T>, R extends Augment<R>> List<InventoryAugmentSnapshot.Two<T, R>> getBackpackInventoriesWithAugment(Player player, AugmentType<T> firstType, AugmentType<R> secondType)
    {
        BackpackedInventoryAccess access = (BackpackedInventoryAccess) player;
        return access.backpacked$streamNonNullBackpackInventories().map(inventory -> {
            ItemStack stack = inventory.getBackpackStack();
            T firstAugment = findAugment(stack, firstType);
            R secondAugment = findAugment(stack, secondType);
            if(firstAugment == null || secondAugment == null)
                return null;
            return new InventoryAugmentSnapshot.Two<>(inventory, firstAugment, secondAugment);
        }).filter(Objects::nonNull).toList();
    }

    public static boolean isCosmeticDisabled(ResourceLocation id)
    {
        return Config.BACKPACK.cosmetics.disabledCosmetics.get().contains(id.toString());
    }
}

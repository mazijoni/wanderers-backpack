package com.mrcrayfish.backpacked.inventory;

import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.augment.impl.BedAugment;
import com.mrcrayfish.backpacked.common.augment.impl.EmptyAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FarmhandAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FluidTankAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FunnellingAugment;
import com.mrcrayfish.backpacked.common.augment.impl.GiantAugment;
import com.mrcrayfish.backpacked.common.augment.impl.HopperBridgeAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ImbuedHideAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ImmortalAugment;
import com.mrcrayfish.backpacked.common.augment.impl.LightweaverAugment;
import com.mrcrayfish.backpacked.common.augment.impl.LootboundAugment;
import com.mrcrayfish.backpacked.common.augment.impl.QuiverlinkAugment;
import com.mrcrayfish.backpacked.common.augment.impl.RecallAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ReforgeAugment;
import com.mrcrayfish.backpacked.common.augment.impl.SeedflowAugment;
import com.mrcrayfish.backpacked.core.ModItems;
import com.mrcrayfish.backpacked.item.AugmentItem;
import com.mrcrayfish.backpacked.item.BackpackItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Bridges the physical augment slots shown in the backpack GUI (3, or 4 for backpacks whose
 * {@link BackpackItem#getMaxAugmentBays} returns 4) to Backpacked's existing {@link Augments} data
 * component (first/second/third/fourth position). Placing an AugmentItem into a slot installs that
 * augment's default settings; removing it clears the slot back to {@link EmptyAugment}. This lets
 * the rest of the mod's augment machinery (AugmentHandler, BackpackHelper#findAugment, etc.) keep
 * working completely unchanged.
 */
public class BackpackAugmentContainer implements Container
{
    private final ItemStack backpackStack;
    private final int size;

    public BackpackAugmentContainer(ItemStack backpackStack)
    {
        this.backpackStack = backpackStack;
        this.size = backpackStack.getItem() instanceof BackpackItem item ? item.getMaxAugmentBays(backpackStack) : 3;
    }

    private Augments getAugments()
    {
        return Augments.get(this.backpackStack);
    }

    private void setAugments(Augments augments)
    {
        if(!this.backpackStack.isEmpty())
        {
            Augments.set(this.backpackStack, augments);
        }
    }

    @Override
    public int getContainerSize()
    {
        return this.size;
    }

    @Override
    public boolean isEmpty()
    {
        Augments augments = this.getAugments();
        return augments.firstAugment() instanceof EmptyAugment
            && augments.secondAugment() instanceof EmptyAugment
            && augments.thirdAugment() instanceof EmptyAugment
            && augments.fourthAugment() instanceof EmptyAugment;
    }

    @Override
    public ItemStack getItem(int index)
    {
        Augment<?> augment = switch(index)
        {
            case 0 -> this.getAugments().firstAugment();
            case 1 -> this.getAugments().secondAugment();
            case 2 -> this.getAugments().thirdAugment();
            case 3 -> this.getAugments().fourthAugment();
            default -> EmptyAugment.INSTANCE;
        };
        return toItemStack(augment);
    }

    @Override
    public ItemStack removeItem(int index, int count)
    {
        ItemStack stack = this.getItem(index);
        this.setItem(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    {
        return this.removeItem(index, 1);
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        Augment<?> augment = stack.getItem() instanceof AugmentItem item ? item.createDefaultAugment() : EmptyAugment.INSTANCE;
        Augments augments = this.getAugments();
        augments = switch(index)
        {
            case 0 -> augments.setAugment(Augments.Position.FIRST, augment).setState(Augments.Position.FIRST, true);
            case 1 -> augments.setAugment(Augments.Position.SECOND, augment).setState(Augments.Position.SECOND, true);
            case 2 -> augments.setAugment(Augments.Position.THIRD, augment).setState(Augments.Position.THIRD, true);
            case 3 -> augments.setAugment(Augments.Position.FOURTH, augment).setState(Augments.Position.FOURTH, true);
            default -> augments;
        };
        this.setAugments(augments);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        if(!(stack.getItem() instanceof AugmentItem item))
            return false;
        return !this.hasAugmentTypeElsewhere(index, item.getAugmentType());
    }

    /**
     * Only one of each augment type may be installed on a backpack at a time, regardless of which
     * bay it occupies.
     */
    private boolean hasAugmentTypeElsewhere(int index, AugmentType<?> type)
    {
        for(int i = 0; i < this.getContainerSize(); i++)
        {
            if(i == index)
                continue;
            if(this.getItem(i).getItem() instanceof AugmentItem other && other.getAugmentType() == type)
                return true;
        }
        return false;
    }

    @Override
    public void setChanged()
    {
    }

    @Override
    public boolean stillValid(Player player)
    {
        return !this.backpackStack.isEmpty();
    }

    @Override
    public void clearContent()
    {
        for(int i = 0; i < this.getContainerSize(); i++)
        {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    private static ItemStack toItemStack(Augment<?> augment)
    {
        if(augment instanceof FunnellingAugment) return new ItemStack(ModItems.FUNNELLING_AUGMENT.get());
        if(augment instanceof ImbuedHideAugment) return new ItemStack(ModItems.IMBUED_HIDE_AUGMENT.get());
        if(augment instanceof ImmortalAugment) return new ItemStack(ModItems.IMMORTAL_AUGMENT.get());
        if(augment instanceof ReforgeAugment) return new ItemStack(ModItems.REFORGE_AUGMENT.get());
        if(augment instanceof LightweaverAugment) return new ItemStack(ModItems.LIGHTWEAVER_AUGMENT.get());
        if(augment instanceof FarmhandAugment) return new ItemStack(ModItems.FARMHAND_AUGMENT.get());
        if(augment instanceof QuiverlinkAugment) return new ItemStack(ModItems.QUIVERLINK_AUGMENT.get());
        if(augment instanceof LootboundAugment) return new ItemStack(ModItems.LOOTBOUND_AUGMENT.get());
        if(augment instanceof SeedflowAugment) return new ItemStack(ModItems.SEEDFLOW_AUGMENT.get());
        if(augment instanceof HopperBridgeAugment) return new ItemStack(ModItems.HOPPER_BRIDGE_AUGMENT.get());
        if(augment instanceof GiantAugment) return new ItemStack(ModItems.GIANT_AUGMENT.get());
        if(augment instanceof RecallAugment) return new ItemStack(ModItems.RECALL_AUGMENT.get());
        if(augment instanceof BedAugment) return new ItemStack(ModItems.BED_AUGMENT.get());
        if(augment instanceof FluidTankAugment) return new ItemStack(ModItems.FLUID_TANK_AUGMENT.get());
        return ItemStack.EMPTY;
    }
}

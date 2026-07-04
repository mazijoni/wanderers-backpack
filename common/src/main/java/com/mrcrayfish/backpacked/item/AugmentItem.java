package com.mrcrayfish.backpacked.item;

import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A physical item version of an augment - placing one of these into an augment slot installs that
 * augment (with its default settings) onto the backpack. Replaces Backpacked's original
 * click-a-button-then-pick-from-a-popup-menu augment installation flow.
 */
public class AugmentItem extends Item
{
    private final AugmentType<?> augmentType;

    public AugmentItem(AugmentType<?> augmentType, Properties properties)
    {
        super(properties);
        this.augmentType = augmentType;
    }

    public AugmentType<?> getAugmentType()
    {
        return this.augmentType;
    }

    public Augment<?> createDefaultAugment()
    {
        return this.augmentType.defaultSupplier().get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(this.augmentType.description().copy().withStyle(ChatFormatting.GRAY));
    }
}

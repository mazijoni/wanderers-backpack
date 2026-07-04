package com.mrcrayfish.backpacked.common.backpack;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Carries a backpack's stored items so its tooltip can render them as a grid of item icons
 * (see BackpackContentsTooltipRenderer) instead of text, while holding Shift.
 */
public record BackpackContentsTooltip(List<ItemStack> items) implements TooltipComponent
{
}

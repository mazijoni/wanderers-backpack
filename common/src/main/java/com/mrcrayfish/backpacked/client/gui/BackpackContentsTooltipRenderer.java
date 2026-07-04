package com.mrcrayfish.backpacked.client.gui;

import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen;
import com.mrcrayfish.backpacked.common.backpack.BackpackContentsTooltip;
import com.mrcrayfish.backpacked.util.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Renders a backpack's stored items as a grid of item icons while Shift is held, styled to match
 * the backpack's own GUI (the same label panel + slot sprites used behind the augment slots)
 * instead of vanilla's bundle tooltip. When Shift isn't held, this reports zero size so it takes
 * no space in the tooltip and draws nothing.
 */
public class BackpackContentsTooltipRenderer implements ClientTooltipComponent
{
    private static final ResourceLocation LABEL_BACKGROUND = Utils.rl("backpack/label");
    private static final ResourceLocation SLOT = Utils.rl("backpack/slot");
    private static final int LABEL_PADDING = BackpackScreen.LABEL_PADDING;
    private static final int MAX_COLUMNS = 9;

    private final List<ItemStack> items;

    public BackpackContentsTooltipRenderer(BackpackContentsTooltip tooltip)
    {
        this.items = tooltip.items();
    }

    private boolean shouldShow()
    {
        return Screen.hasShiftDown();
    }

    private int gridSizeX()
    {
        return Math.max(1, Math.min(MAX_COLUMNS, (int) Math.ceil(Math.sqrt(this.items.size()))));
    }

    private int gridSizeY()
    {
        return Math.max(1, (int) Math.ceil(this.items.size() / (double) this.gridSizeX()));
    }

    private int gridWidth()
    {
        return this.gridSizeX() * 18;
    }

    private int gridHeight()
    {
        return this.gridSizeY() * 18;
    }

    @Override
    public int getHeight()
    {
        return this.shouldShow() ? this.gridHeight() + LABEL_PADDING * 2 : 0;
    }

    @Override
    public int getWidth(Font font)
    {
        return this.shouldShow() ? this.gridWidth() + LABEL_PADDING * 2 : 0;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics)
    {
        if(!this.shouldShow())
            return;

        int columns = this.gridSizeX();
        int rows = this.gridSizeY();
        int gridX = x + LABEL_PADDING;
        int gridY = y + LABEL_PADDING;

        graphics.blitSprite(LABEL_BACKGROUND, x, y, this.gridWidth() + LABEL_PADDING * 2, this.gridHeight() + LABEL_PADDING * 2);
        graphics.blitSprite(SLOT, gridX, gridY, this.gridWidth(), this.gridHeight());

        int index = 0;
        for(int row = 0; row < rows; row++)
        {
            for(int col = 0; col < columns; col++)
            {
                if(index >= this.items.size())
                    break;

                int slotX = gridX + col * 18 + 1;
                int slotY = gridY + row * 18 + 1;
                ItemStack stack = this.items.get(index);
                graphics.renderItem(stack, slotX, slotY, index);
                graphics.renderItemDecorations(font, stack, slotX, slotY);
                index++;
            }
        }
    }
}

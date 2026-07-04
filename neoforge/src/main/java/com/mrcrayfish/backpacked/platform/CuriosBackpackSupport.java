package com.mrcrayfish.backpacked.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Isolated so that {@link top.theillusivec4.curios.api.CuriosApi} is only ever touched (and
 * class-loaded) when Curios is actually installed. Only call these from behind a
 * {@code ModList.get().isLoaded("curios")} check.
 */
final class CuriosBackpackSupport
{
    private CuriosBackpackSupport()
    {
    }

    static ItemStack getStack(Player player)
    {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler("back"))
                .map(stacksHandler -> stacksHandler.getStacks().getStackInSlot(0))
                .orElse(ItemStack.EMPTY);
    }

    static boolean setStack(Player player, ItemStack stack)
    {
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            handler.setEquippedCurio("back", 0, stack);
            return true;
        }).orElse(false);
    }
}

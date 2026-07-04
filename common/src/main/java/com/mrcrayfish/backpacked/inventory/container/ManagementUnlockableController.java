package com.mrcrayfish.backpacked.inventory.container;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.Config;
import com.mrcrayfish.backpacked.common.CostModel;
import com.mrcrayfish.backpacked.common.PaymentItem;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * Shared unlockable-slot controller for the small "which equipped backpack" pickers (currently only
 * the shelf's storage menu - see {@link BackpackShelfMenu}).
 *
 * Author: MrCrayfish
 */
public class ManagementUnlockableController extends UnlockableController
{
    private final List<Container> paymentContainers;

    public ManagementUnlockableController(UnlockableSlots slots, List<Container> paymentContainers)
    {
        super(slots);
        this.paymentContainers = paymentContainers;
    }

    @Override
    public Optional<UnlockableSlots> getSlots(Player player)
    {
        return Optional.of(BackpackHelper.getBackpackUnlockableSlots(player));
    }

    @Override
    public void setSlots(Player player, UnlockableSlots slots)
    {
        BackpackHelper.setBackpackUnlockableSlots(player, slots);
    }

    @Override
    public CostModel getCostModel()
    {
        return Config.BACKPACK.equipable.unlockCost;
    }

    @Override
    public PaymentItem getPaymentItem()
    {
        return Config.getBackpackPaymentItem();
    }

    @Override
    public List<Container> getPaymentContainers()
    {
        return this.paymentContainers;
    }

    @Override
    public boolean allowsUnlockToken()
    {
        return Config.BACKPACK.equipable.allowUnlockingUsingUnlockToken.get();
    }
}

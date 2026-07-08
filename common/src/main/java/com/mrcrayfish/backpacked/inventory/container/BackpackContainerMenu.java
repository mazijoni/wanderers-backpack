package com.mrcrayfish.backpacked.inventory.container;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.Config;
import com.mrcrayfish.backpacked.common.CostModel;
import com.mrcrayfish.backpacked.common.PaymentItem;
import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.augment.impl.FluidTankAugment;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import com.mrcrayfish.backpacked.core.ModContainers;
import com.mrcrayfish.backpacked.core.ModDataComponents;
import com.mrcrayfish.backpacked.inventory.BackpackAugmentContainer;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import com.mrcrayfish.backpacked.inventory.ShelfBackpackContainer;
import com.mrcrayfish.backpacked.inventory.container.data.BackpackContainerData;
import com.mrcrayfish.backpacked.inventory.container.slot.BackpackSlot;
import com.mrcrayfish.backpacked.inventory.container.slot.UnlockableSlot;
import com.mrcrayfish.backpacked.item.AugmentItem;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.network.Network;
import com.mrcrayfish.backpacked.network.message.MessageSyncAugmentChange;
import com.mrcrayfish.backpacked.platform.Services;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class BackpackContainerMenu extends CustomContainerMenu
{
    // There is a technical hard limit of 256, these values allow the widest and tallest inventory possible
    public static final int MAX_COLUMNS = 23;
    public static final int MAX_ROWS = 11;

    public static final int AUGMENT_SLOT_GAP = 2;
    public static final int AUGMENT_PANEL_X = -34;
    public static final int AUGMENT_BUTTONS_X = AUGMENT_PANEL_X + 18 + 2;

    private final Container backpackInventory;
    private final Container augmentInventory;
    private final ItemStack backpackStack;
    private final Player menuPlayer;
    private final int ownerId;
    private final int backpackIndex;
    private final int cols;
    private final int rows;
    private final boolean owner;
    private final int augmentSlots;
    private final Pagination pagination;
    // A locally-cached snapshot of the backpack's Augments (settings + on/off state), used by the
    // toggle/config buttons next to the augment slots. Which AugmentItem physically occupies a
    // slot is tracked separately via BackpackAugmentContainer/the slot's ItemStack - this cache is
    // only for settings and enabled-state, refreshed at open time and via MessageSyncAugmentChange.
    private Augments augments;

    public BackpackContainerMenu(int id, Inventory playerInventory, BackpackContainerData data)
    {
        this(id, playerInventory, new SimpleContainer(Mth.clamp(data.columns(), 1, MAX_COLUMNS) * Mth.clamp(data.rows(), 1, MAX_ROWS)), new SimpleContainer(data.augmentSlots()), -1, data.backpackIndex(), data.columns(), data.rows(), data.owner(), data.slots(), data.pagination(), data.augments(), data.bays());
    }

    public BackpackContainerMenu(int id, Inventory playerInventory, Container backpackContainer, int ownerId, int backpackIndex, int cols, int rows, boolean owner, UnlockableSlots slots, Pagination pagination, Augments augments, UnlockableSlots bays)
    {
        this(id, playerInventory, backpackContainer, new BackpackAugmentContainer(backpackContainer instanceof BackpackInventory inv ? inv.getBackpackStack() : ItemStack.EMPTY), ownerId, backpackIndex, cols, rows, owner, slots, pagination, augments, bays);
    }

    private BackpackContainerMenu(int id, Inventory playerInventory, Container backpackContainer, Container augmentContainer, int ownerId, int backpackIndex, int cols, int rows, boolean owner, UnlockableSlots slots, Pagination pagination, Augments augments, UnlockableSlots bays)
    {
        super(ModContainers.BACKPACK.get(), id);
        this.backpackInventory = backpackContainer;
        this.augmentInventory = augmentContainer;
        this.menuPlayer = playerInventory.player;
        this.ownerId = ownerId;
        this.backpackIndex = backpackIndex;
        this.cols = Mth.clamp(cols, 1, MAX_COLUMNS);
        this.rows = Mth.clamp(rows, 1, MAX_ROWS);
        this.owner = owner;
        this.augmentSlots = augmentContainer.getContainerSize();
        this.pagination = pagination;
        this.augments = augments;
        this.backpackStack = backpackContainer instanceof BackpackInventory inv ? inv.getBackpackStack() : ItemStack.EMPTY;

        checkContainerSize(backpackContainer, this.cols * this.rows);
        backpackContainer.startOpen(playerInventory.player);

        int backpackWidth = 11 + Math.max(9 * 18, this.cols * 18) + 11;
        int backpackSlotsWidth = this.cols * 18;
        int backpackSlotsX = Math.max((backpackWidth - backpackSlotsWidth) / 2, 0) + 1;
        int backpackSlotsY = 28;
        // The Fluid Tank augment's gauge takes over the grid's rightmost column visually
        // (BackpackScreen draws it over that space), but every BackpackSlot is still added here
        // regardless - the total slot count must always stay cols*rows so every other index-based
        // calculation in this class and BackpackScreen (augment slot indices, quickMoveStack, JEI
        // areas, etc.) keeps working. Instead, that column's slots simply reject new items while
        // the augment is active (see reserveFluidColumn below) - existing items already there stay
        // fully visible and can still be taken out, just not added to.
        boolean reserveFluidColumn = this.owner && BackpackHelper.findAugment(this.backpackStack, ModAugmentTypes.FLUID_TANK.get()) != null;
        for(int y = 0; y < rows; y++)
        {
            for(int x = 0; x < cols; x++)
            {
                boolean reserved = reserveFluidColumn && x == cols - 1;
                this.addSlot(reserved
                    ? new BackpackSlot(backpackContainer, x + y * cols, backpackSlotsX + x * 18, backpackSlotsY + y * 18)
                    {
                        @Override
                        public boolean mayPlace(ItemStack stack)
                        {
                            return false;
                        }
                    }
                    : new BackpackSlot(backpackContainer, x + y * cols, backpackSlotsX + x * 18, backpackSlotsY + y * 18));
            }
        }

        // Only the backpack owner gets augment slots - a Wandering Trader's bag (owner=false) has
        // no augments to configure, so it must not gain phantom interactable slots.
        if(this.owner)
        {
            UnlockableController augmentController = new AugmentBayUnlockableController(bays, this.backpackStack, List.of(playerInventory));

            int backpackHeight = 11 + this.rows * 18 + 14;
            int augmentContentHeight = this.augmentSlots * 18 + (this.augmentSlots - 1) * AUGMENT_SLOT_GAP;
            int augmentY = 16 + (backpackHeight - augmentContentHeight) / 2;
            for(int i = 0; i < this.augmentSlots; i++)
            {
                // +1/+1 so the item renders inset within the 18x18 slot frame texture, matching
                // every other slot in this screen (whose backing texture is drawn 1px up-left of
                // the slot's item position).
                int slotIndex = i;
                int y = augmentY + i * (18 + AUGMENT_SLOT_GAP);
                this.addSlot(new UnlockableSlot(augmentController, augmentContainer, i, AUGMENT_PANEL_X + 1, y + 1)
                    .setPredicate(stack -> augmentContainer.canPlaceItem(slotIndex, stack)));
            }
        }

        int inventorySlotsWidth = 9 * 18;
        int inventorySlotsX = Math.max((backpackWidth - inventorySlotsWidth) / 2, 0) + 1;
        int inventorySlotsY = 26 + this.rows * 18 + 15 + 3 + 19;
        this.addPlayerInventorySlots(playerInventory, inventorySlotsX, inventorySlotsY);
    }

    public Container getBackpackInventory()
    {
        return this.backpackInventory;
    }

    public int getOwnerId()
    {
        return ownerId;
    }

    public int getBackpackIndex()
    {
        return this.backpackIndex;
    }

    public int getCols()
    {
        return this.cols;
    }

    public int getRows()
    {
        return this.rows;
    }

    public boolean isOwner()
    {
        return this.owner;
    }

    public int getAugmentSlots()
    {
        return this.augmentSlots;
    }

    public Pagination getPagination()
    {
        return this.pagination;
    }

    public Augments getAugments()
    {
        return this.augments;
    }

    public void setAugments(Augments augments)
    {
        this.augments = augments;
    }

    public int getFluidCapacity()
    {
        return FluidTankAugment.capacity(this.rows);
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        return this.backpackInventory.stillValid(playerIn);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index)
    {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int backpackSlots = this.rows * this.cols;
        int augmentEnd = backpackSlots + (this.owner ? this.augmentSlots : 0);

        if(slot.hasItem())
        {
            ItemStack slotStack = slot.getItem();
            copy = slotStack.copy();
            if(index < augmentEnd)
            {
                if(!this.moveItemStackTo(slotStack, augmentEnd, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if(slotStack.getItem() instanceof AugmentItem && this.moveItemStackTo(slotStack, backpackSlots, augmentEnd, false))
            {
                // moved into an augment slot
            }
            else if(!this.moveItemStackTo(slotStack, 0, backpackSlots, false))
            {
                return ItemStack.EMPTY;
            }

            if(slotStack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        // Equipped BackpackInventory instances are ticked/saved every player tick regardless of
        // whether a menu is even open (see PlayerMixin), but a shelved backpack's container only
        // exists while this menu is - so it needs its own tick call, driven from here since this is
        // already invoked once per server tick for every open menu.
        if(this.backpackInventory instanceof ShelfBackpackContainer shelfContainer)
        {
            shelfContainer.tick();
        }

        // Placing/removing a physical AugmentItem changes the backpack's Augments record (via
        // BackpackAugmentContainer) without going through the normal slot-sync packets, since that
        // change lives on a *different* ItemStack (the backpack itself). Detect and push it here so
        // the toggle/config buttons don't go stale after a swap.
        if(this.owner && this.menuPlayer instanceof ServerPlayer serverPlayer && this.backpackInventory instanceof BackpackInventory inventory)
        {
            Augments fresh = Augments.get(inventory.getBackpackStack());
            if(!fresh.equals(this.augments))
            {
                Augments previous = this.augments;
                this.augments = fresh;
                for(Augments.Position position : Augments.Position.values())
                {
                    if(!fresh.getAugment(position).equals(previous.getAugment(position)) || fresh.getState(position) != previous.getState(position))
                    {
                        Network.getPlay().sendToPlayer(() -> serverPlayer, new MessageSyncAugmentChange(position, fresh.getAugment(position), fresh.getState(position)));
                    }
                }
            }
        }
    }

    @Override
    public void removed(Player playerIn)
    {
        super.removed(playerIn);
        if(this.backpackInventory instanceof ShelfBackpackContainer shelfContainer)
        {
            shelfContainer.saveItemsToStack();
        }
        this.backpackInventory.stopOpen(playerIn);
    }

    /**
     * Fills/drains the Fluid Tank augment directly from whatever the player is currently holding
     * on their cursor in this menu - no intermediate slot is ever placeable, the bucket swap
     * happens instantly server-side in response to a click anywhere in the gauge column (see
     * MessageInteractFluidTank/BackpackScreen). A held empty bucket is swapped for a full one
     * (draining {@link FluidTankAugment#BUCKET_AMOUNT} out of the tank) if the tank has enough of
     * a fluid with a registered bucket item; a held filled bucket (of any fluid with a registered
     * bucket item, vanilla or modded) is swapped for an empty one (filling the tank) if it accepts
     * that fluid and has the spare capacity. Stacks of more than 1 are left untouched since a
     * cursor stack can't be partially swapped.
     */
    public void interactFluidTank(ServerPlayer player)
    {
        if(this.backpackStack.isEmpty())
            return;

        FluidTankAugment tank = BackpackHelper.findAugment(this.backpackStack, ModAugmentTypes.FLUID_TANK.get());
        if(tank == null)
            return;

        ItemStack carried = this.getCarried();
        if(carried.getCount() != 1 || !(carried.getItem() instanceof BucketItem))
            return;

        if(carried.is(Items.BUCKET))
        {
            if(tank.fluid().isEmpty() || tank.amount() < FluidTankAugment.BUCKET_AMOUNT)
                return;
            Optional<Item> filledBucket = Services.PLATFORM.getBucketForFluid(tank.fluid().get());
            filledBucket.ifPresent(item -> {
                this.setCarried(new ItemStack(item));
                this.setFluidTankAugment(tank.drain(FluidTankAugment.BUCKET_AMOUNT));
            });
            return;
        }

        Optional<ResourceKey<Fluid>> fluid = Services.PLATFORM.getFluidFromBucket(carried);
        if(fluid.isPresent() && tank.canAccept(fluid.get(), FluidTankAugment.BUCKET_AMOUNT, this.getFluidCapacity()))
        {
            this.setCarried(new ItemStack(Items.BUCKET));
            this.setFluidTankAugment(tank.fill(fluid.get(), FluidTankAugment.BUCKET_AMOUNT));
        }
    }

    private <T extends Augment<T>> void setFluidTankAugment(T updated)
    {
        AugmentType<T> type = updated.type();
        Augments current = Augments.get(this.backpackStack);
        for(Augments.Position position : Augments.Position.values())
        {
            if(current.getAugment(position).type() == type)
            {
                Augments.set(this.backpackStack, current.setAugment(position, updated));
                return;
            }
        }
    }

    /**
     * Gates the 3 physical augment slots behind the same experience-level unlock flow used
     * elsewhere in the mod (backpack storage slots, equippable backpack slots). Unlike those,
     * the unlocked state lives on the specific backpack ItemStack (see {@link BackpackItem#getUnlockableAugmentBays}),
     * not per-player, since augment bays belong to a particular backpack instance.
     */
    public static class AugmentBayUnlockableController extends UnlockableController
    {
        private final ItemStack backpackStack;
        private final List<Container> paymentContainers;

        public AugmentBayUnlockableController(UnlockableSlots bays, ItemStack backpackStack, List<Container> paymentContainers)
        {
            super(bays);
            this.backpackStack = backpackStack;
            this.paymentContainers = paymentContainers;
        }

        @Override
        public Optional<UnlockableSlots> getSlots(Player player)
        {
            if(!(this.backpackStack.getItem() instanceof BackpackItem item))
                return Optional.empty();
            return Optional.ofNullable(item.getUnlockableAugmentBays(this.backpackStack));
        }

        @Override
        public void setSlots(Player player, UnlockableSlots slots)
        {
            if(!this.backpackStack.isEmpty())
            {
                this.backpackStack.set(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get(), slots);
            }
        }

        @Override
        public CostModel getCostModel()
        {
            return Config.BACKPACK.augmentBays.unlockCost;
        }

        @Override
        public PaymentItem getPaymentItem()
        {
            return Config.getAugmentBayPaymentItem();
        }

        @Override
        public List<Container> getPaymentContainers()
        {
            return this.paymentContainers;
        }

        @Override
        public boolean allowsUnlockToken()
        {
            return Config.BACKPACK.augmentBays.allowUnlockingUsingUnlockToken.get();
        }
    }
}

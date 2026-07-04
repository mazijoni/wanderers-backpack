package com.mrcrayfish.backpacked.item;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.Config;
import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.backpack.BackpackContentsTooltip;
import com.mrcrayfish.backpacked.common.backpack.CosmeticProperties;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.core.ModDataComponents;
import com.mrcrayfish.backpacked.core.ModSyncedDataKeys;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import com.mrcrayfish.backpacked.inventory.BackpackedInventoryAccess;
import com.mrcrayfish.backpacked.network.Network;
import com.mrcrayfish.backpacked.network.message.MessageShowEquipHint;
import com.mrcrayfish.backpacked.platform.Services;
import com.mrcrayfish.backpacked.util.ScreenUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class BackpackItem extends Item implements Equipable
{
    public static final Component BACKPACK_TRANSLATION = Component.translatable("container.backpack");
    public static final Component NO_MORE_BACKPACK_SLOTS_TRANSLATION = Component.translatable("wanderersbackpack.gui.no_more_backpack_slots");

    private final int columns;
    private final int rows;
    private final int maxAugmentBays;

    public BackpackItem(Properties properties, int columns, int rows)
    {
        this(properties, columns, rows, 3);
    }

    public BackpackItem(Properties properties, int columns, int rows, int maxAugmentBays)
    {
        super(properties
            .component(ModDataComponents.COSMETIC_PROPERTIES.get(), CosmeticProperties.DEFAULT)
            .component(ModDataComponents.UNLOCKABLE_SLOTS.get(), new UnlockableSlots(0))
            .component(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get(), new UnlockableSlots(0))
            .component(ModDataComponents.AUGMENTS.get(), Augments.EMPTY)
        );
        this.columns = columns;
        this.rows = rows;
        this.maxAugmentBays = maxAugmentBays;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if(!level.isClientSide())
        {
            // Prefer the Curios back slot (if installed) so it doesn't compete with worn armor
            if(BackpackHelper.equipBackpack(player, stack))
            {
                Network.getPlay().sendToPlayer(() -> (ServerPlayer) player, new MessageShowEquipHint());
                level.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), player.getSoundSource(), 1.0F, 1.0F, player.getRandom().nextLong());
                return InteractionResultHolder.success(stack);
            }

            // Otherwise fall back to the vanilla chest armor slot, swapping out any worn chestplate.
            // The equip sound is played automatically by LivingEntity.onEquipItem via getEquipSound().
            InteractionResultHolder<ItemStack> swapResult = this.swapWithEquipmentSlot(this, level, player, hand);
            if(swapResult.getResult().consumesAction())
            {
                Network.getPlay().sendToPlayer(() -> (ServerPlayer) player, new MessageShowEquipHint());
                return swapResult;
            }

            player.displayClientMessage(NO_MORE_BACKPACK_SLOTS_TRANSLATION, true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public EquipmentSlot getEquipmentSlot()
    {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound()
    {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    public int getColumnCount()
    {
        return this.columns;
    }

    public int getRowCount()
    {
        return this.rows;
    }

    public int getMaxAugmentBays(ItemStack stack)
    {
        return this.maxAugmentBays;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
    {
        List<ItemStack> items = getStoredItems(stack);
        if(items.isEmpty())
            return Optional.empty();

        return Optional.of(new BackpackContentsTooltip(items));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        if(!getStoredItems(stack).isEmpty())
        {
            tooltip.add(Component.translatable("wanderersbackpack.gui.hold_to_preview_contents", ScreenUtil.getShiftIcon()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static List<ItemStack> getStoredItems(ItemStack stack)
    {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if(contents == null)
            return List.of();

        List<ItemStack> items = new ArrayList<>();
        contents.nonEmptyItemsCopy().forEach(items::add);
        return items;
    }

    @Override
    public boolean canFitInsideContainerItems()
    {
        return false;
    }

    public static boolean openBackpack(ServerPlayer ownerPlayer, ServerPlayer openingPlayer, int backpackIndex)
    {
        BackpackInventory inventory = ((BackpackedInventoryAccess) ownerPlayer).backpacked$GetBackpackInventory(backpackIndex);
        if(inventory != null)
        {
            ItemStack backpack = inventory.getBackpackStack();
            if(!(backpack.getItem() instanceof BackpackItem item))
                return false;

            // Remember last opened backpack index
            if(Objects.equals(ownerPlayer, openingPlayer))
            {
                ModSyncedDataKeys.SELECTED_BACKPACK.setValue(ownerPlayer, backpackIndex);
            }

            Component title = backpack.has(DataComponents.CUSTOM_NAME) ? backpack.getHoverName() : BACKPACK_TRANSLATION;
            int cols = item.getColumnCount();
            int rows = item.getRowCount();
            boolean owner = ownerPlayer.equals(openingPlayer);
            UnlockableSlots slots = item.getUnlockableSlots(backpack);
            Pagination pagination = BackpackHelper.createPaginationInfo(ownerPlayer, backpackIndex, owner);
            Augments augments = Augments.get(backpack);
            UnlockableSlots bays = item.getUnlockableAugmentBays(backpack);
            int augmentSlots = item.getMaxAugmentBays(backpack);
            Services.BACKPACK.openBackpackScreen(openingPlayer, inventory, ownerPlayer.getId(), backpackIndex, cols, rows, owner, slots, pagination, augments, title, bays, augmentSlots);
            return true;
        }
        return false;
    }

    @Nullable
    public UnlockableSlots getUnlockableSlots(ItemStack stack)
    {
        if(!stack.is(this))
            return null;

        if(Config.BACKPACK.inventory.slots.unlockAllSlots.get())
            return UnlockableSlots.ALL;

        // If missing, create the component
        UnlockableSlots slots = stack.get(ModDataComponents.UNLOCKABLE_SLOTS.get());
        if(slots == null)
        {
            slots = new UnlockableSlots(this.getColumnCount() * this.getRowCount());
            stack.set(ModDataComponents.UNLOCKABLE_SLOTS.get(), slots);
            return slots;
        }

        // Update the max slots if the size is different
        int maxSlots = this.getColumnCount() * this.getRowCount();
        if(slots.getMaxSlots() != maxSlots)
        {
            slots = slots.setMaxSlots(maxSlots);
            stack.set(ModDataComponents.UNLOCKABLE_SLOTS.get(), slots);
        }

        int initialUnlocked = Config.BACKPACK.inventory.slots.initialUnlockedSlots.get();
        UnlockableSlots before = slots;
        slots = BackpackHelper.unlockInitialSlots(slots, initialUnlocked);
        if(before != slots)
        {
            stack.set(ModDataComponents.UNLOCKABLE_SLOTS.get(), slots);
        }

        return slots;
    }

    @Nullable
    public UnlockableSlots getUnlockableAugmentBays(ItemStack stack)
    {
        if(!stack.is(this))
            return null;

        if(Config.BACKPACK.augmentBays.unlockAllAugmentBays.get())
            return UnlockableSlots.ALL;

        // If missing, create the component
        UnlockableSlots slots = stack.get(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get());
        if(slots == null)
        {
            slots = new UnlockableSlots(this.getMaxAugmentBays(stack));
            stack.set(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get(), slots);
            return slots;
        }

        // Update the max bays if the size is different
        int maxBays = this.getMaxAugmentBays(stack);
        if(slots.getMaxSlots() != maxBays)
        {
            slots = slots.setMaxSlots(maxBays);
            stack.set(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get(), slots);
        }

        // Unlock the first augment bay if configured to do so
        if(Config.BACKPACK.augmentBays.unlockFirstAugmentBay.get())
        {
            if(!slots.isUnlocked(0))
            {
                slots = slots.unlockSlot(0);
                stack.set(ModDataComponents.UNLOCKABLE_AUGMENT_BAYS.get(), slots);
            }
        }

        return slots;
    }
}

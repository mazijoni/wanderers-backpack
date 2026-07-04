package com.mrcrayfish.backpacked.blockentity;

import com.google.common.base.Preconditions;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.block.ShelfBlock;
import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.ShelfKey;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.core.ModBlockEntities;
import com.mrcrayfish.backpacked.core.ModSounds;
import com.mrcrayfish.backpacked.inventory.ManagementInventory;
import com.mrcrayfish.backpacked.inventory.ShelfBackpackContainer;
import com.mrcrayfish.backpacked.inventory.container.BackpackShelfMenu;
import com.mrcrayfish.backpacked.inventory.container.data.ManagementContainerData;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.network.Network;
import com.mrcrayfish.backpacked.network.message.MessageLootboundTakeItem;
import com.mrcrayfish.backpacked.platform.Services;
import com.mrcrayfish.backpacked.util.BlockEntityUtil;
import com.mrcrayfish.framework.api.FrameworkAPI;
import com.mrcrayfish.framework.api.network.LevelLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Author: MrCrayfish
 */
public class ShelfBlockEntity extends BlockEntity
{
    public static final int SIZE = 1;

    private final SimpleContainer container = new ShelfContainer(this);
    private int recallQueueCount;
    private @Nullable UUID recallOwner = null;
    private int recallIndex = -1;
    private @Nullable ShelfKey key;
    private boolean loadingItems;

    public ShelfBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public ShelfBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.BACKPACK_SHELF.get(), pos, state);
    }

    public SimpleContainer getContainer()
    {
        return this.container;
    }

    public int getRecallQueueCount()
    {
        return this.recallQueueCount;
    }

    public void setRecallQueueCount(int queueCount)
    {
        if(this.recallQueueCount != queueCount)
        {
            this.recallQueueCount = queueCount;
            this.setChanged();
        }
    }

    public void recall(ItemStack stack, UUID owner, int originalIndex)
    {
        if(this.level instanceof ServerLevel level)
        {
            this.setBackpack(stack.copyAndClear());
            this.recallOwner = owner;
            this.recallIndex = originalIndex;

            Vec3 center = this.getBlockState().getShape(this.level, this.worldPosition).bounds().getCenter().add(Vec3.atLowerCornerOf(this.worldPosition));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 20, 0.25, 0.25, 0.25, 0.1);

            float pitch = 0.7F + 0.1F * level.random.nextFloat();
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_TELEPORT, SoundSource.BLOCKS, 1.0F, pitch);
        }
    }

    public ShelfKey key()
    {
        Preconditions.checkNotNull(this.level);
        if(this.key == null)
        {
            this.key = new ShelfKey(this.level.dimension(), this.worldPosition.asLong());
        }
        return this.key;
    }

    public void popBackpack(Player player)
    {
        if(!(this.level instanceof ServerLevel serverLevel))
            return;

        ItemStack backpack = this.getBackpack();
        if(backpack.isEmpty())
            return;

        Vec3 pos = this.worldPosition.getCenter().subtract(0, 0.25, 0);
        UUID originalOwner = this.recallOwner;
        int originalIndex = this.recallIndex;
        this.setBackpack(ItemStack.EMPTY);

        // Try to place the backpack on the original player if a recalled backpack
        if(player.getUUID().equals(originalOwner) && originalIndex != -1)
        {
            if(BackpackHelper.getBackpackStack(player, originalIndex).isEmpty())
            {
                if(BackpackHelper.setBackpackStack(player, backpack.copy(), originalIndex))
                {
                    Network.getPlay().sendToTrackingLocation(
                        () -> LevelLocation.create(serverLevel, this.worldPosition, 16),
                        new MessageLootboundTakeItem(player.getId(), new ItemStack(backpack.getItem()), pos, false)
                    );
                    serverLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), player.getSoundSource(), 1.0F, 1.0F, player.getRandom().nextLong());
                    return;
                }
            }
        }

        // Otherwise spawn into the world and instantly pickup by the player
        ItemEntity entity = new ItemEntity(serverLevel, pos.x, pos.y - 0.25, pos.z, backpack.copyAndClear());
        if(serverLevel.addFreshEntity(entity))
        {
            entity.playerTouch(player);
        }
    }

    public void openShelfManagement(ServerPlayer player)
    {
        UnlockableSlots slots = BackpackHelper.getBackpackUnlockableSlots(player);
        FrameworkAPI.openMenuWithData(player, new SimpleMenuProvider((windowId, playerInventory, player1) -> {
            return new BackpackShelfMenu(windowId, playerInventory, new ManagementInventory(player), this.container, slots);
        }, Component.translatable("container.backpack_shelf")), new ManagementContainerData(slots, false));
    }

    /**
     * Lets the backpack sitting on this shelf be browsed in place (sneak right-click) instead of
     * only being poppable off. Opened with {@code owner=false} since {@code ServerPlayHandler}'s
     * rename/sort/augment-toggle handlers key off a real player-equipped {@code backpackIndex} to
     * route back to - a shelved backpack has no such index, so those stay disabled here (matching
     * how a Wandering Trader's bag is browsed) while the storage grid itself is fully usable.
     */
    public void openBackpack(ServerPlayer player)
    {
        ItemStack backpack = this.getBackpack();
        if(!(backpack.getItem() instanceof BackpackItem item))
            return;

        ShelfBackpackContainer container = new ShelfBackpackContainer(item.getColumnCount(), item.getRowCount(), this);
        Component title = backpack.has(DataComponents.CUSTOM_NAME) ? backpack.getHoverName() : BackpackItem.BACKPACK_TRANSLATION;
        UnlockableSlots slots = item.getUnlockableSlots(backpack);
        Augments augments = Augments.get(backpack);
        Services.BACKPACK.openBackpackScreen(player, container, -1, -1, item.getColumnCount(), item.getRowCount(), false, slots, Pagination.NONE, augments, title, UnlockableSlots.NONE, 0);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        this.loadingItems = true;
        this.container.setItem(0, ItemStack.parseOptional(provider, tag.getCompound("Backpack")));
        this.loadingItems = false;

        this.recallQueueCount = tag.getInt("QueueCount");
        if(tag.hasUUID("RecallOwner"))
        {
            this.recallOwner = tag.getUUID("RecallOwner");
            if(tag.contains("RecallIndex"))
            {
                this.recallIndex = tag.getInt("RecallIndex");
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.put("Backpack", this.container.getItem(0).saveOptional(provider));
        tag.putInt("QueueCount", this.recallQueueCount);
        if(this.recallOwner != null)
        {
            tag.putUUID("RecallOwner", this.recallOwner);
            if(this.recallIndex > 0)
            {
                tag.putInt("RecallIndex", this.recallIndex);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        return this.saveWithoutMetadata(provider);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStack getBackpack()
    {
        return this.container.getItem(0);
    }

    public void setBackpack(ItemStack stack)
    {
        this.container.setItem(0, stack);
        this.recallOwner = null;
        this.recallIndex = -1;
        this.setChanged();
    }

    public Direction getDirection()
    {
        return this.getBlockState().getValue(ShelfBlock.FACING);
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        BlockEntityUtil.sendUpdatePacket(this);
    }

    public static class ShelfContainer extends SimpleContainer
    {
        private final ShelfBlockEntity shelf;

        public ShelfContainer(ShelfBlockEntity shelf)
        {
            super(1);
            this.shelf = shelf;
        }

        @Override
        public void setChanged()
        {
            Level level = this.shelf.level;
            if(level instanceof ServerLevel)
            {
                this.shelf.setChanged();
            }
        }

        @Override
        public void setItem(int slot, ItemStack stack)
        {
            ItemStack before = this.getItem(slot);
            if(this.shelf.level instanceof ServerLevel level)
            {
                if(before.isEmpty() ^ stack.isEmpty())
                {
                    float pitch = stack.isEmpty() ? 0.75F : 1.0F;
                    level.playSound(null, this.shelf.worldPosition, ModSounds.ITEM_BACKPACK_PLACE.get(), SoundSource.BLOCKS, 1.0F, pitch);
                }
            }
            super.setItem(slot, stack);
        }

        @Override
        public ItemStack removeItem(int slot, int count)
        {
            ItemStack stack = ContainerHelper.removeItem(this.getItems(), slot, count);
            if(!stack.isEmpty())
            {
                this.setChanged();
            }
            return stack;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack)
        {
            return stack.getItem() instanceof BackpackItem;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }

        public ShelfBlockEntity getShelf()
        {
            return this.shelf;
        }
    }
}

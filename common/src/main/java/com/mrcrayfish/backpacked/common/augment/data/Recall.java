package com.mrcrayfish.backpacked.common.augment.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.Constants;
import com.mrcrayfish.backpacked.block.ShelfBlock;
import com.mrcrayfish.backpacked.blockentity.ShelfBlockEntity;
import com.mrcrayfish.backpacked.common.ShelfKey;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.augment.impl.RecallAugment;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Tracks backpacks queued to be teleported back to a shelf (the Recall augment's "safety net" on
 * death), and delivers them once their shelf is loaded and free. This is a scaled-down version of
 * the original implementation - it keys queues directly by block position within this level
 * (instead of bucketing by chunk section) and doesn't use a point-of-interest cache to speed up
 * shelf lookups, since this pack doesn't need to scale to servers with huge numbers of shelves.
 */
public final class Recall extends SavedData
{
    public static final String ID = "wanderersbackpack_recall";
    private static final int MAX_QUEUE_SIZE = 18;

    private final ServerLevel level;
    private final Map<Long, ShelfQueue> queues;
    private int timer;

    @SuppressWarnings("DataFlowIssue")
    public static Factory<Recall> factory(ServerLevel level)
    {
        return new Factory<>(() -> new Recall(level), (tag, provider) -> load(level, provider, tag), null);
    }

    public Recall(ServerLevel level)
    {
        this.level = level;
        this.queues = new HashMap<>();
    }

    public void onShelfBroken(ShelfBlockEntity shelf)
    {
        this.removeAndFlushQueueAt(shelf.getBlockPos());
    }

    public boolean recallToShelf(ServerPlayer player, ShelfKey key, int originalIndex, ItemStack backpack)
    {
        BlockPos pos = BlockPos.of(key.position());
        if(!this.level.isInWorldBounds(pos))
            return false;

        if(!this.isShelfAtBlockPos(pos))
            return false;

        ShelfQueue queue = this.queues.computeIfAbsent(pos.asLong(), k -> new ShelfQueue());
        if(!queue.add(player, originalIndex, backpack, this.timer))
            return false;

        this.setDirty();
        return true;
    }

    public boolean isShelfAtBlockPos(BlockPos pos)
    {
        if(!this.level.isInWorldBounds(pos) || !this.level.isLoaded(pos))
            return false;
        return this.level.getBlockState(pos).getBlock() instanceof ShelfBlock;
    }

    private void removeAndFlushQueueAt(BlockPos pos)
    {
        ShelfQueue queue = this.queues.remove(pos.asLong());
        if(queue != null)
        {
            queue.forEach((owner, items) -> items.forEach(item -> {
                this.removeInvalidShelfFromItemStack(item.stack);
                this.flushItem(pos.getCenter(), item.stack);
            }));
            this.setDirty();
        }
    }

    private void removeInvalidShelfFromItemStack(ItemStack stack)
    {
        RecallAugment augment = BackpackHelper.findAugment(stack, ModAugmentTypes.RECALL.get());
        if(augment != null)
        {
            Augments augments = Augments.get(stack);
            for(Augments.Position position : Augments.Position.values())
            {
                if(augments.getAugment(position).type() == ModAugmentTypes.RECALL.get())
                {
                    augments = augments.setAugment(position, augment.setShelfKey(null));
                    break;
                }
            }
            Augments.set(stack, augments);
        }
    }

    private void flushItem(Vec3 position, ItemStack stack)
    {
        if(!stack.isEmpty())
        {
            ItemEntity entity = new ItemEntity(this.level, position.x, position.y, position.z, stack.copyAndClear());
            entity.setDefaultPickUpDelay();
            entity.setExtendedLifetime();
            this.level.addFreshEntity(entity);
        }
    }

    public void tick()
    {
        // Only run 4 times a second
        this.timer++;
        if(this.timer % 5 != 0)
            return;

        if(this.level.players().isEmpty())
            return;

        var it = this.queues.entrySet().iterator();
        while(it.hasNext())
        {
            var entry = it.next();
            BlockPos pos = BlockPos.of(entry.getKey());
            if(!this.level.isLoaded(pos))
                continue;

            ShelfQueue queue = entry.getValue();
            if(!(this.level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) || !this.isShelfAtBlockPos(pos))
            {
                queue.forEach((owner, items) -> items.forEach(item -> {
                    this.removeInvalidShelfFromItemStack(item.stack);
                    this.flushItem(pos.getCenter(), item.stack);
                }));
                it.remove();
                this.setDirty();
                continue;
            }

            if(shelf.getBackpack().isEmpty())
            {
                var minimum = getMinimumQueue(queue.queues);
                if(minimum != null)
                {
                    QueuedItem item = minimum.getSecond().removeFirst();
                    shelf.recall(item.stack, minimum.getFirst(), item.originalIndex);
                    queue.decrementCount();
                    queue.cleanQueues();
                    this.setDirty();
                }
            }

            shelf.setRecallQueueCount(queue.count);

            if(queue.isEmpty())
            {
                it.remove();
                this.setDirty();
            }
        }
    }

    @Nullable
    private static Pair<UUID, List<QueuedItem>> getMinimumQueue(@Nullable Map<UUID, List<QueuedItem>> playerToQueue)
    {
        if(playerToQueue == null)
            return null;

        UUID owner = null;
        List<QueuedItem> minItems = null;
        for(var entry : playerToQueue.entrySet())
        {
            var items = entry.getValue();
            if(items.isEmpty())
                continue;

            if(minItems == null || items.getFirst().time < minItems.getFirst().time)
            {
                owner = entry.getKey();
                minItems = items;
            }
        }
        return minItems != null ? Pair.of(owner, minItems) : null;
    }

    private static Recall load(ServerLevel level, HolderLookup.Provider provider, CompoundTag tag)
    {
        Recall recall = new Recall(level);
        recall.timer = tag.getInt("Timer");

        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        ListTag shelfList = tag.getList("RecallQueues", Tag.TAG_COMPOUND);
        shelfList.forEach(nbt -> {
            try
            {
                CompoundTag shelfTag = (CompoundTag) nbt;
                if(!shelfTag.contains("Pos", Tag.TAG_LONG))
                    throw new IllegalArgumentException("Missing shelf position");

                long pos = shelfTag.getLong("Pos");
                ListTag entryList = shelfTag.getList("PlayerQueues", Tag.TAG_COMPOUND);
                if(entryList.isEmpty())
                    return;

                Map<UUID, List<QueuedItem>> playerQueues = new LinkedHashMap<>();
                entryList.forEach(nbt1 -> {
                    try
                    {
                        CompoundTag entryTag = (CompoundTag) nbt1;
                        UUID owner = entryTag.getUUID("Owner");
                        List<QueuedItem> items = QueuedItem.CODEC.listOf()
                            .parse(ops, entryTag.get("Backpacks"))
                            .resultOrPartial(Constants.LOG::error)
                            .map(LinkedList::new).orElse(new LinkedList<>());
                        items.removeIf(item -> item.stack.isEmpty());
                        if(!items.isEmpty())
                        {
                            playerQueues.put(owner, items);
                        }
                    }
                    catch(Exception e)
                    {
                        Constants.LOG.error("Error while reading Recall player queues", e);
                    }
                });

                if(!playerQueues.isEmpty())
                {
                    recall.queues.put(pos, new ShelfQueue(playerQueues));
                }
            }
            catch(Exception e)
            {
                Constants.LOG.error("Error while reading Recall shelf queues", e);
            }
        });
        return recall;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        tag.putInt("Timer", this.timer);
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        ListTag shelfList = new ListTag();
        this.queues.forEach((pos, shelfQueue) -> {
            if(shelfQueue.isEmpty())
                return;
            CompoundTag shelfTag = new CompoundTag();
            shelfTag.putLong("Pos", pos);
            ListTag entryList = new ListTag();
            shelfQueue.forEach((owner, items) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("Owner", owner);
                QueuedItem.CODEC.listOf()
                    .encodeStart(ops, items)
                    .resultOrPartial(Constants.LOG::error)
                    .ifPresent(t -> entryTag.put("Backpacks", t));
                entryList.add(entryTag);
            });
            if(!entryList.isEmpty())
            {
                shelfTag.put("PlayerQueues", entryList);
                shelfList.add(shelfTag);
            }
        });
        if(!shelfList.isEmpty())
        {
            tag.put("RecallQueues", shelfList);
        }
        return tag;
    }

    private static final class ShelfQueue
    {
        private @Nullable Map<UUID, List<QueuedItem>> queues;
        private int count;

        private ShelfQueue() {}

        private ShelfQueue(@Nullable Map<UUID, List<QueuedItem>> queues)
        {
            this.queues = queues != null && !queues.isEmpty() ? queues : null;
            this.cleanQueues();
            this.updateCount();
        }

        public void forEach(BiConsumer<UUID, List<QueuedItem>> consumer)
        {
            if(this.queues != null)
            {
                this.queues.forEach(consumer);
            }
        }

        public boolean add(ServerPlayer player, int originalIndex, ItemStack backpack, int time)
        {
            if(this.queues == null)
            {
                this.count = 0;
                this.queues = new HashMap<>();
            }
            List<QueuedItem> items = this.queues.computeIfAbsent(player.getUUID(), k -> new LinkedList<>());
            if(items.size() >= MAX_QUEUE_SIZE)
                return false;
            items.add(new QueuedItem(originalIndex, backpack.copyAndClear(), time));
            this.count++;
            return true;
        }

        private void decrementCount()
        {
            if(this.count > 0)
            {
                this.count--;
            }
        }

        private void updateCount()
        {
            if(this.queues != null)
            {
                this.count = 0;
                for(List<QueuedItem> items : this.queues.values())
                {
                    this.count += items.size();
                }
            }
        }

        private void cleanQueues()
        {
            if(this.queues != null)
            {
                this.queues.entrySet().removeIf(e -> e.getValue().isEmpty());
                if(this.queues.isEmpty())
                {
                    this.queues = null;
                    this.count = 0;
                }
            }
        }

        private boolean isEmpty()
        {
            return this.queues == null || this.queues.isEmpty();
        }
    }

    private record QueuedItem(int originalIndex, ItemStack stack, int time)
    {
        private static final Codec<QueuedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("original_index").orElse(-1).forGetter(QueuedItem::originalIndex),
            ItemStack.OPTIONAL_CODEC.fieldOf("item").orElse(ItemStack.EMPTY).forGetter(QueuedItem::stack),
            Codec.INT.fieldOf("queued_at").orElse(0).forGetter(QueuedItem::time)
        ).apply(instance, QueuedItem::new));
    }

    public interface Access
    {
        Recall backpacked$getRecall();
    }
}

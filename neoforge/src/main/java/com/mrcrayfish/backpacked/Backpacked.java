package com.mrcrayfish.backpacked;

import com.mrcrayfish.backpacked.block.HayBedBlock;
import com.mrcrayfish.backpacked.blockentity.HayBedBlockEntity;
import com.mrcrayfish.backpacked.client.ClientBootstrap;
import com.mrcrayfish.backpacked.common.WanderingTraderEvents;
import com.mrcrayfish.backpacked.common.augment.AugmentHandler;
import com.mrcrayfish.backpacked.common.augment.impl.ImbuedHideAugment;
import com.mrcrayfish.backpacked.common.augment.impl.RecallAugment;
import com.mrcrayfish.backpacked.common.backpack.loader.BackpackLoader;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import com.mrcrayfish.backpacked.core.ModBlocks;
import com.mrcrayfish.backpacked.core.ModFeatures;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.datagen.BlockTagGen;
import com.mrcrayfish.backpacked.datagen.LootTableGen;
import com.mrcrayfish.backpacked.datagen.RecipeGen;
import com.mrcrayfish.backpacked.integration.YoureInGraveDangerSupport;
import com.mrcrayfish.backpacked.worldgen.MushroomShelfFeature;
import com.mrcrayfish.framework.api.Environment;
import com.mrcrayfish.framework.api.util.TaskRunner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// TODO clean up this class

/**
 * Author: MrCrayfish
 */
@Mod(Constants.MOD_ID)
public class Backpacked
{
    /**
     * Backpacks equipped in the vanilla chest slot are removed here, in {@link LivingDeathEvent}
     * (fired before {@code dropAllDeathLoot}/{@code dropEquipment}), so that vanilla's
     * {@code Inventory#dropAll} never gets a chance to auto-drop them itself - otherwise it would
     * beat {@link #onLivingDrops} to it, since that only runs once the {@link LivingDropsEvent} is
     * fired afterwards, bypassing Recall/keepOnDeath entirely. Stashed here and picked back up by
     * {@link #onLivingDrops} for the actual Recall-or-drop handling, keyed by player so the two
     * events agree on what was removed for a given death.
     */
    private final Map<UUID, NonNullList<ItemStack>> pendingRemovedBackpacks = new HashMap<>();

    /**
     * The configured features a planted oak/birch sapling can grow into (see vanilla's
     * {@code TreeGrower.OAK}/{@code TreeGrower.BIRCH}). Referenced by resource location rather than
     * the {@code net.minecraft.data.worldgen.features.TreeFeatures} constants, since that datagen
     * class isn't guaranteed to be present on a dedicated server at runtime.
     */
    private static final Set<ResourceKey<ConfiguredFeature<?, ?>>> OAK_AND_BIRCH_FEATURES = Set.of(
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("oak")),
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("oak_bees_005")),
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("fancy_oak")),
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("fancy_oak_bees_005")),
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("birch")),
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("birch_bees_005"))
    );

    /** Chance that a grown oak/birch sapling additionally rolls for a nearby mushroom shelf/sprout */
    private static final float SAPLING_GROWTH_CHANCE = 0.1F;

    public Backpacked(IEventBus bus)
    {
        TaskRunner.runIf(Environment.CLIENT, () -> ClientBootstrap::earlyInit);
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onGatherData);
        ModFeatures.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onEntityDropLoot);
        NeoForge.EVENT_BUS.addListener(this::onInteract);
        NeoForge.EVENT_BUS.addListener(this::onGetProjectile);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::onBlockDropLoot);
        // LOWEST: only strip equipped backpacks once we know death is definitely proceeding (i.e.
        // no other mod cancelled it at a higher priority), otherwise a cancelled death would leave
        // the player alive but silently missing their equipped backpacks.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(this::onEntityInvulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(this::onBlockGrowFeature);
        NeoForge.EVENT_BUS.addListener(this::onLivingEquipmentChange);
        NeoForge.EVENT_BUS.addListener(this::onPlayerWakeUp);

        if(ModList.get().isLoaded("yigd"))
        {
            YoureInGraveDangerSupport.init();
        }
    }

    /**
     * Gives planted oak/birch saplings the same small chance of growing a mushroom shelf/sprout
     * that naturally-generated trees in the forest/birch forest biomes have. The tree hasn't been
     * placed into the world yet at the point this event fires (it only lets us pick which
     * configured feature grows), so the actual log-scanning happens a tick later, once
     * {@code TreeGrower} has finished placing the tree this same tick.
     */
    private void onBlockGrowFeature(BlockGrowFeatureEvent event)
    {
        Holder<ConfiguredFeature<?, ?>> feature = event.getFeature();
        if(feature == null)
            return;

        Optional<ResourceKey<ConfiguredFeature<?, ?>>> key = feature.unwrapKey();
        if(key.isEmpty() || !OAK_AND_BIRCH_FEATURES.contains(key.get()))
            return;

        if(!(event.getLevel() instanceof ServerLevel level))
            return;

        if(level.random.nextFloat() >= SAPLING_GROWTH_CHANCE)
            return;

        BlockPos pos = event.getPos();
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () ->
            MushroomShelfFeature.tryAttachNearby(level, level.random, pos)
        ));
    }

    /**
     * A Hay Bed disappears back into the sleeper's inventory once they get up. {@link PlayerWakeUpEvent}
     * fires from {@code Player.stopSleepInBed} before the sleeping position is actually cleared, so
     * {@code player.getSleepingPos()} is still valid here.
     */
    private void onPlayerWakeUp(PlayerWakeUpEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player))
            return;

        player.getSleepingPos().ifPresent(pos -> {
            Level level = player.level();
            BlockState state = level.getBlockState(pos);
            if(state.getBlock() instanceof HayBedBlock)
            {
                // Both halves store the same FACING; find the partner half so it can be removed too.
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                BlockPos otherPos = state.getValue(HayBedBlock.PART) == BedPart.HEAD ? pos.relative(facing.getOpposite()) : pos.relative(facing);

                // Checked on both halves (rather than just the sleeping position) since the Bed
                // augment marks both when it summons a bed - this way a mismatch between the two
                // can't accidentally make a summoned bed drop/return an item nobody placed.
                boolean summoned = isSummoned(level, pos) || isSummoned(level, otherPos);
                level.removeBlock(pos, false);
                if(level.getBlockState(otherPos).getBlock() instanceof HayBedBlock)
                {
                    level.removeBlock(otherPos, false);
                }
                // The Bed augment's temporary bed didn't come from the player's inventory, so it
                // shouldn't return or drop an item when it vanishes - only a real, manually placed
                // Hay Bed gives itself back. Creative players don't need it given back either (a
                // Hay Bed can be placed without sneaking, which immediately triggers sleep - waking
                // up straight afterward would otherwise hand a "new" Hay Bed to their creative
                // inventory/hotbar, which reads exactly like an unwanted drop).
                if(!summoned && !player.isCreative() && !player.getInventory().add(new ItemStack(ModBlocks.HAY_BED.get())))
                {
                    Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), new ItemStack(ModBlocks.HAY_BED.get()));
                }
            }
        });
    }

    private static boolean isSummoned(Level level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof HayBedBlockEntity hayBedBlockEntity && hayBedBlockEntity.isSummoned();
    }

    /**
     * Backpacks implement {@link net.minecraft.world.item.Equipable} so players can wear one in the
     * vanilla chest slot, but that also makes them valid chest-slot equipment for every other living
     * entity - a mob that auto-equips gear it finds (or is given random equipment on spawn) could
     * end up wearing one. This fires for any equipment change detected on a living entity (including
     * joining the world), so it catches both cases; anything but a player gets its backpack
     * immediately unequipped and dropped on the ground instead.
     */
    private void onLivingEquipmentChange(LivingEquipmentChangeEvent event)
    {
        if(event.getSlot() != EquipmentSlot.CHEST || !(event.getTo().getItem() instanceof BackpackItem))
            return;

        LivingEntity entity = event.getEntity();
        if(entity instanceof Player)
            return;

        entity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        if(entity.level() instanceof ServerLevel level)
        {
            Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), event.getTo().copy());
        }
    }

    private void onLivingDeath(LivingDeathEvent event)
    {
        if(event.getEntity() instanceof ServerPlayer player && this.shouldDropBackpacksOnDeath(player))
        {
            this.pendingRemovedBackpacks.put(player.getUUID(), BackpackHelper.removeAllBackpacks(player));
        }
    }

    private boolean shouldDropBackpacksOnDeath(ServerPlayer player)
    {
        return !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
            && !Config.BACKPACK.equipable.keepOnDeath.get();
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(Bootstrap::init);
    }

    private void onGatherData(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        generator.addProvider(event.includeServer(), new LootTableGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new RecipeGen(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new BlockTagGen(packOutput, lookupProvider, event.getExistingFileHelper()));
    }

    private void addReloadListener(AddReloadListenerEvent event)
    {
        event.addListener(new BackpackLoader(event.getServerResources().getRegistryLookup()));
    }

    private void onEntityDropLoot(LivingDropsEvent event)
    {
        if(event.getSource().getEntity() instanceof ServerPlayer player)
        {
            AugmentHandler.onLootDroppedByEntity(event.getDrops(), player);
        }
    }

    private void onBlockDropLoot(BlockDropsEvent event)
    {
        Entity breaker = event.getBreaker();
        if(breaker instanceof Player player)
        {
            AugmentHandler.onLootDroppedByBlock(event.getDrops(), player);
        }
    }

    private void onInteract(PlayerInteractEvent.EntityInteract event)
    {
        if(WanderingTraderEvents.onInteract(event.getTarget(), event.getEntity()))
        {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private void onGetProjectile(LivingGetProjectileEvent event)
    {
        if(event.getEntity() instanceof Player player)
        {
            ItemStack ammo = AugmentHandler.locateAmmunition(player, event.getProjectileWeaponItemStack(), event.getProjectileItemStack());
            if(!ammo.isEmpty())
            {
                event.setProjectileItemStack(ammo);
            }
        }
    }

    private void onLivingDrops(LivingDropsEvent event)
    {
        if(event.getEntity() instanceof ServerPlayer player)
        {
            NonNullList<ItemStack> removed = this.pendingRemovedBackpacks.remove(player.getUUID());
            if(removed == null)
                return;

            for(int index = 0; index < removed.size(); index++)
            {
                ItemStack stack = removed.get(index);
                if(!stack.isEmpty())
                {
                    RecallAugment augment = BackpackHelper.findAugment(stack, ModAugmentTypes.RECALL.get());
                    if(augment != null && AugmentHandler.recallBackpack(player, index, stack, augment))
                    {
                        continue;
                    }
                    event.getDrops().add(this.createDrop(player, stack));
                }
            }
        }
    }

    private ItemEntity createDrop(Player player, ItemStack stack)
    {
        float deltaX = player.getRandom().nextFloat() * 0.5F;
        float deltaZ = player.getRandom().nextFloat() * (Mth.PI * 2);
        ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3F, player.getZ(), stack.copyAndClear());
        entity.setDeltaMovement(-Mth.sin(deltaZ) * deltaX, 0.2, Mth.cos(deltaZ) * deltaX);
        entity.setPickUpDelay(40);
        return entity;
    }

    private void onEntityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event)
    {
        // Don't run logic if already invulnerable
        if(event.isInvulnerable())
            return;

        if(event.getEntity().getType() == EntityType.ITEM)
        {
            ItemEntity entity = ((ItemEntity) event.getEntity());
            ItemStack stack = entity.getItem();
            if(stack.getItem() instanceof BackpackItem)
            {
                ImbuedHideAugment augment = BackpackHelper.findAugment(stack, ModAugmentTypes.IMBUED_HIDE.get());
                if(augment != null)
                {
                    if(this.isImbuedHideImmuneToDamageSource(entity.registryAccess(), event.getSource()))
                    {
                        event.setInvulnerable(true);
                    }
                }
            }
        }
    }

    private boolean isImbuedHideImmuneToDamageSource(RegistryAccess access, DamageSource source)
    {
        Registry<DamageType> types = access.registryOrThrow(Registries.DAMAGE_TYPE);
        ResourceLocation key = types.getKey(source.type());
        if(key != null)
        {
            return Config.AUGMENTS.imbuedHide.invulnerableToDamageTypes.get().contains(key.toString());
        }
        return false;
    }
}

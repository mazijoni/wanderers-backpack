package com.mrcrayfish.backpacked.core;

import com.mrcrayfish.backpacked.common.augment.impl.BedAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FarmhandAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FluidTankAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FunnellingAugment;
import com.mrcrayfish.backpacked.common.augment.impl.GiantAugment;
import com.mrcrayfish.backpacked.common.augment.impl.HopperBridgeAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ImbuedHideAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ImmortalAugment;
import com.mrcrayfish.backpacked.common.augment.impl.LightweaverAugment;
import com.mrcrayfish.backpacked.common.augment.impl.LootboundAugment;
import com.mrcrayfish.backpacked.common.augment.impl.QuiverlinkAugment;
import com.mrcrayfish.backpacked.common.augment.impl.RecallAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ReforgeAugment;
import com.mrcrayfish.backpacked.common.augment.impl.SeedflowAugment;
import com.mrcrayfish.backpacked.item.AugmentItem;
import com.mrcrayfish.backpacked.platform.Services;
import com.mrcrayfish.backpacked.util.Utils;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import net.minecraft.world.item.Item;

/**
 * Author: MrCrayfish
 */
@RegistryContainer
public class ModItems
{
    public static final RegistryEntry<Item> BACKPACK = RegistryEntry.item(Utils.rl("backpack"), () -> Services.BACKPACK.createBackpackItem(new Item.Properties().stacksTo(1), 9, 4, 2));
    public static final RegistryEntry<Item> COPPER_BACKPACK = RegistryEntry.item(Utils.rl("copper_backpack"), () -> Services.BACKPACK.createBackpackItem(new Item.Properties().stacksTo(1), 9, 5, 2));
    public static final RegistryEntry<Item> GOLD_BACKPACK = RegistryEntry.item(Utils.rl("gold_backpack"), () -> Services.BACKPACK.createBackpackItem(new Item.Properties().stacksTo(1), 9, 6, 3));
    public static final RegistryEntry<Item> DIAMOND_BACKPACK = RegistryEntry.item(Utils.rl("diamond_backpack"), () -> Services.BACKPACK.createBackpackItem(new Item.Properties().stacksTo(1), 9, 7, 3));
    public static final RegistryEntry<Item> NETHERITE_BACKPACK = RegistryEntry.item(Utils.rl("netherite_backpack"), () -> Services.BACKPACK.createBackpackItem(new Item.Properties().stacksTo(1).fireResistant(), 11, 9, 4));

    // Augments are now physical items you place directly into the augment slots (3, or 4 on the
    // diamond backpack), instead of picking from a popup menu.
    public static final RegistryEntry<Item> FUNNELLING_AUGMENT = RegistryEntry.item(Utils.rl("funnelling_augment"), () -> new AugmentItem(FunnellingAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> IMBUED_HIDE_AUGMENT = RegistryEntry.item(Utils.rl("imbued_hide_augment"), () -> new AugmentItem(ImbuedHideAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> IMMORTAL_AUGMENT = RegistryEntry.item(Utils.rl("immortal_augment"), () -> new AugmentItem(ImmortalAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> REFORGE_AUGMENT = RegistryEntry.item(Utils.rl("reforge_augment"), () -> new AugmentItem(ReforgeAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> LIGHTWEAVER_AUGMENT = RegistryEntry.item(Utils.rl("lightweaver_augment"), () -> new AugmentItem(LightweaverAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> FARMHAND_AUGMENT = RegistryEntry.item(Utils.rl("farmhand_augment"), () -> new AugmentItem(FarmhandAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> QUIVERLINK_AUGMENT = RegistryEntry.item(Utils.rl("quiverlink_augment"), () -> new AugmentItem(QuiverlinkAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> LOOTBOUND_AUGMENT = RegistryEntry.item(Utils.rl("lootbound_augment"), () -> new AugmentItem(LootboundAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> SEEDFLOW_AUGMENT = RegistryEntry.item(Utils.rl("seedflow_augment"), () -> new AugmentItem(SeedflowAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> HOPPER_BRIDGE_AUGMENT = RegistryEntry.item(Utils.rl("hopper_bridge_augment"), () -> new AugmentItem(HopperBridgeAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> GIANT_AUGMENT = RegistryEntry.item(Utils.rl("giant_augment"), () -> new AugmentItem(GiantAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> RECALL_AUGMENT = RegistryEntry.item(Utils.rl("recall_augment"), () -> new AugmentItem(RecallAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> BED_AUGMENT = RegistryEntry.item(Utils.rl("bed_augment"), () -> new AugmentItem(BedAugment.TYPE, new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> FLUID_TANK_AUGMENT = RegistryEntry.item(Utils.rl("fluid_tank_augment"), () -> new AugmentItem(FluidTankAugment.TYPE, new Item.Properties().stacksTo(1)));

    // Not obtainable - exists only to host the backpack_bed.json/backpack_fluid.json models,
    // rendered onto the worn backpack by BackpackLayer when the corresponding augment is equipped.
    // Hidden from the creative tab in ModCreativeTabs.
    public static final RegistryEntry<Item> BACKPACK_BED = RegistryEntry.item(Utils.rl("backpack_bed"), () -> new Item(new Item.Properties()));
    public static final RegistryEntry<Item> BACKPACK_FLUID_TANK = RegistryEntry.item(Utils.rl("backpack_fluid_tank"), () -> new Item(new Item.Properties()));
}

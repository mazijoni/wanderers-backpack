package com.mrcrayfish.backpacked.network;

import com.mrcrayfish.backpacked.common.backpack.BackpackManager;
import com.mrcrayfish.backpacked.network.message.*;
import com.mrcrayfish.backpacked.util.Utils;
import com.mrcrayfish.framework.api.FrameworkAPI;
import com.mrcrayfish.framework.api.network.FrameworkNetwork;
import net.minecraft.network.protocol.PacketFlow;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class Network
{
    public static final FrameworkNetwork PLAY = FrameworkAPI
        .createNetworkBuilder(Utils.rl("play"), 1)
        .registerConfigurationMessage("sync_backpacks", MessageSyncBackpacks.class, MessageSyncBackpacks.STREAM_CODEC, MessageSyncBackpacks::handle, () -> List.of(BackpackManager.instance().getSyncMessage()))
        .registerPlayMessage("open_backpack", MessageOpenBackpack.class, MessageOpenBackpack.STREAM_CODEC, MessageOpenBackpack::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("pickpocket_backpack", MessagePickpocketBackpack.class, MessagePickpocketBackpack.STREAM_CODEC, MessagePickpocketBackpack::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("navigate_backpack", MessageNavigateBackpack.class, MessageNavigateBackpack.STREAM_CODEC, MessageNavigateBackpack::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("backpack_cosmetics", MessageBackpackCosmetics.class, MessageBackpackCosmetics.STREAM_CODEC, MessageBackpackCosmetics::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("sync_unlock_tracker", MessageSyncUnlockTracker.class, MessageSyncUnlockTracker.STREAM_CODEC, MessageSyncUnlockTracker::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("unlock_backpack", MessageUnlockBackpack.class, MessageUnlockBackpack.STREAM_CODEC, MessageUnlockBackpack::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("sync_villager_backpack", MessageSyncVillagerBackpack.class, MessageSyncVillagerBackpack.STREAM_CODEC, MessageSyncVillagerBackpack::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("unlock_slot", MessageUnlockSlot.class, MessageUnlockSlot.STREAM_CODEC, MessageUnlockSlot::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("sync_unlock_slot", MessageSyncUnlockSlot.class, MessageSyncUnlockSlot.STREAM_CODEC, MessageSyncUnlockSlot::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("set_augment_state", MessageSetAugmentState.class, MessageSetAugmentState.STREAM_CODEC, MessageSetAugmentState::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("update_augment", MessageUpdateAugment.class, MessageUpdateAugment.STREAM_CODEC, MessageUpdateAugment::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("sync_augment_change", MessageSyncAugmentChange.class, MessageSyncAugmentChange.STREAM_CODEC, MessageSyncAugmentChange::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("lootbound_take_item", MessageLootboundTakeItem.class, MessageLootboundTakeItem.STREAM_CODEC, MessageLootboundTakeItem::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("rename_backpack", MessageRenameBackpack.class, MessageRenameBackpack.STREAM_CODEC, MessageRenameBackpack::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("sort_backpack", MessageSortBackpack.class, MessageSortBackpack.STREAM_CODEC, MessageSortBackpack::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("farmhand_plant", MessageFarmhandPlant.class, MessageFarmhandPlant.STREAM_CODEC, MessageFarmhandPlant::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("show_equip_hint", MessageShowEquipHint.class, MessageShowEquipHint.STREAM_CODEC, MessageShowEquipHint::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("check_shelf_key", MessageCheckShelfKey.class, MessageCheckShelfKey.STREAM_CODEC, MessageCheckShelfKey::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("response_shelf_key", MessageResponseShelfKey.class, MessageResponseShelfKey.STREAM_CODEC, MessageResponseShelfKey::handle, PacketFlow.CLIENTBOUND)
        .registerPlayMessage("use_bed_augment", MessageUseBedAugment.class, MessageUseBedAugment.STREAM_CODEC, MessageUseBedAugment::handle, PacketFlow.SERVERBOUND)
        .registerPlayMessage("interact_fluid_tank", MessageInteractFluidTank.class, MessageInteractFluidTank.STREAM_CODEC, MessageInteractFluidTank::handle, PacketFlow.SERVERBOUND)
        .build();

    public static void init() {}

    public static FrameworkNetwork getPlay()
    {
        return PLAY;
    }
}

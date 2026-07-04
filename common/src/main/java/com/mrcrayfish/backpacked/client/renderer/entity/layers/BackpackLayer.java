package com.mrcrayfish.backpacked.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import com.mrcrayfish.backpacked.core.ModItems;
import com.mrcrayfish.backpacked.item.BackpackItem;
import com.mrcrayfish.backpacked.platform.Services;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Renders the equipped backpack directly from its own ItemStack/model (see models/item/backpack.json
 * and friends), instead of Backpacked's original cosmetic-style DSL (ClientBackpack/ModelMeta/
 * BackpackRenderContext). Positioning logic (relative to the player's body, offset for a worn
 * chestplate) is kept from the original implementation.
 *
 * Author: MrCrayfish
 */
public class BackpackLayer<T extends Player, M extends PlayerModel<T>> extends RenderLayer<T, M>
{
    private final ItemRenderer itemRenderer;

    public BackpackLayer(RenderLayerParent<T, M> renderer, ItemRenderer itemRenderer)
    {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource source, int light, T player, float p_225628_5_, float p_225628_6_, float partialTick, float p_225628_8_, float p_225628_9_, float p_225628_10_)
    {
        if(!Services.BACKPACK.isBackpackVisible(player))
            return;

        ItemStack stack = BackpackHelper.getFirstBackpackStack(player);
        if(stack.isEmpty())
            return;

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(chestStack.getItem() == Items.ELYTRA)
            return;

        pose.pushPose();

        // Transforms the pose to player's body
        this.getParentModel().body.translateAndRotate(pose);

        // Apply transforms to fix rotation and inverted model. Uses ItemDisplayContext.NONE
        // (instead of FIXED) so the model's own baked "fixed" transform (rotation [0,-180,0]) isn't
        // also applied on top of this - both are Y-axis rotations, so stacking them cancelled out
        // to a net 0 degrees, leaving the backpack facing backwards.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(1.0F, -1.0F, -1.0F);
        // A worn chestplate pushes the backpack further out. When the backpack itself is what's
        // occupying the chest slot (equipped there instead of Curios), there's no real chestplate
        // underneath it, so it must sit at the same position as if equipped via Curios with an
        // empty chest slot.
        boolean wearingChestArmor = !chestStack.isEmpty() && !(chestStack.getItem() instanceof BackpackItem);
        int offset = wearingChestArmor ? 6 : 5;
        pose.translate(0, -0.15, offset * 0.0625 - 0.06);

        this.itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, source, player.level(), player.getId());

        if(BackpackHelper.findAugment(stack, ModAugmentTypes.BED.get()) != null)
        {
            ItemStack bedStack = new ItemStack(ModItems.BACKPACK_BED.get());
            this.itemRenderer.renderStatic(bedStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, source, player.level(), player.getId());
        }

        if(BackpackHelper.findAugment(stack, ModAugmentTypes.FLUID_TANK.get()) != null)
        {
            ItemStack tankStack = new ItemStack(ModItems.BACKPACK_FLUID_TANK.get());
            this.itemRenderer.renderStatic(tankStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, source, player.level(), player.getId());
        }

        pose.popPose();
    }
}

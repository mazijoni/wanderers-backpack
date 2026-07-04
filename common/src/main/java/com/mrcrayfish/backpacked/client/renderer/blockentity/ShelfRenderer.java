package com.mrcrayfish.backpacked.client.renderer.blockentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.blockentity.ShelfBlockEntity;
import com.mrcrayfish.backpacked.client.Icons;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import com.mrcrayfish.backpacked.core.ModItems;
import com.mrcrayfish.backpacked.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Matrix4f;

/**
 * Renders the shelved backpack directly from its own ItemStack/model (see {@code models/item/backpack.json}
 * and friends), the same way {@link com.mrcrayfish.backpacked.client.renderer.entity.layers.BackpackLayer}
 * renders an equipped one, instead of Backpacked's original cosmetic-style DSL. The rotation/offset
 * constants that position the model "resting" on the shelf are kept from the original implementation.
 *
 * Author: MrCrayfish
 */
public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity>
{
    private static final Component RECALL_ICON = ScreenUtil.getIconComponent(Icons.RECALL);

    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderDispatcher;

    public ShelfRenderer(BlockEntityRendererProvider.Context context)
    {
        this.itemRenderer = context.getItemRenderer();
        this.entityRenderDispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(ShelfBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay)
    {
        ItemStack stack = entity.getBackpack();
        if(stack.isEmpty())
            return;

        Direction facing = entity.getDirection();
        this.renderBackpackName(entity, facing, pose, buffer, light);

        pose.pushPose();

        pose.translate(0.5, 0.0, 0.5);
        pose.translate(0, 0.001, 0);
        pose.mulPose(facing.getRotation());

        pose.translate(-0.5, 0.0, -0.5);
        pose.translate(0.5, -6 * 0.0625, -5 * 0.0625);

        // Fix rotation and invert, matching how the model sits flat on the shelf. Uses
        // ItemDisplayContext.NONE (instead of FIXED) so the model's own baked "fixed" transform
        // isn't also applied on top of this - see BackpackLayer for the same reasoning.
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.scale(1.0F, -1.0F, -1.0F);

        this.itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffer, entity.getLevel(), (int) entity.getBlockPos().asLong());

        if(BackpackHelper.findAugment(stack, ModAugmentTypes.BED.get()) != null)
        {
            ItemStack bedStack = new ItemStack(ModItems.BACKPACK_BED.get());
            this.itemRenderer.renderStatic(bedStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffer, entity.getLevel(), (int) entity.getBlockPos().asLong());
        }

        if(BackpackHelper.findAugment(stack, ModAugmentTypes.FLUID_TANK.get()) != null)
        {
            ItemStack tankStack = new ItemStack(ModItems.BACKPACK_FLUID_TANK.get());
            this.itemRenderer.renderStatic(tankStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffer, entity.getLevel(), (int) entity.getBlockPos().asLong());
        }

        pose.popPose();
        RenderSystem.disableBlend();
    }

    private void renderBackpackName(ShelfBlockEntity shelf, Direction facing, PoseStack poseStack, MultiBufferSource source, int light)
    {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(facing.getRotation());
        poseStack.translate(0, -0.1875, -1.1875);
        poseStack.mulPose(facing.getRotation().invert());
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.02F, -0.02F, 0.02F);

        Minecraft mc = Minecraft.getInstance();
        if(mc.hitResult instanceof BlockHitResult result && result.getBlockPos().equals(shelf.getBlockPos()))
        {
            ItemStack backpack = shelf.getBackpack();
            if(!backpack.isEmpty() && backpack.has(DataComponents.CUSTOM_NAME))
            {
                Component label = backpack.get(DataComponents.CUSTOM_NAME);
                if(label != null)
                {
                    float halfWidth = mc.font.width(label) / 2F;
                    mc.font.drawInBatch(label, -halfWidth, 0, 0x20FFFFFF, false, poseStack.last().pose(), source, Font.DisplayMode.SEE_THROUGH, 0x2A000000, light);
                    mc.font.drawInBatch(label, -halfWidth, 0, -1, true, poseStack.last().pose(), source, Font.DisplayMode.NORMAL, 0, light);
                    poseStack.translate(0, -12, 0);
                }
            }
        }

        int recallCount = shelf.getRecallQueueCount();
        if(recallCount > 0)
        {
            poseStack.scale(1.1F, 1.1F, 1.1F);
            Matrix4f matrix = poseStack.last().pose();
            Component label = ScreenUtil.join(" ", RECALL_ICON, Component.literal(Integer.toString(recallCount)));
            float halfWidth = mc.font.width(label) / 2F;
            mc.font.drawInBatch(label, -halfWidth, 0, 0x20FFFFFF, false, matrix, source, Font.DisplayMode.SEE_THROUGH, 0, light);
            mc.font.drawInBatch(label, -halfWidth, 0, -1, false, matrix, source, Font.DisplayMode.NORMAL, 0, light);
        }

        poseStack.popPose();
    }
}

package com.mrcrayfish.backpacked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.Constants;
import com.mrcrayfish.backpacked.client.backpack.loader.ModelMetaLoader;
import com.mrcrayfish.backpacked.client.gui.BackpackContentsTooltipRenderer;
import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen;
import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackShelfScreen;
import com.mrcrayfish.backpacked.client.renderer.FirstPersonEffectsRenderer;
import com.mrcrayfish.backpacked.client.renderer.blockentity.ShelfRenderer;
import com.mrcrayfish.backpacked.client.renderer.entity.layers.BackpackLayer;
import com.mrcrayfish.backpacked.client.renderer.entity.layers.VillagerBackpackLayer;
import com.mrcrayfish.backpacked.common.backpack.BackpackContentsTooltip;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import com.mrcrayfish.backpacked.core.ModBlockEntities;
import com.mrcrayfish.backpacked.core.ModBlocks;
import com.mrcrayfish.backpacked.core.ModContainers;
import com.mrcrayfish.backpacked.core.ModItems;
import com.mrcrayfish.backpacked.packs.AddonRepositorySource;
import com.mrcrayfish.backpacked.util.Utils;
import com.mrcrayfish.framework.api.client.FrameworkClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.nio.file.Path;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ClientBackpacked
{
    public ClientBackpacked(IEventBus bus)
    {
        bus.addListener(this::onClientSetup);
        bus.addListener(this::onRegisterClientLoaders);
        bus.addListener(this::onRegisterMenuScreens);
        bus.addListener(this::onRegisterRenderers);
        bus.addListener(this::onAddLayers);
        bus.addListener(this::onRegisterAdditionalModels);
        bus.addListener(this::onFindPacks);
        bus.addListener(this::onRegisterClientExtensions);
        bus.addListener(this::onRegisterTooltipComponents);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            ClientBootstrap.init();
            registerBedAugmentModelOverride(ModItems.BACKPACK.get());
            registerBedAugmentModelOverride(ModItems.COPPER_BACKPACK.get());
            registerBedAugmentModelOverride(ModItems.GOLD_BACKPACK.get());
            registerBedAugmentModelOverride(ModItems.DIAMOND_BACKPACK.get());
            registerBedAugmentModelOverride(ModItems.NETHERITE_BACKPACK.get());
            registerFluidTankAugmentModelOverride(ModItems.BACKPACK.get());
            registerFluidTankAugmentModelOverride(ModItems.COPPER_BACKPACK.get());
            registerFluidTankAugmentModelOverride(ModItems.GOLD_BACKPACK.get());
            registerFluidTankAugmentModelOverride(ModItems.DIAMOND_BACKPACK.get());
            registerFluidTankAugmentModelOverride(ModItems.NETHERITE_BACKPACK.get());
            // The straw bed texture has hard (alpha 0/255, no partial values) transparent pixels
            // around the mattress silhouette - the default solid render layer ignores alpha
            // entirely and shows the raw (black) RGB underneath, so it needs cutout instead.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HAY_BED.get(), RenderType.cutout());
            // Same issue for the hollow logs' moss/snow overlay layer, which is hard-edged
            // transparency baked into the same log texture.
            for(Block hollowLog : new Block[]{
                ModBlocks.OAK_HOLLOW_LOG.get(), ModBlocks.STRIPPED_OAK_HOLLOW_LOG.get(),
                ModBlocks.SPRUCE_HOLLOW_LOG.get(), ModBlocks.STRIPPED_SPRUCE_HOLLOW_LOG.get(),
                ModBlocks.BIRCH_HOLLOW_LOG.get(), ModBlocks.STRIPPED_BIRCH_HOLLOW_LOG.get(),
                ModBlocks.JUNGLE_HOLLOW_LOG.get(), ModBlocks.STRIPPED_JUNGLE_HOLLOW_LOG.get(),
                ModBlocks.DARK_OAK_HOLLOW_LOG.get(), ModBlocks.STRIPPED_DARK_OAK_HOLLOW_LOG.get(),
                ModBlocks.ACACIA_HOLLOW_LOG.get(), ModBlocks.STRIPPED_ACACIA_HOLLOW_LOG.get(),
                ModBlocks.CHERRY_HOLLOW_LOG.get(), ModBlocks.STRIPPED_CHERRY_HOLLOW_LOG.get(),
                ModBlocks.CRIMSON_HOLLOW_STEM.get(), ModBlocks.STRIPPED_CRIMSON_HOLLOW_STEM.get(),
                ModBlocks.WARPED_HOLLOW_STEM.get(), ModBlocks.STRIPPED_WARPED_HOLLOW_STEM.get(),
                ModBlocks.MANGROVE_HOLLOW_LOG.get(), ModBlocks.STRIPPED_MANGROVE_HOLLOW_LOG.get()
            })
            {
                ItemBlockRenderTypes.setRenderLayer(hollowLog, RenderType.cutout());
            }
            if(!FMLLoader.isProduction()) {
                NeoForge.EVENT_BUS.register(new PickpocketDebugRenderer());
            }
        });
    }

    /**
     * Switches a backpack item's model to its "_with_bed" variant (see models/item/*_with_bed.json)
     * whenever the Bed augment is installed, so the bed attachment shows on the inventory icon, held
     * in hand, and dropped on the ground - not just worn on the body (BackpackLayer) or sitting on a
     * shelf (ShelfRenderer), which render from their own ItemStack directly. {@code ItemProperties}'
     * {@code ItemPropertyFunction} overload is a NeoForge addition not visible from the vanilla-only
     * {@code common} classpath, so this has to live here.
     */
    private static void registerBedAugmentModelOverride(Item item)
    {
        ItemProperties.register(item, Utils.rl("bed_augment"), (stack, level, entity, seed) ->
            BackpackHelper.findAugment(stack, ModAugmentTypes.BED.get()) != null ? 1.0F : 0.0F);
    }

    /**
     * Same purpose as {@link #registerBedAugmentModelOverride}, but for the Fluid Tank augment's
     * "_with_fluid_tank" model variant.
     */
    private static void registerFluidTankAugmentModelOverride(Item item)
    {
        ItemProperties.register(item, Utils.rl("fluid_tank_augment"), (stack, level, entity, seed) ->
            BackpackHelper.findAugment(stack, ModAugmentTypes.FLUID_TANK.get()) != null ? 1.0F : 0.0F);
    }

    private void onRegisterClientLoaders(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(new ModelMetaLoader());
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModContainers.BACKPACK.get(), BackpackScreen::new);
        event.register(ModContainers.BACKPACK_SHELF.get(), BackpackShelfScreen::new);
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ModBlockEntities.BACKPACK_SHELF.get(), ShelfRenderer::new);
    }

    private void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        addBackpackLayer(event.getSkin(PlayerSkin.Model.WIDE), event.getContext().getItemRenderer());
        addBackpackLayer(event.getSkin(PlayerSkin.Model.SLIM), event.getContext().getItemRenderer());

        EntityRenderer<?> renderer = event.getRenderer(EntityType.WANDERING_TRADER);
        if(renderer instanceof WanderingTraderRenderer traderRenderer)
        {
            traderRenderer.addLayer(new VillagerBackpackLayer<>(traderRenderer, event.getContext().getItemRenderer()));
        }
    }

    private static void addBackpackLayer(EntityRenderer<?> renderer, ItemRenderer itemRenderer)
    {
        if(renderer instanceof PlayerRenderer playerRenderer)
        {
            playerRenderer.addLayer(new BackpackLayer<>(playerRenderer, itemRenderer));
        }
    }

    private void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event)
    {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> models = manager.listResources("models/wanderersbackpack", location -> location.getPath().endsWith(".json"));
        models.forEach((key, resource) -> {
            String path = key.getPath().substring("models/".length(), key.getPath().length() - ".json".length());
            ModelResourceLocation location = FrameworkClientAPI.createModelResourceLocation(key.getNamespace(), path);
            event.register(location);
        });
    }

    private void onFindPacks(AddPackFindersEvent event)
    {
        // Search the resource packs folder for any backpacked addons. This makes it compatible with CurseForge modpacks.
        if(event.getPackType() == PackType.SERVER_DATA)
        {
            Path gameDir = FMLLoader.getGamePath();
            Path addonDir = gameDir.resolve("resourcepacks");
            DirectoryValidator directoryValidator = LevelStorageSource.parseValidator(gameDir.resolve("allowed_symlinks.txt"));
            event.addRepositorySource(new AddonRepositorySource(addonDir, PackType.SERVER_DATA, PackSource.FEATURE, directoryValidator));
        }
    }

    private void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;

        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.level == null)
            return;

        if(!mc.options.getCameraType().isFirstPerson())
            return;

        PoseStack stack = event.getPoseStack();
        MultiBufferSource source = mc.renderBuffers().bufferSource();
        boolean frozen = mc.level.tickRateManager().isEntityFrozen(mc.player);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(!frozen);
        FirstPersonEffectsRenderer.draw(mc.player, stack, source, partialTick);
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
    {
        // Backpack items now use plain vanilla item models (see models/item/*.json) instead of the
        // BackpackItemSpecialRenderer + cosmetic-style DSL, so no custom client extensions are needed.
    }

    private void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(BackpackContentsTooltip.class, BackpackContentsTooltipRenderer::new);
    }
}

package com.mrcrayfish.backpacked.client.gui.screen.inventory;

import com.mrcrayfish.backpacked.Config;
import com.mrcrayfish.backpacked.client.Icons;
import com.mrcrayfish.backpacked.client.Keys;
import com.mrcrayfish.backpacked.client.augment.AugmentHolder;
import com.mrcrayfish.backpacked.client.augment.AugmentSettingsFactories;
import com.mrcrayfish.backpacked.client.gui.MouseRestorer;
import com.mrcrayfish.backpacked.client.gui.screen.widget.*;
import com.mrcrayfish.backpacked.client.gui.screen.widget.popup.TextInputMenu;
import com.mrcrayfish.backpacked.common.ItemSorting;
import com.mrcrayfish.backpacked.common.Pagination;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.common.augment.Augments;
import com.mrcrayfish.backpacked.common.augment.impl.BedAugment;
import com.mrcrayfish.backpacked.common.augment.impl.FluidTankAugment;
import com.mrcrayfish.backpacked.core.ModAugmentTypes;
import com.mrcrayfish.backpacked.inventory.container.BackpackContainerMenu;
import com.mrcrayfish.backpacked.network.Network;
import com.mrcrayfish.backpacked.network.message.*;
import com.mrcrayfish.backpacked.platform.ClientServices;
import com.mrcrayfish.backpacked.platform.Services;
import com.mrcrayfish.backpacked.util.ScreenUtil;
import com.mrcrayfish.backpacked.util.Utils;
import com.mrcrayfish.framework.api.client.screen.widget.FrameworkButton;
import com.mrcrayfish.framework.api.client.screen.widget.element.Icon;
import com.mrcrayfish.framework.api.client.screen.widget.element.Sound;
import com.mrcrayfish.framework.api.client.screen.widget.input.Action;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class BackpackScreen extends UnlockableContainerScreen<BackpackContainerMenu>
{
    private static final Component CONFIG_TOOLTIP = Component.translatable("wanderersbackpack.button.config.tooltip");
    private static final Component CONFIGURE = Component.translatable("wanderersbackpack.gui.configure");
    private static final Component RENAME = Component.translatable("wanderersbackpack.gui.rename");
    private static final Component SORT = Component.translatable("wanderersbackpack.gui.sort");
    private static final Component BED = Component.translatable("wanderersbackpack.gui.bed");
    private static final Component BED_TOOLTIP = Component.translatable("wanderersbackpack.gui.bed.tooltip");
    private static final Component FLUID_TANK = Component.translatable("wanderersbackpack.gui.fluid_tank");
    private static final Component FLUID_TANK_EMPTY = Component.translatable("wanderersbackpack.gui.fluid_tank_empty").withStyle(ChatFormatting.GRAY);

    private static final WidgetSprites AUGMENT_TOGGLE_SPRITES = new WidgetSprites(
        Utils.rl("backpack/augment_toggle_on"),
        Utils.rl("backpack/augment_toggle_off"),
        Utils.rl("backpack/augment_toggle_on_focused"),
        Utils.rl("backpack/augment_toggle_off_focused")
    );
    private static final WidgetSprites AUGMENT_SETTINGS_SPRITES = new WidgetSprites(
        Utils.rl("backpack/augment_settings"),
        Utils.rl("backpack/augment_settings_disabled"),
        Utils.rl("backpack/augment_settings_focused")
    );

    private static final ResourceLocation BACKPACK_BACKGROUND = Utils.rl("backpack/background");
    private static final ResourceLocation BACKPACK_SLOT = Utils.rl("backpack/slot");
    private static final ResourceLocation INVENTORY_SPRITE = Utils.rl("backpack/inventory");
    private static final ResourceLocation INVENTORY_SLOT = Utils.rl("backpack/inventory_slot");
    private static final ResourceLocation LABEL_BACKGROUND = Utils.rl("backpack/label");
    private static final ResourceLocation ICON_CONFIG = Utils.rl("backpack/config");
    private static final ResourceLocation ICON_PREVIOUS = Utils.rl("backpack/previous");
    private static final ResourceLocation ICON_NEXT = Utils.rl("backpack/next");
    private static final ResourceLocation ICON_RENAME = Utils.rl("backpack/rename");
    private static final ResourceLocation ICON_SORT = Utils.rl("backpack/sort");
    private static final ResourceLocation ICON_BED = Utils.rl("backpack/bed");
    private static final ResourceLocation CHECKERS = Utils.rl("backpack/checkers");

    private static final int TITLE_LABEL_WIDTH = 110;
    private static final int TITLE_PADDING = 5;
    private static final int BACKPACK_TOP = 16;
    private static final int BACKPACK_PADDING_TOP = 11;
    private static final int BACKPACK_PADDING_SIDE = 11;
    private static final int BACKPACK_PADDING_BOTTOM = 14;
    private static final int GAP = 3;
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 101;
    public static final int LABEL_PADDING = 5;
    // Augment slot (18) + gap (2) + toggle/config button column (10)
    private static final int AUGMENT_PANEL_CONTENT_WIDTH = 30;
    // How far the quick-action buttons' label background overlaps into the main window's right
    // edge (buttonLeft = leftPos + imageWidth + 2, background starts at buttonLeft - LABEL_PADDING).
    private static final int MAIN_AREA_OVERLAP = 3;

    private static ItemSorting sorting = ItemSorting.ALPHABETICAL;

    private final Player player;
    private final int cols;
    private final int rows;
    private final boolean owner;
    private boolean opened;
    private int timer;
    private final List<Layout> layouts = new ArrayList<>();

    public BackpackScreen(BackpackContainerMenu menu, Inventory playerInventory, Component titleIn)
    {
        super(menu, playerInventory, titleIn);
        this.player = playerInventory.player;
        this.cols = menu.getCols();
        this.rows = menu.getRows();
        this.owner = menu.isOwner();
        this.imageWidth = BACKPACK_PADDING_SIDE + Math.max(this.cols, 9) * 18 + BACKPACK_PADDING_SIDE;
        this.imageHeight = BACKPACK_TOP + BACKPACK_PADDING_TOP + (this.rows * 18) + BACKPACK_PADDING_BOTTOM + GAP + INVENTORY_HEIGHT;
        this.titleLabelX = 6;
        this.titleLabelY = 0;
        this.inventoryLabelX = this.imageWidth / 2 - 80;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private int getAugmentPanelY()
    {
        int backpackHeight = BACKPACK_PADDING_TOP + (this.rows * 18) + BACKPACK_PADDING_BOTTOM;
        return BACKPACK_TOP + (backpackHeight - this.getAugmentPanelHeight()) / 2;
    }

    private int getAugmentPanelHeight()
    {
        int augmentSlots = this.menu.getAugmentSlots();
        int height = augmentSlots * 18 + (augmentSlots - 1) * BackpackContainerMenu.AUGMENT_SLOT_GAP;
        if(this.hasFluidTankAugment())
        {
            height += BackpackContainerMenu.AUGMENT_SLOT_GAP + 18;
        }
        return height;
    }

    private int getFluidTankSlotY()
    {
        int augmentSlots = this.menu.getAugmentSlots();
        return this.getAugmentPanelY() + augmentSlots * (18 + BackpackContainerMenu.AUGMENT_SLOT_GAP);
    }

    private boolean hasFluidTankAugment()
    {
        return this.owner && this.getFluidTankAugment().isPresent();
    }

    private Optional<FluidTankAugment> getFluidTankAugment()
    {
        for(Augments.Position position : Augments.Position.values())
        {
            Augments augments = this.menu.getAugments();
            if(augments.getState(position) && augments.getAugment(position) instanceof FluidTankAugment tank)
            {
                return Optional.of(tank);
            }
        }
        return Optional.empty();
    }

    @Override
    public void init()
    {
        MouseRestorer.loadCapturedPosition();

        super.init();

        if(!this.opened)
        {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.75F, 1.0F));
            this.opened = true;
        }

        this.layouts.clear();

        Layout titleLayout = this.createTitleLayout();
        titleLayout.arrangeElements();
        titleLayout.setPosition(this.leftPos + (this.imageWidth - titleLayout.getWidth()) / 2, this.topPos + LABEL_PADDING);
        titleLayout.visitWidgets(this::addRenderableWidget);
        this.layouts.add(titleLayout);

        LinearLayout actionsLayout = this.createActionsLayout();
        actionsLayout.arrangeElements();
        actionsLayout.visitWidgets(this::addRenderableWidget);
        int backpackHeight = BACKPACK_PADDING_TOP + (this.rows * 18) + BACKPACK_PADDING_BOTTOM;
        int buttonsHeight = LABEL_PADDING + actionsLayout.getHeight() + LABEL_PADDING;
        int buttonLeft = this.leftPos + this.imageWidth + 2;
        if(buttonsHeight > backpackHeight - LABEL_PADDING * 2) // TODO temporary
        {
            buttonLeft += 6;
        }
        int buttonTop = this.topPos + BACKPACK_TOP + (backpackHeight - buttonsHeight) / 2 + LABEL_PADDING;
        actionsLayout.setPosition(buttonLeft, buttonTop);
        this.layouts.add(actionsLayout);

        Pagination pagination = this.menu.getPagination();
        if(pagination.totalPages() > 1)
        {
            Tooltip navigateTooltip = Tooltip.create(
                Component.literal(Integer.toString(pagination.currentPage()))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                    .append(Integer.toString(pagination.totalPages()))
            );

            MiniButton navPrevious = this.addRenderableWidget(new MiniButton(titleLayout.getX() - 12 - LABEL_PADDING - 2, this.topPos + 3, 12, 12, ICON_PREVIOUS, onPress -> {
                pagination.previousPage();
            }));
            navPrevious.setTooltip(navigateTooltip);
            navPrevious.active = pagination.currentPage() > 1;

            MiniButton navNext = this.addRenderableWidget(new MiniButton(titleLayout.getX() + titleLayout.getWidth() + LABEL_PADDING + 2, this.topPos + 3, 12, 12, ICON_NEXT, onPress -> {
                pagination.nextPage();
            }));
            navNext.setTooltip(navigateTooltip);
            navNext.active = pagination.currentPage() < pagination.totalPages();
        }

        this.createAugmentButtons();
    }

    private void createAugmentButtons()
    {
        if(!this.owner)
            return;

        int augmentPanelY = this.getAugmentPanelY();
        Augments.Position[] positions = Augments.Position.values();
        for(int i = 0; i < this.menu.getAugmentSlots(); i++)
        {
            Augments.Position position = positions[i];
            int x = this.leftPos + BackpackContainerMenu.AUGMENT_BUTTONS_X;
            int y = this.topPos + augmentPanelY + i * (18 + BackpackContainerMenu.AUGMENT_SLOT_GAP);

            this.addRenderableWidget(FrameworkButton.builder()
                .setPosition(x, y)
                .setSize(10, 10)
                .noTexture()
                .setIcon(btn -> () -> AUGMENT_TOGGLE_SPRITES.get(this.menu.getAugments().getState(position), btn.isHovered() && btn.isActive()), 10, 10)
                .setDependent(() -> this.hasAugmentAt(position))
                .setAction(btn -> {
                    boolean newValue = !this.menu.getAugments().getState(position);
                    this.menu.setAugments(this.menu.getAugments().setState(position, newValue));
                    Network.getPlay().sendToServer(new MessageSetAugmentState(position, newValue));
                    this.rebuildWidgets();
                })
                .build());

            this.addRenderableWidget(FrameworkButton.builder()
                .setPosition(x, y + 9)
                .setSize(10, 10)
                .setTexture(AUGMENT_SETTINGS_SPRITES)
                .setTooltip(btn -> Tooltip.create(CONFIGURE))
                .setTooltipDelay(0)
                .setTooltipOptions(TooltipOptions.DISABLE_TOOLTIP_WHEN_WIDGET_INACTIVE)
                .setAction(btn -> {
                    Augment<?> augment = this.menu.getAugments().getAugment(position);
                    var factory = AugmentSettingsFactories.getFactory(augment);
                    if(factory == null)
                        return;
                    AugmentHolder<Augment<?>> holder = new AugmentHolder<>(() -> this.menu.getAugments().getAugment(position), updatedAugment -> {
                        Network.getPlay().sendToServer(new MessageUpdateAugment(position, updatedAugment));
                        this.menu.setAugments(this.menu.getAugments().setAugment(position, updatedAugment));
                    }, position, this.menu.getBackpackIndex());
                    factory.apply(this, holder).show(btn);
                })
                .setDependent(() -> {
                    if(!this.hasAugmentAt(position))
                        return false;
                    AugmentType<?> type = this.menu.getAugments().getAugment(position).type();
                    return AugmentSettingsFactories.hasFactory(type);
                })
                .build());
        }
    }

    // The physical AugmentItem in the slot is the source of truth for "is an augment placed here" -
    // the cached menu.getAugments() can briefly lag behind right after a swap (see
    // BackpackContainerMenu#broadcastChanges), so the buttons' active-state is driven by the slot.
    private boolean hasAugmentAt(Augments.Position position)
    {
        int index = this.rows * this.cols + position.ordinal();
        return index < this.menu.slots.size() && !this.menu.getSlot(index).getItem().isEmpty();
    }

    public void updateAugment(Augments.Position position, Augment<?> augment, boolean state)
    {
        this.menu.setAugments(this.menu.getAugments().setAugment(position, augment).setState(position, state));
        // The actions layout (e.g. the Bed augment's button) is only built in init() - without a
        // rebuild here, placing/removing a physical augment item leaves it stale until the player
        // closes and reopens the backpack.
        this.rebuildWidgets();
    }

    private Layout createTitleLayout()
    {
        LinearLayout layout = LinearLayout.horizontal().spacing(2);
        TitleWidget title = new TitleWidget(this.getTrimmedTitle(), this.title, Minecraft.getInstance().font);
        title.setWidth(TITLE_LABEL_WIDTH);
        layout.addChild(title);
        if(this.owner)
        {
            title.setShift(6);
            layout.addChild(new MiniButton(0, 0, ICON_RENAME, onPress -> {
                new TextInputMenu(this, this.title.getString(), 50, s -> {
                    Network.PLAY.sendToServer(new MessageRenameBackpack(s));
                }).show(this.getRectangle());
            })).setTooltip(Tooltip.create(RENAME));
        }
        return layout;
    }

    private LinearLayout createActionsLayout()
    {
        LinearLayout layout = LinearLayout.vertical().spacing(2);

        layout.addChild(FrameworkButton.builder()
            .setSize(10, 10)
            .setIcon(ICON_SORT, 10, 10)
            .setTooltipDelay(0)
            .setTooltip(btn -> ScreenUtil.createMultilineTooltip(List.of(
                SORT, sorting.label().plainCopy().withStyle(ChatFormatting.BLUE),
                Component.translatable("wanderersbackpack.gui.cycle_sort_mode", ScreenUtil.getIconComponent(Icons.MIDDLE_MOUSE)).withStyle(ChatFormatting.DARK_GRAY)
            )))
            .setPrimaryAction(btn -> {
                Network.getPlay().sendToServer(new MessageSortBackpack(sorting));
            })
            .setTertiaryAction(Action.create(btn -> {
                ItemSorting[] values = ItemSorting.values();
                sorting = values[(sorting.ordinal() + 1) % values.length];
            }, Sound.create(SoundEvents.WOODEN_BUTTON_CLICK_ON)))
            .setContentRenderer((btn, graphics, mouseX, mouseY, partialTick) -> {
                Icon icon = btn.getIcon();
                if(icon != null) {
                    icon.draw(graphics, btn.getX(), btn.getY(), partialTick);
                }
                if(btn.isHovered() && btn.isActive()) {
                    graphics.fillGradient(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), -2130706433, -2130706433);
                }
            }).build());

        if(this.hasBedAugment())
        {
            layout.addChild(FrameworkButton.builder()
                .setSize(10, 10)
                .setIcon(ICON_BED, 10, 10)
                .setTooltipDelay(0)
                .setTooltip(btn -> ScreenUtil.createMultilineTooltip(List.of(BED, BED_TOOLTIP.plainCopy().withStyle(ChatFormatting.DARK_GRAY))))
                .setPrimaryAction(btn -> {
                    Network.getPlay().sendToServer(new MessageUseBedAugment());
                })
                .setContentRenderer((btn, graphics, mouseX, mouseY, partialTick) -> {
                    Icon icon = btn.getIcon();
                    if(icon != null) {
                        icon.draw(graphics, btn.getX(), btn.getY(), partialTick);
                    }
                    if(btn.isHovered() && btn.isActive()) {
                        graphics.fillGradient(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), -2130706433, -2130706433);
                    }
                }).build());
        }

        if(!Config.CLIENT.hideConfigButton.get())
            layout.addChild(Divider.horizontal(10).colour(0xFFE0CDB7));

        if(!Config.CLIENT.hideConfigButton.get())
        {
            MiniButton configButton = layout.addChild(new MiniButton(0, 0, ICON_CONFIG, onPress -> this.openConfigScreen()));
            configButton.setTooltip(Tooltip.create(CONFIG_TOOLTIP));
        }

        return layout;
    }

    private boolean hasBedAugment()
    {
        for(Augments.Position position : Augments.Position.values())
        {
            Augments augments = this.menu.getAugments();
            if(augments.getState(position) && augments.getAugment(position).type() == ModAugmentTypes.BED.get())
                return true;
        }
        return false;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // Only show the custom fluid-status tooltip while the tank slot is empty - if it's holding
        // a bucket, the vanilla per-slot item tooltip (drawn by super below) already covers it.
        Optional<FluidTankAugment> tank = this.getFluidTankAugment();
        if(tank.isPresent() && this.menu.getTankSlotItem().isEmpty())
        {
            int slotX = this.leftPos + BackpackContainerMenu.AUGMENT_PANEL_X;
            int slotY = this.topPos + this.getFluidTankSlotY();
            if(ScreenUtil.isPointInArea(mouseX, mouseY, slotX, slotY, 18, 18))
            {
                List<ClientTooltipComponent> components = new ArrayList<>();
                components.add(new ClientTextTooltip(FLUID_TANK.getVisualOrderText()));
                Component status = tank.get().fluid().flatMap(Services.PLATFORM::getBucketForFluid).<Component>map(item ->
                    Component.translatable("wanderersbackpack.gui.fluid_tank_amount", new ItemStack(item).getHoverName(), tank.get().amount(), FluidTankAugment.CAPACITY)
                ).orElse(FLUID_TANK_EMPTY);
                components.add(new ClientTextTooltip(status.getVisualOrderText()));
                ClientServices.CLIENT.drawTooltip(graphics, this.font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        this.drawBackgroundWindow(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, mouseX, mouseY);
    }

    private FormattedCharSequence getTrimmedTitle()
    {
        int maxWidth = TITLE_LABEL_WIDTH - TITLE_PADDING * 2;
        if(this.font.width(this.title) > maxWidth)
        {
            return Language.getInstance().getVisualOrder(FormattedText.composite(this.font.substrByWidth(this.title, maxWidth - this.font.width("...")), FormattedText.of("...")));
        }
        return this.title.getVisualOrderText();
    }

    private void drawBackgroundWindow(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY)
    {
        //graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFFFFFFF);

        // Draw the background labels for the quick action buttons and augment panel. The augment
        // panel's label is drawn here (behind the main backpack background, drawn next) rather
        // than after it, so it reads as sitting on the same layer as the other side panels instead
        // of overlapping on top of the main window's edge.
        this.layouts.forEach(layout -> {
            graphics.blitSprite(LABEL_BACKGROUND, layout.getX() - LABEL_PADDING, layout.getY() - LABEL_PADDING, LABEL_PADDING + layout.getWidth() + LABEL_PADDING, LABEL_PADDING + layout.getHeight() + LABEL_PADDING);
        });
        if(this.owner)
        {
            int augmentPanelY = this.getAugmentPanelY();
            int augmentPanelHeight = this.getAugmentPanelHeight();
            // Extends past the augment slots to overlap into the main window's left edge by the
            // same amount the quick-action buttons panel overlaps its right edge (see buttonLeft
            // above), so both side panels read as flush with the main backpack area.
            int augmentBackgroundX = x + BackpackContainerMenu.AUGMENT_PANEL_X - LABEL_PADDING;
            int augmentBackgroundWidth = (x + MAIN_AREA_OVERLAP) - augmentBackgroundX;
            graphics.blitSprite(LABEL_BACKGROUND, augmentBackgroundX, y + augmentPanelY - LABEL_PADDING, augmentBackgroundWidth, LABEL_PADDING + augmentPanelHeight + LABEL_PADDING);
        }

        // Backpack Inventory
        int backpackHeight = BACKPACK_PADDING_TOP + (this.rows * 18) + BACKPACK_PADDING_BOTTOM;
        graphics.blitSprite(BACKPACK_BACKGROUND, x, y + BACKPACK_TOP, width, backpackHeight);

        // Augment slots (physical items placed directly into these slots, with a toggle and config
        // button stacked to the right of each slot)
        if(this.owner)
        {
            int augmentPanelY = this.getAugmentPanelY();
            for(int i = 0; i < this.menu.getAugmentSlots(); i++)
            {
                int slotY = augmentPanelY + i * (18 + BackpackContainerMenu.AUGMENT_SLOT_GAP);
                graphics.blitSprite(BACKPACK_SLOT, x + BackpackContainerMenu.AUGMENT_PANEL_X, y + slotY, 18, 18);
            }
            if(this.hasFluidTankAugment())
            {
                graphics.blitSprite(BACKPACK_SLOT, x + BackpackContainerMenu.AUGMENT_PANEL_X, y + this.getFluidTankSlotY(), 18, 18);
            }
        }

        // Draw Backpack Slots
        int backpackSlotsWidth = this.cols * 18;
        int backpackSlotsHeight = this.rows * 18;
        int backpackSlotsX = (width - backpackSlotsWidth) / 2;
        int backpackSlotsY = 27;
        graphics.blitSprite(BACKPACK_SLOT, x + backpackSlotsX, y + backpackSlotsY, backpackSlotsWidth, backpackSlotsHeight);

        int backpackCheckersWidth = (width - 11 - 11 - backpackSlotsWidth) / 2 - 3;
        if(backpackCheckersWidth > 0)
        {
            graphics.blitSprite(CHECKERS, x + 11, y + 27, backpackCheckersWidth, backpackSlotsHeight);
            graphics.blitSprite(CHECKERS, x + backpackSlotsX + backpackSlotsWidth + 3, y + 27, backpackCheckersWidth, backpackSlotsHeight);
        }

        // Player Inventory
        int inventoryX = (width - INVENTORY_WIDTH) / 2;
        int inventoryY = BACKPACK_TOP + backpackHeight + GAP;
        graphics.blitSprite(INVENTORY_SPRITE, x + inventoryX, y + inventoryY, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        // Draw Player Inventory Slots
        int inventorySlotsWidth = 9 * 18;
        int inventorySlotsHeight = 3 * 18;
        int inventorySlotsX = (width - inventorySlotsWidth) / 2;
        int inventorySlotsY = inventoryY + 18;
        graphics.blitSprite(INVENTORY_SLOT, x + inventorySlotsX, y + inventorySlotsY, inventorySlotsWidth, inventorySlotsHeight);
        graphics.blitSprite(INVENTORY_SLOT, x + inventorySlotsX, y + inventorySlotsY + inventorySlotsHeight + 4, 9 * 18, 18);
    }

    private void openConfigScreen()
    {
        ClientServices.CLIENT.openConfigScreen();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int action)
    {
        if(!this.hasPopupMenu() && Keys.KEY_BACKPACK.matches(key, scanCode))
        {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, action);
    }

    @Override
    public void removed()
    {
        super.removed();
        MouseRestorer.capturePosition();
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button)
    {
        for(Layout layout : this.layouts)
        {
            if(ScreenUtil.isPointInArea((int) mouseX, (int) mouseY, layout.getX() - LABEL_PADDING, layout.getY() - LABEL_PADDING, LABEL_PADDING + layout.getWidth() + LABEL_PADDING, LABEL_PADDING + layout.getHeight() + LABEL_PADDING))
            {
                return false;
            }
        }

        int backpackX = this.leftPos;
        int backpackY = this.topPos + BACKPACK_TOP;
        int backpackWidth = this.imageWidth;
        int backpackHeight = BACKPACK_PADDING_TOP + (this.rows * 18) + BACKPACK_PADDING_BOTTOM;
        if(ScreenUtil.isPointInArea((int) mouseX, (int) mouseY, backpackX, backpackY, backpackWidth, backpackHeight))
        {
            return false;
        }

        if(this.owner && ScreenUtil.isPointInArea((int) mouseX, (int) mouseY, this.leftPos + BackpackContainerMenu.AUGMENT_PANEL_X - LABEL_PADDING, this.topPos + this.getAugmentPanelY() - LABEL_PADDING, LABEL_PADDING + AUGMENT_PANEL_CONTENT_WIDTH + LABEL_PADDING, LABEL_PADDING + this.getAugmentPanelHeight() + LABEL_PADDING))
        {
            return false;
        }

        int inventoryX = this.leftPos + (this.imageWidth - INVENTORY_WIDTH) / 2;
        int inventoryY = this.topPos + BACKPACK_TOP + backpackHeight + GAP;
        if(ScreenUtil.isPointInArea((int) mouseX, (int) mouseY, inventoryX, inventoryY, INVENTORY_WIDTH, INVENTORY_HEIGHT))
        {
            return false;
        }

        return true;
    }

    // Note: For JEI extra areas
    public List<Layout> getLayouts()
    {
        return this.layouts;
    }
}

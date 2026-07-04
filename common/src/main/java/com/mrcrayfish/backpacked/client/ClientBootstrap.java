package com.mrcrayfish.backpacked.client;

import com.mrcrayfish.backpacked.client.augment.AugmentSettingsFactories;
import com.mrcrayfish.backpacked.client.augment.menu.FunnellingMenu;
import com.mrcrayfish.backpacked.client.augment.menu.ImmortalMenu;
import com.mrcrayfish.backpacked.client.augment.menu.LightweaverMenu;
import com.mrcrayfish.backpacked.client.augment.menu.RecallMenu;
import com.mrcrayfish.backpacked.client.renderer.backpack.DefaultRenderer;
import com.mrcrayfish.backpacked.client.renderer.backpack.RendererTypes;
import com.mrcrayfish.backpacked.client.renderer.backpack.advanced.AdvancedRenderer;
import com.mrcrayfish.backpacked.client.renderer.backpack.advanced.condition.*;
import com.mrcrayfish.backpacked.client.renderer.backpack.advanced.function.*;
import com.mrcrayfish.backpacked.client.renderer.backpack.advanced.value.*;
import com.mrcrayfish.backpacked.common.augment.impl.FunnellingAugment;
import com.mrcrayfish.backpacked.common.augment.impl.ImmortalAugment;
import com.mrcrayfish.backpacked.common.augment.impl.LightweaverAugment;
import com.mrcrayfish.backpacked.common.augment.impl.RecallAugment;
import com.mrcrayfish.framework.api.event.InputEvents;

/**
 * Author: MrCrayfish
 */
public class ClientBootstrap
{
    public static void earlyInit()
    {
        InputEvents.REGISTER_KEY_MAPPING.register(consumer -> {
            consumer.accept(Keys.KEY_BACKPACK);
        });
        RendererTypes.register(DefaultRenderer.TYPE);
        RendererTypes.register(AdvancedRenderer.TYPE);
        FunctionTypes.register(PushMatrixFunction.TYPE);
        FunctionTypes.register(TranslateMatrixFunction.TYPE);
        FunctionTypes.register(RotateMatrixFunction.TYPE);
        FunctionTypes.register(ScaleMatrixFunction.TYPE);
        FunctionTypes.register(DrawModelFunction.TYPE);
        FunctionTypes.register(SpawnParticleFunction.TYPE);
        FunctionTypes.register(ConditionalFunction.TYPE);
        ValueTypes.register(ConstantValue.TYPE);
        ValueTypes.register(TickCountValue.TYPE);
        ValueTypes.register(WaveformValue.TYPE);
        ValueTypes.register(AndValue.TYPE);
        ValueTypes.register(ExpressionValue.TYPE);
        ValueTypes.register(EntityDataValue.TYPE);
        ValueTypes.register(LevelDataValue.TYPE);
        ConditionTypes.register(AndCondition.TYPE);
        ConditionTypes.register(ManyCondition.TYPE);
        ConditionTypes.register(InvertedCondition.TYPE);
        ConditionTypes.register(TestValueCondition.TYPE);
    }

    public static void init()
    {
        ClientEvents.init();
        AugmentSettingsFactories.registerFactory(FunnellingAugment.TYPE, FunnellingMenu::new);
        AugmentSettingsFactories.registerFactory(ImmortalAugment.TYPE, ImmortalMenu::new);
        AugmentSettingsFactories.registerFactory(LightweaverAugment.TYPE, LightweaverMenu::new);
        AugmentSettingsFactories.registerFactory(RecallAugment.TYPE, RecallMenu::new);
    }
}

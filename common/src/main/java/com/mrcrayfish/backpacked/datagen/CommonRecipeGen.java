package com.mrcrayfish.backpacked.datagen;

import com.mrcrayfish.backpacked.core.ModItems;
import com.mrcrayfish.backpacked.util.Utils;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

/**
 * Author: MrCrayfish
 *
 * Note: This isn't authoritative at runtime (the mod ships hand-written recipe JSON under
 * data/backpacked/recipe instead), but is kept in sync for anyone who runs the data generator.
 */
public class CommonRecipeGen
{
    public static void generate(RecipeOutput output, Function<ItemLike, Criterion<?>> hasItem, Function<TagKey<Item>, Criterion<?>> hasTag, TagKey<Item> ironIngotTag, TagKey<Item> copperIngotTag, TagKey<Item> stringTag, TagKey<Item> stickTag, TagKey<Item> leatherTag)
    {
        backpack(output, hasTag, ironIngotTag, stringTag, leatherTag);
        backpackUpgrade(output, hasItem, ModItems.BACKPACK.get(), ModItems.COPPER_BACKPACK.get(), Items.COPPER_INGOT);
        backpackUpgrade(output, hasItem, ModItems.COPPER_BACKPACK.get(), ModItems.GOLD_BACKPACK.get(), Items.GOLD_INGOT);
        backpackUpgrade(output, hasItem, ModItems.GOLD_BACKPACK.get(), ModItems.DIAMOND_BACKPACK.get(), Items.DIAMOND);
        backpackNetheriteUpgrade(output, hasItem, ModItems.DIAMOND_BACKPACK.get(), ModItems.NETHERITE_BACKPACK.get());
    }

    private static void backpack(RecipeOutput output, Function<TagKey<Item>, Criterion<?>> hasTag, TagKey<Item> ironIngotTag, TagKey<Item> stringTag, TagKey<Item> leatherTag)
    {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BACKPACK.get())
                .pattern("SLS")
                .pattern("ICI")
                .pattern("LLL")
                .define('S', stringTag)
                .define('L', leatherTag)
                .define('I', ironIngotTag)
                .define('C', Items.CHEST)
                .unlockedBy("has_leather", hasTag.apply(leatherTag))
                .unlockedBy("has_string", hasTag.apply(stringTag))
                .unlockedBy("has_iron_ingot", hasTag.apply(ironIngotTag))
                .save(output);
    }

    private static void backpackUpgrade(RecipeOutput output, Function<ItemLike, Criterion<?>> hasItem, ItemLike previousTier, ItemLike result, ItemLike ingot)
    {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern(" I ")
                .pattern("IBI")
                .pattern(" I ")
                .define('I', ingot)
                .define('B', previousTier)
                .unlockedBy("has_backpack", hasItem.apply(previousTier))
                .save(output);
    }

    // Netherite tools/armor are always a smithing table upgrade (never a crafting-table recipe) -
    // matches that convention rather than the earlier tiers' crafting-table upgrade pattern.
    private static void backpackNetheriteUpgrade(RecipeOutput output, Function<ItemLike, Criterion<?>> hasItem, Item previousTier, Item result)
    {
        SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(previousTier),
                    Ingredient.of(Items.NETHERITE_INGOT),
                    RecipeCategory.TOOLS,
                    result
                )
                .unlocks("has_backpack", hasItem.apply(previousTier))
                .save(output, Utils.rl("netherite_backpack_smithing"));
    }
}

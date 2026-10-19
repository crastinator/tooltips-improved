package com.tooltipsimproved.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    /** Null until the recipe book has been opened once. */
    @Accessor("searchBox")
    @Nullable EditBox tooltipsimproved$getSearchBox();
}

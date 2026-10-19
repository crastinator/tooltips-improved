package com.tooltipsimproved;

import com.mojang.blaze3d.platform.InputConstants;
import com.tooltipsimproved.mixin.KeyMappingAccessor;
import com.tooltipsimproved.mixin.RecipeBookComponentAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Keybinds for moving tooltips. They are added to the vanilla list by {@link com.tooltipsimproved.mixin.OptionsMixin}
 * so no loader API is needed, and live in the vanilla Inventory category.
 */
public final class TooltipsImprovedKeys {
    //? if >=1.21.9 {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.INVENTORY;
    //?} else {
    /*private static final String CATEGORY = KeyMapping.CATEGORY_INVENTORY;
    *///?}
    //? if >=26.3 {
    private static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYBOARD;
    //?} else {
    /*private static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYSYM;
    *///?}

    public static final KeyMapping UP = create("up", InputConstants.KEY_PAGEUP);
    public static final KeyMapping DOWN = create("down", InputConstants.KEY_PAGEDOWN);
    /** Held with the wheel or Up/Down to move sideways. Not Shift, which expands tooltips, or Ctrl, which is Sprint. */
    public static final KeyMapping SIDEWAYS = create("sideways", InputConstants.KEY_LALT);
    public static final KeyMapping RESET = create("reset", InputConstants.UNKNOWN.getValue());

    public static final KeyMapping[] ALL = {UP, DOWN, SIDEWAYS, RESET};

    private static KeyMapping create(String name, int key) {
        return new KeyMapping("key." + TooltipsImproved.MOD_ID + "." + name, KEYBOARD, key, CATEGORY);
    }

    /**
     * Whether the bound key or mouse button is physically held. {@link KeyMapping#isDown()} doesn't
     * update while a screen is open, which is exactly when tooltips show.
     */
    public static boolean isHeld(KeyMapping mapping) {
        InputConstants.Key key = ((KeyMappingAccessor) mapping).tooltipsimproved$getKey();
        if (key.getType() == InputConstants.Type.MOUSE) return isMouseHeld(key.getValue());
        return key.getType() == KEYBOARD
            && key.getValue() != InputConstants.UNKNOWN.getValue()
            && isHeld(key.getValue());
    }

    /** Whether a text field on the open screen has focus, so key presses are typing. */
    public static boolean isTyping() {
        //? if >=26.1 {
        net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().gui.screen();
        //?} else {
        /*net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
        *///?}
        return screen != null && hasFocusedTextField(screen);
    }

    // Some screens focus a text field without making it the focused child, like the creative
    // inventory's search; the recipe book keeps its search box out of the widget tree entirely
    private static boolean hasFocusedTextField(net.minecraft.client.gui.components.events.GuiEventListener listener) {
        if (listener instanceof net.minecraft.client.gui.components.EditBox box) return box.isFocused();
        if (listener instanceof RecipeBookComponentAccessor recipeBook) return recipeBook.tooltipsimproved$getSearchBox() != null
            && recipeBook.tooltipsimproved$getSearchBox().isFocused();
        if (listener instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
            for (var child : container.children()) if (hasFocusedTextField(child)) return true;
        }
        return false;
    }

    private static boolean isMouseHeld(int button) {
        //? if >=26.3 {
        // SDL numbers buttons from 1, and its button state has one bit per button
        return button >= 1 && button <= 32
            && (org.lwjgl.sdl.SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, null) & (1 << (button - 1))) != 0;
        //?} elif >=1.21.9 {
        /*return org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        *///?} else {
        /*return org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        *///?}
    }

    public static boolean isHeld(int key) {
        //? if >=26.3 {
        return InputConstants.isKeyDown(key);
        //?} elif >=1.21.9 {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        *///?} else {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
        *///?}
    }
}

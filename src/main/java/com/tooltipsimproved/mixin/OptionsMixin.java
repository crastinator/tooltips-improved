package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipsImprovedKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Adds the keybinds to the vanilla list right before saved bindings load, the same point
 * the loaders' own keybind APIs use. Hooked at the start of {@code load()} rather than where the
 * constructor calls it, as Forge's Mixin only allows constructor injections at the end.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "load()V", at = @At("HEAD"))
    private void tooltipsimproved$addKeyMappings(CallbackInfo ci) {
        // Options can be loaded again later
        if (Arrays.asList(keyMappings).contains(TooltipsImprovedKeys.UP)) return;
        KeyMapping[] mappings = Arrays.copyOf(keyMappings, keyMappings.length + TooltipsImprovedKeys.ALL.length);
        System.arraycopy(TooltipsImprovedKeys.ALL, 0, mappings, keyMappings.length, TooltipsImprovedKeys.ALL.length);
        keyMappings = mappings;
    }
}

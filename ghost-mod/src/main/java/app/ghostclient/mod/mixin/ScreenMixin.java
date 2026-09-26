package app.ghostclient.mod.mixin;

import app.ghostclient.mod.gui.FriendsHudOverlay;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handle clicks on chat notifications
 */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ghostclient$onNotificationClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) { // Left click
            try {
                FriendsHudOverlay.onChatNotificationClicked((int) mouseX, (int) mouseY);
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }
}

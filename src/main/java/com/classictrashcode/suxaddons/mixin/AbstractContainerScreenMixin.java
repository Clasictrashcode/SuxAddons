package com.classictrashcode.suxaddons.mixin;

import com.classictrashcode.suxaddons.client.utils.BazzarTracker.BazaarTrackerOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Inject(method = "render",at = @At("HEAD"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci){
        AbstractContainerScreen<?> screen =(AbstractContainerScreen<?>) (Object) this;
        BazaarTrackerOverlay.renderOverlay(guiGraphics,screen.width,screen.height,mouseX,mouseY);
    }
    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    private void onMouseclicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir){
        if (mouseButtonEvent.button() == 0){
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (BazaarTrackerOverlay.handleMouseClick(mouseX,mouseY)){
                cir.setReturnValue(true);
                return;
            }
        }
    }
    @Inject(method = "mouseScrolled",at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir){
        if (BazaarTrackerOverlay.handleMouseScroll(mouseX,mouseY,verticalAmount)){
            cir.setReturnValue(true);
            return;
        }
    }
}

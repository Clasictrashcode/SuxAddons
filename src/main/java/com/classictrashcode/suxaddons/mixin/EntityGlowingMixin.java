package com.classictrashcode.suxaddons.mixin;

import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.utils.TracerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowingMixin {
    @Shadow public abstract boolean isInvisible();

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (TracerRenderer.isEnabled() && ((Object)this) == TracerRenderer.currentTarget) {
            cir.setReturnValue(true);
            return;
        }
        Config config = ConfigManager.getConfig();
        if (((Object)this) instanceof Shulker){
            if (config.hunting.hideonLeafTracker.enabled && config.hunting.hideonLeafTracker.hideOnLeafsGlowing){
                cir.setReturnValue(true);
                return;
            }
        }
        if (((Object)this) instanceof Bat){
            if (config.hunting.cinderBatTracker.enabled && config.hunting.cinderBatTracker.cinderBatGlowing && !this.isInvisible()){
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void overrideTracerGlowColor(CallbackInfoReturnable<Integer> cir) {
        if (!TracerRenderer.isEnabled()) return;
        if (((Object)this) != TracerRenderer.currentTarget) return;
        var theme = ConfigManager.getConfig().guiTheme;
        cir.setReturnValue(theme.parseColor(theme.primaryColor));
    }
}

package com.classictrashcode.suxaddons.mixin;

import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowingMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Config config = ConfigManager.getConfig();
        if (((Object)this) instanceof Shulker){
            if (config.hunting.hideonLeafTracker.enabled && config.hunting.hideonLeafTracker.hideOnLeafsGlowing){
                cir.setReturnValue(true);
                return;
            }
        }
        if (((Object)this) instanceof Bat){
            if (config.hunting.cinderBatTracker.enabled && config.hunting.cinderBatTracker.cinderBatGlowing){
                cir.setReturnValue(true);
                return;
            }
        }
    }
}

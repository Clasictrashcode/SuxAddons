package com.classictrashcode.suxaddons.mixin;

import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowingMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (((Object)this) instanceof Shulker){
            if (ConfigManager.getConfig().hunting.hideonLeafTracker.hideOnLeafsGlowing){
                cir.setReturnValue(true);
                return;
            }
        }
    }
}

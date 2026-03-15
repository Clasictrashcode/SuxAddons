package com.classictrashcode.suxaddons.mixin;

import com.classictrashcode.suxaddons.client.utils.BazzarTracker.BazzarTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {
    @Inject(method = "clicked",at = @At("HEAD"))
    private void onClickSlot(int slotIndex, int button, ClickType clickType, Player player, CallbackInfo ci){
        BazzarTracker.onSlotClick(slotIndex);
    }
}

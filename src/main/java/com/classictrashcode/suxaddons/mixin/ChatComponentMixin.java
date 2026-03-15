package com.classictrashcode.suxaddons.mixin;

import com.classictrashcode.suxaddons.client.hunting.CinderBatTracker;
import com.classictrashcode.suxaddons.client.hunting.HideonLeafTracker;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onChatMessage(Component message, CallbackInfo ci){
        String raw = message.getString();
        String clean = raw.replaceAll("(?i)§[0-9A-FK-OR]", "");
        System.out.println("ChatHudMixin: "+clean);
        HideonLeafTracker.onMessage(clean);
        CinderBatTracker.onMessage(clean);
    }
}

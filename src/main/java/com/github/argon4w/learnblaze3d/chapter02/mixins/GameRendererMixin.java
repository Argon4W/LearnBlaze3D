package com.github.argon4w.learnblaze3d.chapter02.mixins;

import com.github.argon4w.learnblaze3d.chapter02.LearnBlaze3D;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	public void onSetup(CallbackInfo info) {
		LearnBlaze3D.setup();
	}

	@Inject(method = "render", at = @At("TAIL"))
	public void onLoop(CallbackInfo info) {
		LearnBlaze3D.loop();
	}

	@Inject(method = "close", at = @At("TAIL"))
	public void onClose(CallbackInfo ci) {
		LearnBlaze3D.close();
	}
}

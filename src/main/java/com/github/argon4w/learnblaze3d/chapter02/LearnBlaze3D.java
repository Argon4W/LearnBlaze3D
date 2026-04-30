package com.github.argon4w.learnblaze3d.chapter02;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// 指定MOD主类
// @Mod(LearnBlaze3D.MOD_ID)
public class LearnBlaze3D {

	public static final String MOD_ID = "learnblaze3d02"; // 指南参考 MOD 的 MOD ID, 作为标识符, 将他替换为你在 gradle.properties 或 neoforge.mods.toml 中指定的 MOD ID.
	public static final Logger LOGGER = LogUtils.getLogger(); // 指南参考 MOD 的 Logger, 作为 Log 输出.

	// 在 GameRenderer 初始化时调用, 用于初始化各种渲染资源, 仅调用一次.
	public static void setup() {
		LOGGER.info("LearnBlaze3D正在初始化.");
	}

	// 在 GameRenderer 每帧渲染后调用, 用于渲染实例参考代码.
	public static void loop() {

	}

	// 在 GameRenderer 终止 (如关闭游戏) 时调用, 用来清理各种渲染资源, 仅调用一次.
	public static void close() {
		LOGGER.info("LearnBlaze3D正在进行清理.");
	}
}

# LearnBlaze3D

自Mojang Studio (以下简称 Mojang) 于2025年2月26日发布 Minecraft Java Edition (以下简称 Minecraft) 25w09a 快照后,
Minecraft 正式进入了由新 Blaze3D 引擎驱动的渲染现代化的进程, 开始了从1.21.5至26.1的渲染底层大重构.

在26.1发布前, Mojang 公开了"[Another step towards Vibrant Visuals](https://www.minecraft.net/en-us/article/another-step-towards-vibrant-visuals-for-java-edition)"
这篇文章, 明确了未来将使用 Vulkan 作为现代化渲染后端的发展路线, 并在2026年4月7日发布的 Minecraft 26.2-snapshot-1 快照中将 Vulkan 后端加入新 Blaze3D.

这一连串举动令不少需要与在旧版本 Minecraft 中需要接触图形渲染 API 的 MOD 开发者 (以下简称Modders) 感到不知所措. 面对需要同时兼容 OpenGL 与全新的,
学习曲线更为陡峭的Vulkan API的移植压力, 确实会令人望而生畏.

但我们无需担心, Mojang 在公布使用 Vulkan 后提出"尽可能使用 Blaze3D 进行渲染" 的解决方案, Blaze3D 进行了**彻底的重写**, 变得更加通用与现代.
这允许 Modders 使用同一套渲染代码同时驱动 OpenGL 与 Vulkan 渲染, 是目前公认的最优解.

## 1. 介绍

LearnBlaze3D 旨在以尽可能简洁的篇幅带领有一定经验的 Modders 学习并使用新 Blaze3D 进行渲染, 帮助 Modders 迁移旧版本渲染代码至 Blaze3D API 下.
为了节省篇幅, 这篇指南不会面面俱到, 部分情况下仅会描述常见做法——至少是我认可的做法.

对于并不熟悉 Minecraft Modding 的玩家, 请先前往以下站点了解 Minecraft NeoForge Modding 的基本概念与流程.
本指南不会对 Modding 中的基本概念进行额外阐述.

1. [正山小种](https://www.teacon.cn/xiaozhong/): 由 TeaCon 执行委员会负责维护的模组开发指南.
2. [NeoForge Docs](https://docs.neoforged.net/): 由 NeoForge 官方团队维护的模组开发文档.

## 2. 指南及源代码

本指南及其源码均位于其 [GitHub 仓库](https://github.com/Argon4W/LearnBlaze3D) 下.

指南中出现的参考 Java 源代码可在本仓库根目录下 `src/main/java/` 目录中找到.

指南本身的 Markdown 源代码可在本仓库根目录下 `books/` 目录中找到.

## 4. 授权协议

[猫猫狐AR/Argon4W](https://github.com/Argon4W) 保留对 LearnBlaze3D 中所有文本的法定权利,
唯文中代码片段均以 [WTFPL 2.0 协议](http://www.wtfpl.net/about/) 开源.

以下为协议全文.

```text
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2022 Argon4W

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
```

## 5. 阅读准备

这篇指南基于 Minecraft 26.1.1 版本, 并基于 NeoForge 26.1.1.6-beta, 和 Java 25.0.2. 这篇指南假设读者拥有一定基于 Java 25 的开发经验, 一定量的 Minecraft 游玩与 MOD 开发经验, 和足够通畅的网络环境. 这篇指南同时假设读者拥有一台能够正常运行 Minecraft 26.1, 并拥有至少 4 GB 空闲内存的计算机.

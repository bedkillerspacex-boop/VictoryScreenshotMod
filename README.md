# VictoryScreenshotMod

一个简单的 Minecraft Fabric 模组，用于在游戏胜利时自动捕捉屏幕截图。

## 功能描述

*   **自动截图**：当检测到游戏胜利（如 Bedwars 胜利、Game Over 界面显示等）时，模组会自动触发截图功能。
*   **Fabric 支持**：专为 Fabric 加载器设计，适配最新版本的 Minecraft (1.21.4)。
*   **高效集成**：通过 Mixin 技术与游戏原生渲染层无缝集成。

## 安装步骤

1.  确保你已安装了 [Fabric Loader](https://fabricmc.net/)。
2.  下载本模组的 `.jar` 文件。
3.  将 `.jar` 文件放入 Minecraft 根目录下的 `mods` 文件夹中。
4.  启动游戏，尽情享受胜利时刻！

## 开发与构建

本项目使用 Gradle 进行管理。

### 环境要求
*   Java 21 或更高版本
*   Minecraft 1.21.4

### 构建指令
```bash
./gradlew build
```
构建完成后的文件位于 `build/libs` 目录下。

## 贡献

欢迎通过 Issue 或 Pull Request 提交建议与修复。

## 开源协议

本项目基于 MIT 协议开源。

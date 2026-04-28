# 鸿蒙MDM 项目

> 📁 独立项目目录，位于 `obsidian/鸿蒙MDM/`

## 基本信息

| 项目 | 内容 |
|------|------|
| 项目名 | mdm-enterprise-hos |
| 应用包名 | `com.siyu.mdm.enterprise.hos` |
| 目标平台 | HarmonyOS Next 6.0+ (API 14+) |
| 开发语言 | ArkTS (TypeScript 超集) |
| 状态 | **待开发** |

## 核心功能

- 设备远程锁定/解锁、恢复出厂设置、重启、重置密码
- 应用安装/卸载/黑名单管理
- 网络管控（WiFi、移动数据、蓝牙、GPS）
- 硬件外设限制（相机、麦克风、USB、截图）
- MQTT 长连接 + 智能心跳 + 自动重连

## 技术架构

- MQTT Broker: `tcp://192.168.1.199:1883`
- 通信方式: MQTT over TCP
- MDM Kit: `@kit.MDMKit` (EnterpriseAdminExtensionAbility)
- 后台服务: `@ohos.ability.backgroundTaskManager`

## 目录结构

```
entry/src/main/ets/
├── entryability/       # 应用入口 EntryAbility
├── service/            # MqttBackgroundService 后台长连接服务
├── mqtt/               # MqttClient / MqttConfig / ReconnectManager
├── command/            # CommandParser / CommandHandler / CommandResult
├── mdm/                # DeviceController / AppManager / RestrictionManager / InfoQuerier
├── battery/            # BatteryMonitor / HeartbeatAdjuster
├── network/            # NetworkMonitor
├── storage/            # ConfigStore
└── pages/              # Index / Settings
```

## 项目文件

```
鸿蒙MDM/
├── 归档/
│   ├── 01-技术架构文档.md
│   └── 02-技术设计书.md          # 接口定义、状态机、错误码
├── 03-软件说明文档.md
├── 04-操作手册.md
├── 05-源代码.md                  # ArkTS 示例源码
└── 06-源代码_混淆.md
```

## 下一步

- [ ] 创建 GitHub 仓库 `mdm-enterprise-hos`
- [ ] 初始化 DevEco Studio 项目，包名 `com.siyu.mdm.enterprise.hos`
- [ ] 引入 `@ohos/mqtt` 依赖
- [ ] 配置 `EnterpriseAdminExtensionAbility`
- [ ] 实现 MQTT 长连接服务
- [ ] 实现命令分发与执行
- [ ] 集成 MDM Kit 各模块

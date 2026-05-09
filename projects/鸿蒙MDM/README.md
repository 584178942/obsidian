# mdm-enterprise-hos

> 企业移动设备管理系统（鸿蒙 NEXT 6.0+）

**包名**：`com.siyu.mdm.enterprise.hos`  
**平台**：HarmonyOS Next 6.0+ (API 14+)  
**语言**：ArkTS  
**当前模式**：⚠️ Mock 模式（等待企业MDM证书）

---

## 项目状态

| 项目 | 状态 |
|------|------|
| MQTT 通信层 | ✅ 完整实现 |
| 命令处理系统 | ✅ 完整实现（Mock） |
| 后台服务 | ✅ 完整实现 |
| MDM Kit 集成 | ⏳ 待获取证书 |
| UI 界面 | ✅ 完整实现 |

**Mock 模式说明**：目前所有 MDM 命令均为模拟执行。如需启用真实 MDM 能力，需：
1. 向华为申请企业 MDM 证书
2. 配置签名证书（.p7b）
3. 将 `MdmApiFactory.ets` 中的 `USE_MOCK` 改为 `false`

---

## 目录结构

```
entry/src/main/ets/
├── entryability/
│   └── EntryAbility.ets               # 应用入口，初始化配置+启动后台服务
├── enterpriseadmin/
│   └── EnterpriseAdminAbility.ets     # 企业管理员扩展能力
├── entrybackupability/
│   └── EntryBackupAbility.ets         # 备份能力
├── service/
│   ├── index.ets
│   └── MqttBackgroundService.ets      # 后台长连接服务（核心）
├── mqtt/
│   ├── index.ets
│   ├── MqttClient.ets                 # MQTT客户端（连接/订阅/发布/重连）
│   ├── MqttConfig.ets                 # 配置参数
│   └── ReconnectManager.ets          # 智能重连策略
├── command/
│   ├── index.ets
│   ├── CommandParser.ets              # 命令消息解析
│   ├── CommandHandler.ets             # 命令分发与执行
│   └── CommandResult.ets             # 结果封装
├── mdm/
│   ├── index.ets
│   ├── IDeviceController.ets          # MDM能力接口定义
│   ├── MdmApiFactory.ets              # 工厂类（Mock/Real切换）
│   ├── DeviceControllerMock.ets      # 设备控制Mock
│   ├── AppManagerMock.ets             # 应用管理Mock
│   ├── RestrictionManagerMock.ets     # 限制策略Mock
│   └── InfoQuerierMock.ets            # 信息查询Mock
├── battery/
│   ├── index.ets
│   ├── BatteryMonitor.ets             # 电池状态监控
│   └── HeartbeatAdjuster.ets          # 动态心跳调整
├── network/
│   ├── index.ets
│   └── NetworkMonitor.ets            # 网络状态监控
├── storage/
│   └── ConfigStore.ets                # 配置持久化（Preferences）
└── pages/
    ├── Index.ets                      # 主页（设备状态+命令测试）
    └── Settings.ets                   # 设置页
```

---

## 技术参数

| 配置项 | 值 |
|--------|-----|
| MQTT Broker | `tcp://192.168.1.199:1883` |
| 客户端ID前缀 | `mdm_siyu_` |
| 心跳间隔（正常） | 120秒 |
| 心跳间隔（高性能） | 60秒 |
| 心跳间隔（省电） | 300秒 |
| 心跳间隔（极致省电） | 600秒 |
| 连接超时 | 30秒 |
| 保活间隔 | 60秒 |
| 遗嘱主题 | `mdm/report` |

---

## 支持的命令

**设备控制**：`lock` `unlock` `wipe` `reboot` `reset_password`

**应用管理**：`install_app` `uninstall_app` `get_app_list`

**限制策略**：`disable_wifi` `enable_wifi` `disable_mobile_data` `enable_mobile_data` `set_gps` `set_bluetooth` `disable_camera` `enable_camera` `disable_microphone` `enable_microphone` `disable_usb_debug` `enable_usb_debug` `disable_screenshot` `enable_screenshot`

**信息查询**：`get_device_info`

---

## 开发待办

- [ ] 申请企业 MDM 证书
- [ ] 配置签名证书 .p7b
- [ ] 切换 `USE_MOCK = false`
- [ ] 实现 `DeviceController`（真实华为 MDM Kit）
- [ ] 实现 `AppManager`（真实华为 MDM Kit）
- [ ] 实现 `RestrictionManager`（真实华为 MDM Kit）
- [ ] 实现 `InfoQuerier`（真实华为 MDM Kit）

---

## 构建与运行

```bash
# DevEco Studio 中打开项目
# 选择设备并运行
```

---

**GitHub**：https://github.com/584178942/mdm-enterprise-hos  
**维护者**：上海思御信息科技有限公司

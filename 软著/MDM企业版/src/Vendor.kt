package com.siyu.mdm.enterprise.util.mdm

/**
 * 设备厂商枚举
 * 用于自动检测当前设备厂商并选择对应的MDM实现
 */
enum class Vendor {
    /** 华为 */
    HUAWEI,

    /** 小米/红米 */
    XIAOMI,

    /** vivo */
    VIVO,

    /** 未知厂商 */
    UNKNOWN;

    companion object {
        /**
         * 根据厂商标识字符串获取Vendor枚举
         * @param manufacturer 厂商标识字符串（通常来自Build.MANUFACTURER）
         */
        fun fromString(manufacturer: String): Vendor {
            val lower = manufacturer.lowercase()
            return when {
                lower.contains("huawei") || lower.contains("honor") -> HUAWEI
                lower.contains("xiaomi") || lower.contains("redmi") -> XIAOMI
                lower.contains("vivo") -> VIVO
                else -> UNKNOWN
            }
        }
    }
}

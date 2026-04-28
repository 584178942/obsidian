package com.siyu.mdm.custom.device.json

import android.app.Application

/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



data class DeviceInfo(
    var model: String? = null,
    val permissions: MutableList<Int> = mutableListOf(),
    val applications: MutableList<Application> = mutableListOf(),
   // val files: MutableList<RemoteFile> = mutableListOf(),
    var deviceId: String? = null,
    var phone: String? = null,
    var imei: String? = null,
    var mdmMode: Boolean = false,
    var kioskMode: Boolean = false,
    var batteryLevel: Int = 0,
    var batteryCharging: String? = null,
    var androidVersion: String? = null,
    var factoryReset: Boolean? = null,
    var location: Location? = null,
    var launcherType: String? = null,
    var launcherPackage: String? = null,
    var isDefaultLauncher: Boolean = false,
    var iccid: String? = null,
    var imsi: String? = null,
    var phone2: String? = null,
    var imei2: String? = null,
    var iccid2: String? = null,
    var imsi2: String? = null,
    var cpu: String? = null,
    var serial: String? = null,
    // 自定义字段，用于Headwind MDM的定制版本
    var custom1: String? = null,
    var custom2: String? = null,
    var custom3: String? = null
) {
    data class Location(
        var ts: Long = 0,
        var lat: Double = 0.0,
        var lon: Double = 0.0
    )
}

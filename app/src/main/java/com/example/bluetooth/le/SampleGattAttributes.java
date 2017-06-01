/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.bluetooth.le;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    public static final String GAP_UUID="00001800-0000-1000-8000-00805f9b34fb";
    public static final String GATT_UUID="00001801-0000-1000-8000-00805f9b34fb";
    public static final String RX_UUID = "65786365-6c70-6f69-6e74-2e636f6d0001";
    public static final String TX_UUID = "65786365-6c70-6f69-6e74-2e636f6d0002";
    public static final String BLE_RX_TX_UUID = "65786365-6c70-6f69-6e74-2e636f6d0000";
    public static final String MANUFACTURE_SPECIFIC_DATA_CHAR_UUID="00002a01-0000-1000-8000-00805F9B34FB";

    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
//    public static String HEART_RATE_MEASUREMENT = "65786365-6c70-6f69-6e74-2e636f6d0000";

    public static String MY_FLIP4_SERVICE = "65786365-6c70-6f69-6e74-2e636f6d0000";

    static {
        // Sample Services.
        //attributes.put(GAP_UUID,"GAP Service");
        //attributes.put(GATT_UUID,"GATT Service");
        attributes.put(RX_UUID,"Receive Data for Device");
        attributes.put(TX_UUID,"Write Data to Device");
        attributes.put(BLE_RX_TX_UUID,"BLE Data Receive Send Service");
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

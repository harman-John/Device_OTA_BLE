package com.example.bluetooth.le;

/**
 * Created by Johngan on 26/05/2017.
 */

public class Util {

    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 5;

    public static String bytesToHexString(byte[] data){
        String result="";
        for (int i = 0; i < data.length; i++) {
            result+=Integer.toHexString((data[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3);
        }
        return result;
    }

}

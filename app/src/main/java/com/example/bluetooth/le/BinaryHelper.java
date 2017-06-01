/****************************************************
 * Created by Wu Beibei on 15/5/8.
 * Copyright (c) 2015年 Harman International Industries. All rights reserved.
 ****************************************************/
package com.example.bluetooth.le;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Created by insgupta020 on 8/11/2016.
 * Utility class to handle lower data conversion
 */
public class BinaryHelper {
	/**
	 * Function to convert integer to byte array
	 * @param num int
	 * @return byte[]
	 */
	public static  byte[] int2ByteArray(int num) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream oos;
		byte[] buffer = null;
		try {
			oos = new DataOutputStream(byteArrayOutputStream);
			oos.writeInt(num);
			oos.flush();
			oos.close();
			buffer = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return buffer;
	}

	public static void main(String[] args) {
		int a = 875684310;
		byte[] b =int2ByteArray(a);
		System.out.println(String.format("--0---> %x", b[0]));
		System.out.println(String.format("--1---> %x", b[1]));
		System.out.println(String.format("--2---> %x", b[2]));
		System.out.println(String.format("--3---> %x", b[3]));

	}
}

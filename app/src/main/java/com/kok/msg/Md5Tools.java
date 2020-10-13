package com.kok.msg;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Tools {
    /**
     * @param val 要加密的字符串
     * @return 加密后的byte数组
     * @throws NoSuchAlgorithmException 会抛出的异常
     * 使用md5加密的流程:
     * 1.通过MessageDigest获取一个MessageDigest对象
     * 2.通过MessageDigest对象的update方法把我们要加密的内容转换成为一个byte数组传进去
     * 3.通过MessageDigest对象的digest方法进行加密,返回的是加密后的byte数组
     *
     */
    public static String getMD5(String val) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(val.getBytes());
        byte[] m = md5.digest();// 加密
        return getString(m);
    }

    /**
     *
     * @param bytearray 加密后的byte数组
     * @return byte数组转换成一个16进制字符串
     *
     * toHexString方法是把int类型转换为16进制的字符串
     */
    private static String getString(byte[] bytearray) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytearray) {
            int i = (b & 0xFF);
            if (i < 0x10) sb.append('0');
            sb.append(Integer.toHexString(i));
        }
        return sb.toString();
    }


}

package me.hhhaiai.utils;

public class Texts {

    /**
     * 比较字符串是否相等
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.toString().equals(b.toString());
    }

    /**
     * 去除前后不可见符号
     * @param origin
     * @return
     */
    public static String trim(CharSequence origin) {
        return origin == null ? null : origin.toString().trim();
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || trim(str).length() == 0;
    }


}

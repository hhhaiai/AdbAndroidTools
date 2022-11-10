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
        return trim(a).equals(trim(b));
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


    /**
     * 判断origin是否以sub开始
     * @param origin 目标字段
     * @param sub 查找字段
     * @return
     */
    public static boolean startWith(CharSequence origin, CharSequence sub) {
        if (isEmpty(origin) || isEmpty(sub) || origin.length() < sub.length()) {
            return false;
        }

        /**
         * 比较前n个字符
         */
        for (int i = 0; i < sub.length(); i++) {
            if (origin.charAt(i) != sub.charAt(i)) {
                return false;
            }
        }

        return true;
    }

}

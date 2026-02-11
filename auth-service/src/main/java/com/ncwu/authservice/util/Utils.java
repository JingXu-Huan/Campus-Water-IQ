package com.ncwu.authservice.util;


import java.util.UUID;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/11
 */
public class Utils {
    public static String genUid(int type) {
        String prefix = "user_";
        switch (type) {
            case 1 -> prefix = "user_";
            case 2 -> prefix = "maintain";
            case 3 -> prefix = "root";
        }
        return prefix + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
    }
}

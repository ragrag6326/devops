package com.tkb.utils.Version;

public class VersionUtil {

    /**
     * 版本字串 +1
     * ex:
     * 1.0.20 → 1.0.21
     * 2.4.9 → 2.4.10
     * 3 → 4
     * 0.9 → 0.10
     */

    public static String plusOne(String version) {
        if (version == null || version.trim().isEmpty()) return "1.0.0";

        String[] arr = version.split("\\.");
        int last = Integer.parseInt(arr[arr.length - 1]);
        last++;

        arr[arr.length - 1] = String.valueOf(last);
        return String.join(".", arr);
    }

}

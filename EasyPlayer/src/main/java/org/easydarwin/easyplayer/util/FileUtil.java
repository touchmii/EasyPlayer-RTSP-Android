package org.easydarwin.easyplayer.util;

import android.os.Environment;

public class FileUtil {

    private static String path = Environment.getExternalStorageDirectory() +"/EasyPlayerRTSP";

    public static String getPicturePath(String url) {
        return path + "/" + urlDir(url) + "/picture";
    }

    public static String getMoviePath(String url) {
        return path + "/" + urlDir(url) + "/movie";
    }

    private static String urlDir(String url) {
        if (url == null) {
            return "";
        }

        url = url.replace("://", "");
        url = url.replace("/", "");
        url = url.replace(".", "");

        if (url.length() > 64) {
            url.substring(0, 63);
        }

        return url;
    }
}

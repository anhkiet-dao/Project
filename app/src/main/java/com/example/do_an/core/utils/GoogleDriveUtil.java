package com.example.do_an.core.util;

public class GoogleDriveUtil {

    public static String convertGoogleDriveUrl(String url) {
        if (!url.contains("drive.google.com")) {
            return url;
        }

        String fileId = "";
        if (url.contains("/d/")) {
            fileId = url.split("/d/")[1].split("/")[0];
        } else if (url.contains("id=")) {
            fileId = url.substring(url.indexOf("id=") + 3);
        }

        return fileId.isEmpty() ? url : "https://drive.google.com/uc?export=view&id=" + fileId;
    }
}

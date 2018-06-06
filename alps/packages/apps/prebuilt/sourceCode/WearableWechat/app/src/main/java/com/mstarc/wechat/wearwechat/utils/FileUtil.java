package com.mstarc.wechat.wearwechat.utils;

import java.io.File;

public class FileUtil {
    public static void createDir(String paramString) {
        File localFile = new File(paramString);
        if (!localFile.exists())
            localFile.mkdir();
    }
}


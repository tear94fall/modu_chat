package com.example.modumessenger.Global;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ScopedStorageUtil {

    private final List<String> tempFileList = new ArrayList<>();

    /**
     * Android Q (Android 10, API 29) upper need this (because Scoped Storage)
     */
    public String copyFromScopedStorage(Activity activity, Uri uri, String fileName) {
        final ContentResolver contentResolver = activity.getContentResolver();
        if (contentResolver == null)
            return null;

        String filePath = activity.getApplicationInfo().dataDir + File.separator + System.currentTimeMillis() + "." + fileName.substring(fileName.lastIndexOf(".") + 1);
        File file = new File(filePath);
        tempFileList.add(filePath);

        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null)
                return null;
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                outputStream.write(buf, 0, len);
            outputStream.close();
            inputStream.close();
        } catch (IOException ignore) {
            return null;
        }
        return file.getAbsolutePath();
    }

    public void deleteTempFiles() {
        for(int i=0; i<tempFileList.size(); i++) {
            File tempFile = new File(tempFileList.get(i));
            if(tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}

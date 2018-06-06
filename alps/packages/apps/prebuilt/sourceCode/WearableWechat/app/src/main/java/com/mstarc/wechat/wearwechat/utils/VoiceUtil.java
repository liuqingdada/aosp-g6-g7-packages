package com.mstarc.wechat.wearwechat.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VoiceUtil {
    public static int BUFFER_SIZE = 2048;
    public static int CHUNK_LENGTH = 2048;
    public static String SPEECH_TO_TEXT;
    private static String TAG = "VoiceUtil";

    static {
        SPEECH_TO_TEXT = "http://streaming.mobvoi.com/speech2text";
    }

    public static void fromInStreamToOutStream(InputStream paramInputStream, OutputStream paramOutputStream)
            throws IOException {
        if (paramOutputStream == null)
            throw new IllegalArgumentException("Output stream may not be null");
        try {
            byte[] arrayOfByte = new byte[BUFFER_SIZE];
            while (true) {
                int i = paramInputStream.read(arrayOfByte);
                if (i == -1)
                    break;
                paramOutputStream.write(arrayOfByte, 0, i);
            }
        } finally {
            paramInputStream.close();
        }
        paramInputStream.close();
    }

    public static String readFromStream(InputStream paramInputStream, String paramString)
            throws IOException {
        StringBuffer localStringBuffer;
        try {
            InputStreamReader localInputStreamReader = new InputStreamReader(paramInputStream, paramString);
            localStringBuffer = new StringBuffer();
            char[] arrayOfChar = new char[1024];
            while (true) {
                int i = localInputStreamReader.read(arrayOfChar);
                if (i == -1)
                    break;
                localStringBuffer.append(arrayOfChar, 0, i);
            }
        } finally {
            paramInputStream.close();
        }
        String str = localStringBuffer.toString();
        paramInputStream.close();
        return str;
    }


    private class VoiceTask extends AsyncTask<String, Void, String> {
        private VoiceTask() {
        }

        protected String doInBackground(String[] paramArrayOfString) {
            String str;
            try {
                FileInputStream localFileInputStream = new FileInputStream(paramArrayOfString[0]);
                HttpURLConnection localHttpURLConnection = (HttpURLConnection) new URL(VoiceUtil.SPEECH_TO_TEXT).openConnection();
                localHttpURLConnection.setRequestMethod("POST");
                localHttpURLConnection.setRequestProperty("Content-Type", "audio/AMR");
                localHttpURLConnection.setConnectTimeout(6000);
                localHttpURLConnection.setDoInput(true);
                localHttpURLConnection.setDoOutput(true);
                localHttpURLConnection.setUseCaches(false);
                localHttpURLConnection.setChunkedStreamingMode(VoiceUtil.CHUNK_LENGTH);
                localHttpURLConnection.connect();
                OutputStream localOutputStream = localHttpURLConnection.getOutputStream();
                VoiceUtil.fromInStreamToOutStream(localFileInputStream, localOutputStream);
                localOutputStream.close();
                int i = localHttpURLConnection.getResponseCode();
                if (i >= 400) ;
                InputStream localInputStream  = localHttpURLConnection.getInputStream();;
                for (Object localObject = localHttpURLConnection.getErrorStream(); ; localObject = localInputStream) {
                    str = VoiceUtil.readFromStream((InputStream) localObject, "utf-8");
                    Log.d(VoiceUtil.TAG, "voice2text, response=" + str);
                    if (i < 400)
                        break;
                    return "";

                }
            } catch (Exception localException) {
                Log.w(VoiceUtil.TAG, localException);
                str = "";
            }
            return str;
        }

        protected void onPostExecute(String paramString) {
        }
    }
}


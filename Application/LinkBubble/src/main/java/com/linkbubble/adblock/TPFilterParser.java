package com.linkbubble.adblock;

import android.content.Context;

import com.linkbubble.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by serg on 16-01-05.
 */
public class TPFilterParser {

    static {
        System.loadLibrary("LinkBubble");
    }

    private static final int BUFFER_TO_READ = 16384;    // 16Kb

    public TPFilterParser(Context context) {
        mVerNumber = getDataVerNumber(context);
        mBuffer = readTPData(context);
        if (mBuffer != null) {
            init(mBuffer);
        }
    }

    private String getDataVerNumber(Context context) {
        String url = context.getString(R.string.tracking_protection_url);
        String[] split = url.split("/");
        if (split.length > 2) {
            return split[split.length - 2];
        }

        return "";
    }

    private void removeOldVersionFiles(Context context) {
        File dataDirPath = new File(context.getApplicationInfo().dataDir);
        File[] fileList = dataDirPath.listFiles();

        for (File file : fileList) {
            if (file.getAbsoluteFile().toString().endsWith(context.getString(R.string.tracking_protection_localfilename))) {
                file.delete();
            }
        }
    }

    private byte[] readTPData(Context context) {
        File dataPath = new File(context.getApplicationInfo().dataDir,
                mVerNumber + context.getString(R.string.tracking_protection_localfilename));
        if (!dataPath.exists()) {
            removeOldVersionFiles(context);
            downloadTPData(context);
        }

        byte[] buffer = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dataPath.getAbsolutePath());
            int size = inputStream.available();
            buffer = new byte[size];
            int n = - 1;
            int bytesOffset = 0;
            byte[] tempBuffer = new byte[BUFFER_TO_READ];
            while ( (n = inputStream.read(tempBuffer)) != -1) {
                System.arraycopy(tempBuffer, 0, buffer, bytesOffset, n);
                bytesOffset += n;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    private byte[] downloadTPData(Context context) {
        byte[] buffer = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(context.getString(R.string.tracking_protection_url));
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return buffer;
            }

            File path = new File(context.getApplicationInfo().dataDir,
                    mVerNumber + context.getString(R.string.tracking_protection_localfilename));
            FileOutputStream outputStream = new FileOutputStream(path);
            inputStream = connection.getInputStream();
            buffer = new byte[BUFFER_TO_READ];
            int n = - 1;
            while ( (n = inputStream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, n);
            }
            outputStream.close();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (connection != null)
                connection.disconnect();
        }

        return buffer;
    }


    public native void init(byte[] data);
    public native boolean matchesTracker(String baseHost);
    public native String findFirstPartyHosts(String baseHost);

    private byte[] mBuffer;
    private String mVerNumber;
}
package org.walkersguide.exceptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;


public class CustomExceptionHandler implements UncaughtExceptionHandler {

    private Context mContext;
    private SettingsManager settingsManager;
    private UncaughtExceptionHandler defaultUEH;
    private String logPath;
    private String url;

    public CustomExceptionHandler(Context mContext) {
        this.mContext = mContext;
        settingsManager = ((Globals) mContext.getApplicationContext()).getSettingsManagerInstance();
        this.logPath = settingsManager.getProgramLogFolder();
        this.url = null;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        String filename = timestamp + ".log";

        // create bug report with some general information about the program and the stacktrace
        String bugReport = String.format(
                "Bug report\n\nCreated at: %s\nClient version: %s\nWebserver interface version: %d\n" +
                "Route indirection factor: %.1f\nWay classes: %s\n\nStacktrace:\n%s",
                timestamp, settingsManager.getClientVersion(),
                settingsManager.getInterfaceVersion(), settingsManager.getRouteFactor(),
                TextUtils.join(", ", settingsManager.getRoutingWayClasses()), stacktrace);

        if (logPath != null) {
            if ( checkSDCardStatus() == 2) {
                writeToFile(bugReport, filename);
            }
        }
        if (url != null) {
            sendToServer(bugReport, filename);
        }
        defaultUEH.uncaughtException(t, e);
    }

    private int checkSDCardStatus() {
        String auxSDCardStatus = Environment.getExternalStorageState();
        if (auxSDCardStatus.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText( mContext,
                    "Info: SD Card is writable.", Toast.LENGTH_LONG) .show();
            return 2;
    } else if (auxSDCardStatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Toast.makeText(
                    mContext,
                    "Warning, the SDCard it's only in read mode.\nthis does not result in malfunction"
                    + " of the read aplication", Toast.LENGTH_LONG)
                .show();
            return 1;
        } else if (auxSDCardStatus.equals(Environment.MEDIA_NOFS)) {
            Toast.makeText(
                    mContext,
                    "Error, the SDCard can be used, it has not a corret format or "
                    + "is not formated.", Toast.LENGTH_LONG)
                .show();
            return 0;
        } else if (auxSDCardStatus.equals(Environment.MEDIA_REMOVED)) {
            Toast.makeText(
                    mContext,
                    "Error, the SDCard is not found, to use the reader you need "
                    + "insert a SDCard on the device.",
                    Toast.LENGTH_LONG).show();
            return 0;
        } else if (auxSDCardStatus.equals(Environment.MEDIA_SHARED)) {
            Toast.makeText(
                    mContext,
                    "Error, the SDCard is not mounted beacuse is using "
                    + "connected by USB. Plug out and try again.",
                    Toast.LENGTH_LONG).show();
            return 0;
        } else if (auxSDCardStatus.equals(Environment.MEDIA_UNMOUNTABLE)) {
            Toast.makeText(
                    mContext,
                    "Error, the SDCard cant be mounted.\nThe may be happend when the SDCard is corrupted "
                    + "or crashed.", Toast.LENGTH_LONG).show();
            return 0;
        } else if (auxSDCardStatus.equals(Environment.MEDIA_UNMOUNTED)) {
            Toast.makeText(
                    mContext,
                    "Error, the SDCArd is on the device but is not mounted."
                    + "Mount it before use the app.",
                    Toast.LENGTH_LONG).show();
            return 0;
        }
        return -1;
    }

    private void writeToFile(String stacktrace, String filename) {
        File logFolder = new File(this.logPath);
        if (! logFolder.exists())
            logFolder.mkdirs();
        if (logFolder.exists()) {
            File logFile = new File(logFolder, filename);
            try {
            	BufferedWriter out = new BufferedWriter(new FileWriter(logFile.getAbsolutePath(), false));
                out.write(stacktrace);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("log folder does not exist");
        }
    }

    private void sendToServer(String stacktrace, String filename) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("filename", filename));
        nvps.add(new BasicNameValuePair("stacktrace", stacktrace));
        try {
            httpPost.setEntity(
                    new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            httpClient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package org.walkersguide.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONObject;
import org.walkersguide.R;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;

public class DataDownloader extends AsyncTask<String, Void, JSONObject> {

    public interface DataDownloadListener {
        public void dataDownloadedSuccessfully(JSONObject jsonObject);
        public void dataDownloadFailed(String error);
        public void dataDownloadCanceled();
    }

	private String error;
    private DataDownloadListener dataDownloadListener;
    private Context mContext;
    private HttpsURLConnection connection;
    private Handler cancelConnectionHandler;
    private CancelConnection cancelConnection;

	public DataDownloader(Context mContext) {
        this.mContext = mContext;
        this.error = "";
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    @Override protected void onPreExecute() {
        cancelConnectionHandler.postDelayed(cancelConnection, 100);
    }

	@Override protected JSONObject doInBackground(String... params) {
        if (params.length == 0)
            return null;
        try {
            // check connectivity of the android device
            ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (! networkInfo.isConnected()) {
                this.error = mContext.getResources().getString(R.string.messageNoNetworkAvailable);
                return null;
            }

            // create a HttpsURLConnection object
            URL url = new URL(params[0]);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(90000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json, application/gzip");

            // load self signed certificates of the following two domains
            if (params[0].startsWith("https://wasserbett.ath.cx")
                    || params[0].startsWith("https://walkersguide.org")) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput;
                // Create a KeyStore containing our trusted CAs
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                // load certificate of wasserbett.ath.cx
                caInput = mContext.getResources().openRawResource(R.raw.wasserbett);
                try {
                    keyStore.setCertificateEntry("ca1", cf.generateCertificate(caInput));
                } finally {
                    caInput.close();
                }
                // load certificate of walkersguide.org
                caInput = mContext.getResources().openRawResource(R.raw.walkersguide);
                try {
                    keyStore.setCertificateEntry("ca2", cf.generateCertificate(caInput));
                } finally {
                    caInput.close();
                }
                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);
                // Create an SSLContext that uses our TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                connection.setSSLSocketFactory(sslContext.getSocketFactory());
            }

            // load additional parameters via post method, if given
            if (params.length > 1) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(params[1].getBytes("UTF-8"));
                os.close();
            }

            // connect to server
            connection.connect();
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                this.error = "Bad response code from server: " + connection.getResponseCode();
                return null;
            }

            // check if the user canceled the request in the meantime
            if (isCancelled())
                return null;

            // receive web server response
            BufferedReader reader;
	        StringBuilder sb = new StringBuilder();
            InputStream in = connection.getInputStream();
            if (connection.getContentType().equals("application/gzip")) {
    	        reader = new BufferedReader(new InputStreamReader(
                            new GZIPInputStream(in), "utf-8"), 8);
            } else {
    	        reader = new BufferedReader(new InputStreamReader(in, "utf-8"), 8);
            }
            String line = null;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
            in.close();
            connection.disconnect();

            // convert to json
			return new JSONObject(sb.toString());
        } catch (Exception e) {
            error = e.getMessage();
            System.out.println("xx error = " + error);
            return null;
        }
	}

    @Override protected void onPostExecute(JSONObject jsonObject) {
        cancelConnectionHandler.removeCallbacks(cancelConnection);
        if (jsonObject != null) {
            dataDownloadListener.dataDownloadedSuccessfully(jsonObject);
        } else {
            dataDownloadListener.dataDownloadFailed(this.error);
        }
    }

    @Override protected void onCancelled(JSONObject jsonObject) {
        cancelConnectionHandler.removeCallbacks(cancelConnection);
        dataDownloadListener.dataDownloadCanceled();
    }

    public void setDataDownloadListener(DataDownloadListener dataDownloadListener) {
        this.dataDownloadListener = dataDownloadListener;
    }

    public void cancelDownloadProcess() {
        this.cancel(true);
    }

    private class CancelConnection implements Runnable {
        public void run() {
            if (isCancelled()) {
                try {
                    connection.disconnect();
                } catch (Exception e) {}
                return;
            }
            cancelConnectionHandler.postDelayed(this, 100);
        }
    }
}

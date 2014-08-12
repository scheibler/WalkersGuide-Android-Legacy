package org.walkersguide.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

public class DataDownloader extends AsyncTask<String, Void, JSONObject> {

    public interface DataDownloadListener {
        public void dataDownloadedSuccessfully(JSONObject jsonObject);
        public void dataDownloadFailed(String error);
        public void dataDownloadCanceled();
    }

	private int timeout = 90000;
	private String error;
    private DataDownloadListener dataDownloadListener;
    private Context mContext;
    private HttpClient  httpClient;
	public DataDownloader(Context mContext) {
        this.mContext = mContext;
        this.error = "";
    }

    // @Override protected void onPreExecute() {}

	@Override protected JSONObject doInBackground(String... params) {
        if (this.getNetworkStatus() == false) {
            return null;
        }
        // check given parameter list
        if (params.length != 3)
            return null;
        if ((params[1].equals("")) || (params[2].equals("")))
            return null;
        // send request
        HttpResponse httpResponse;
        if (params[0].equals("get")) {
            httpResponse = this.getMethod(params[1], params[2]);
        } else if (params[0].equals("post")) {
            httpResponse = this.postMethod(params[1], params[2]);
        } else {
            return null;
        }
		if(httpResponse == null || isCancelled()) {
            return null;
        }
		String jsonString = this.getResponseString(httpResponse);
		if (jsonString == null || isCancelled()) {
            return null;
        }
        JSONObject jsonObject = this.getJSONObject(jsonString);
        if (jsonObject == null) {
            return null;
        }
    	return jsonObject;
	}

    @Override protected void onPostExecute(JSONObject jsonObject) {
        if (jsonObject != null) {
            dataDownloadListener.dataDownloadedSuccessfully(jsonObject);
        } else {
            dataDownloadListener.dataDownloadFailed(this.error);
        }
    }

    @Override protected void onCancelled(JSONObject jsonObject) {
        dataDownloadListener.dataDownloadCanceled();
    }

	public void setTimeout(int timeout){
		this.timeout = timeout;
	}

    public void setDataDownloadListener(DataDownloadListener dataDownloadListener) {
        this.dataDownloadListener = dataDownloadListener;
    }

    public void cancelDownloadProcess() {
        this.cancel(true);
        if (httpClient != null) {
            if (httpClient.getConnectionManager() != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

    private Boolean getNetworkStatus() {
        ConnectivityManager connMgr = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            this.error = mContext.getResources().getString(R.string.messageNoNetworkAvailable);
            return false;
        }
    }

	private HttpResponse getMethod(String url, String args){
		httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter("http.socket.timeout", this.timeout);
		try {
			return httpClient.execute(
                    new HttpGet(url + "/" + args.replace(' ','+')) );
		} catch (UnsupportedEncodingException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageTransmissionError),
                    e.getMessage() );
			e.printStackTrace();
		} catch (ClientProtocolException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageTransmissionError),
                    e.getMessage() );
			e.printStackTrace();
		} catch (IOException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageTransmissionError),
                    e.getMessage() );
			e.printStackTrace();
		}
        return null;
	}	

	private HttpResponse postMethod(String url, String args){
		httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter("http.socket.timeout", this.timeout);
        HttpPost httpPost = new HttpPost(url);        
		try {
            StringEntity entity = new StringEntity( args, "UTF-8" );
            entity.setContentType("application/json;charset=UTF-8");
            entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
			httpPost.setEntity( entity );
			return httpClient.execute(httpPost);
		} catch (UnsupportedEncodingException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageTransmissionError),
                    e.getMessage() );
			e.printStackTrace();
		} catch (ClientProtocolException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageTransmissionError),
                    e.getMessage() );
			e.printStackTrace();
		} catch (IOException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageTransmissionError),
                    e.getMessage() );
			e.printStackTrace();
		}
        return null;
	}	

	private String getResponseString(HttpResponse httpResponse) {
		try {
			HttpEntity httpEntity = httpResponse.getEntity();
			// get data
			InputStream is = httpEntity.getContent();
            // check if data is zipped
            BufferedReader reader;
            GZIPInputStream gzInput = null;
            Header contentType = httpEntity.getContentType();
            if (contentType.getValue().split(";")[0].trim().equals("application/gzip")) {
                gzInput = new GZIPInputStream(is);
    	        reader = new BufferedReader(new InputStreamReader(gzInput, "utf-8"), 8);
            } else {
    	        reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
            }
            // parse input line by line
	        StringBuilder sb = new StringBuilder();
            String line = null;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
            // close streams
            if (gzInput != null) {
                gzInput.close();
            }
            is.close();
	        return sb.toString();
	    } catch (Exception e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageReadContentsError),
                    e.getMessage() );
			e.printStackTrace();
	    }
		return null;
	}

    private JSONObject getJSONObject(String jsonString) {
		try {
			return new JSONObject(jsonString);
		} catch (JSONException e) {
            this.error = String.format(
                    mContext.getResources().getString(R.string.messageJSONParseError),
                    e.getMessage() );
			e.printStackTrace();
		}
        return null;
	}

}


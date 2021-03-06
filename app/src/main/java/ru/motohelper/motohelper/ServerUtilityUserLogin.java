package ru.motohelper.motohelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by Michael on 05.11.2016.
 */

public class ServerUtilityUserLogin extends AsyncTask<Void, Void, User> {
    Context ctx;
    String login;
    String password;
    SettingsHolder settingsHolder;
    User loggedUser;
    ProgressDialog processing;
    private OnRefreshed listener;

    public ServerUtilityUserLogin(Context context, String login, String password) {
        this.ctx = context;
        this.login = login;
        this.password = password;
        settingsHolder = new SettingsHolder(ctx);
    }
    public interface OnRefreshed {
        void onRefreshCompleted() throws ExecutionException, InterruptedException;
    }

    public void setOnRefreshed(OnRefreshed listener) {
        this.listener = listener;
    }


    @Override
    protected void onPreExecute() {
        processing = new ProgressDialog(ctx);
        processing.setMessage(ctx.getString(R.string.LoggingIn));
        processing.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        processing.setIndeterminate(true);
        processing.setCancelable(false);
        processing.show();
    }

    @Override
    protected User doInBackground(Void... voids) {
        ArrayList<NameValuePair> dataToSend = new ArrayList<NameValuePair>();
        dataToSend.add(new BasicNameValuePair("email", login));
        dataToSend.add(new BasicNameValuePair("password", password));

        HttpParams params1 = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params1, 1000 * 15);
        HttpConnectionParams.setSoTimeout(params1, 1000 * 15);

        HttpClient client = new DefaultHttpClient(params1);
        HttpPost post = new HttpPost(settingsHolder.getIpAddress() + "validate_user.php");
        loggedUser = null;


        try {
            post.setEntity(new UrlEncodedFormEntity(dataToSend, HTTP.UTF_8));
            HttpResponse httpResponse = client.execute(post);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            if (result.equals("[]")) {
                loggedUser = new User("invalid", "", "", "", "");
                return loggedUser;
            }
            JSONObject jObject = new JSONObject(result);


            loggedUser = new User(jObject.getString("email"), jObject.getString("password"), jObject.getString("name"), jObject.getString("fam"), jObject.getString("phone"));

        } catch (Exception e) {
            e.printStackTrace();
            loggedUser = new User("noConnection", "", "", "", "");
            return loggedUser;
        }
        return loggedUser;
    }
    public User getUser(){
        return this.loggedUser;

    }

    @Override
    protected void onPostExecute(User result) {
        super.onPostExecute(result);
        processing.dismiss();
        this.loggedUser = result;
        if (listener != null)
            try {
                listener.onRefreshCompleted();
            } catch (Exception e) {
            }

    }
}

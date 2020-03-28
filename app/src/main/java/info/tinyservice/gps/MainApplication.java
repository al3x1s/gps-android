package info.tinyservice.gps;

/*
 * Copyright 2016 Irving Gonzalez (ialexis93@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.support.multidex.MultiDexApplication;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.tinyservice.gps.model.User;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class MainApplication  extends MultiDexApplication {
    public static final String PREFERENCE_AUTHENTICATED = "authenticated";
    public static final String PREFERENCE_URL = "url";
    public static final String PREFERENCE_EMAIL = "email";
    public static final String PREFERENCE_PASSWORD = "password";

    public static final String DEFAULT_SERVER = "http://162.248.52.101"; // local - http://10.0.2.2:8082

    public interface GetServiceCallback {
        void onServiceReady(OkHttpClient client, Retrofit retrofit, WebService service);
        boolean onFailure();
    }

    private SharedPreferences preferences;

    private OkHttpClient client;
    private WebService service;
    private Retrofit retrofit;
    private User user;

    private final List<GetServiceCallback> callbacks = new LinkedList<>();

    public void getServiceAsync(GetServiceCallback callback) {
        if (service != null) {
            callback.onServiceReady(client, retrofit, service);
        } else {
            if (callbacks.isEmpty()) {
                initService();
            }
            callbacks.add(callback);
        }
    }


    public WebService getService() { return service; }
    public User getUser() { return user; }

    public void removeService() {
        service = null;
        user = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void initService() {
        final String url = DEFAULT_SERVER;
        String email = preferences.getString(PREFERENCE_EMAIL, null);
        final String password = preferences.getString(PREFERENCE_PASSWORD, null);

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        try {
            retrofit = new Retrofit.Builder()
                    .client(client)
                    .baseUrl(url + ":3000")
                    .addConverterFactory(JacksonConverterFactory.create())
                    .build();
        } catch (IllegalArgumentException e) {
            Log.e("Error Main Application", e.getMessage());
            for (GetServiceCallback callback : callbacks) {
                callback.onFailure();
            }
            callbacks.clear();
        }

        final WebService service = retrofit.create(WebService.class);

        service.addSession(email, password).enqueue(new WebServiceCallback<User>(this) {
            @Override
            public void onSuccess(Response<User> response) {
                MainApplication.this.service = service;
                MainApplication.this.user = response.body();
                for (GetServiceCallback callback : callbacks) {
                    callback.onServiceReady(client, retrofit, service);
                }
                callbacks.clear();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                boolean handled = false;
                for (GetServiceCallback callback : callbacks) {
                    handled = callback.onFailure();
                }
                callbacks.clear();
                if (!handled) {
                    super.onFailure(call, t);
                }
            }
        });
    }
}
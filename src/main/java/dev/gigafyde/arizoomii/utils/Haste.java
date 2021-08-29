package dev.gigafyde.arizoomii.utils;

import java.io.IOException;
import java.util.Collections;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

public class Haste {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).build();

    public static String paste(String input) {
        try {
            RequestBody body = RequestBody.create(input, MediaType.parse("text/plain; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://hastebin.com/documents")
                    .post(body)
                    .build();
            Response response = httpClient.newCall(request).execute();
            @SuppressWarnings("ConstantConditions") JSONObject json = new JSONObject(response.body().string());
            return "https://hastebin.com/" + json.getString("key");
        } catch (IOException | NullPointerException | JSONException e) {
            LoggerFactory.getLogger(Haste.class).error("Failed to generate paste", e);
            return null;
        }
    }

}

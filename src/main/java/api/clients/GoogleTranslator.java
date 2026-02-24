package api.clients;

import api.language.Language;
import com.alibaba.fastjson2.*;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class GoogleTranslator implements TranslationClient {

    private static final String url = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t";

    private final OkHttpClient client = new OkHttpClient.Builder()
        .callTimeout(1, TimeUnit.SECONDS)
        .followRedirects(false)
        .build();

    public String translate(String text, Language from, Language to){
        String body = "tl=" + to.code + "&sl=" + from.code + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(body, MediaType.parse("application/x-www-form-urlencoded")))
            .build();

        try(Response response = client.newCall(request).execute()){
            if(!response.isSuccessful()){
                throw new IllegalStateException("Google translator returned HTTP " + response.code());
            }

            JSONArray root = JSON.parseArray(response.body().string());
            if(root == null || root.isEmpty() || root.getJSONArray(0) == null || root.getJSONArray(0).isEmpty()){
                throw new IllegalStateException("Google translator returned malformed response");
            }
            return root.getJSONArray(0).getString(0);
        }catch(IOException e){
            throw new RuntimeException("Google translator request failed", e);
        }
    }
}

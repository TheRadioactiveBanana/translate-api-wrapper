package api.clients;

import api.language.Language;
import com.alibaba.fastjson2.*;
import okhttp3.*;
import org.slf4j.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static api.language.Language.auto;

public class LLMTranslator implements TranslationClient {

    private static final MediaType jsonMediaType = MediaType.get("application/json; charset=utf-8");
    private static final Logger log = LoggerFactory.getLogger(LLMTranslator.class);
    public static final String chatFormat = """
            <start_of_turn>user
            You are a professional $FROM/$TO translator.
            Your goal is to accurately convey the meaning and nuances of the original text while adhering to $TO grammar, vocabulary, and cultural sensitivities.
            Produce only the $TO translation, without any additional explanations or commentary.
            Please translate the following $FROM text into $TO, while keeping informal tone:

            $TEXT
            <end_of_turn>
            <start_of_turn>model
            """;

    private final OkHttpClient client = new OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build();

    private final String url;
    private final int maxTokens;
    
    public LLMTranslator(String url, int maxTokens){
        if(url == null || url.isBlank()) throw new IllegalArgumentException("LLM translator URL is missing");

        this.url = url;
        this.maxTokens = maxTokens;
    }

    @Override
    public String backend(){
        return "llm";
    }

    @Override
    public String translate(String text, Language from, Language to){
        var requestBody = new JSONObject();

        requestBody.put("temperature", 0.6);
        requestBody.put("top_p", 0.95);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("prompt", chatFormat
            .replace("$FROM", from == auto ? "" : from.name + " (" + from + ")")
            .replace("$TO", to.name + " (" + to + ")")
            .replace("$TEXT", text.trim())
        );

        Request request = new Request.Builder()
            .url(url + "/v1/completions")
            .post(RequestBody.create(requestBody.toJSONString(), jsonMediaType))
            .build();

        try(Response response = client.newCall(request).execute()){
            String body = response.body().string();

            if(!response.isSuccessful()) {
                log.error(body);
                throw new IllegalStateException("LLMTranslator returned HTTP " + response.code());
            }

            try{
                return JSONObject.parseObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getString("text");
            }catch(Exception e){
                log.error(body);
                throw new IllegalStateException("LLMTranslator returned malformed output. " + body);
            }
        }catch(IOException e){
            throw new RuntimeException("LLM translator request failed", e);
        }
    }

}

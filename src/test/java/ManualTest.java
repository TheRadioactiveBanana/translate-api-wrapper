import okhttp3.*;

import static org.junit.jupiter.api.Assertions.*;

void main(){
    var client = new OkHttpClient();

    var body = RequestBody.create("Hello! How are you?", MediaType.parse("text/plain"));

    var request = new Request.Builder()
        .url("http://localhost:8080/api/translate")
        .post(body)
        .header("token", "example-token")
        .header("from", "en")
        .header("to", "fr")
        .build();

    try(var response = client.newCall(request).execute()){
        System.out.println(response.body().string());
    }catch(Exception e){
        fail("Failed to translate.");
    }
}
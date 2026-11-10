package pl.nks;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void send(String message) {
        String url = Config.INSTANCE.webhook;
        if (url == null || url.isBlank()) return;
        try {
            JsonObject o = new JsonObject();
            o.addProperty("content", message);
            o.addProperty("username", "NKS Sell");
            HttpRequest req = HttpRequest.newBuilder(URI.create(url.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(o.toString()))
                    .build();
            CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

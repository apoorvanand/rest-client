import okhttp3.*;
import okhttp3.Request.Builder;
import okhttp3.RequestBody.Builder as RequestBodyBuilder;
import okio.BufferedSink;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EfficientRestClient {

    private OkHttpClient client;
    private MetricRegistry metricRegistry;
    private Timer requestTimer;

    public EfficientRestClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        metricRegistry = new MetricRegistry();
        requestTimer = metricRegistry.timer("request-timer");
    }

    public String sendGetRequest(String url) throws IOException {
        Request request = new Builder()
                .url(url)
                .get()
                .build();

        try (Response response = executeRequest(request)) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new IOException("Request failed with status code: " + response.code());
            }
        }
    }

    public void sendPostRequest(String url, String payload) throws IOException {
        RequestBody requestBody = new RequestBodyBuilder()
                .create(MediaType.get("application/json; charset=utf-8"), payload)
                .build();

        Request request = new Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed with status code: " + response.code());
            }
        }
    }

    private Response executeRequest(Request request) throws IOException {
        try (Timer.Context context = requestTimer.time()) {
            return client.newCall(request).execute();
        }
    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public static void main(String[] args) {
        EfficientRestClient client = new EfficientRestClient();

        try {
            String response = client.sendGetRequest("https://api.example.com/data");
            System.out.println("GET Response: " + response);

            client.sendPostRequest("https://api.example.com/submit", "{\"data\": \"sample\"}");
            System.out.println("POST Request sent.");

            client.getMetricRegistry().getMetrics().forEach((name, metric) -> {
                System.out.println(name + ": " + metric);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

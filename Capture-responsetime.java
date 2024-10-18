import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RedisCacheManager {

    private final RedisAsyncCommands<String, String> asyncCommands;

    public RedisCacheManager(String redisUri) {
        RedisClient client = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> connection = client.connect();
        this.asyncCommands = connection.async();
    }

    public CompletableFuture<String> getAsync(String key) {
        long startTime = System.nanoTime();
        return asyncCommands.get(key)
                .thenApply(value -> {
                    long endTime = System.nanoTime();
                    long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                    System.out.printf("Cache GET for key %s took %d ms%n", key, duration);
                    return value;
                });
    }

    public CompletableFuture<String> setAsync(String key, String value, long expirationSeconds) {
        long startTime = System.nanoTime();
        return asyncCommands.setex(key, expirationSeconds, value)
                .thenApply(result -> {
                    long endTime = System.nanoTime();
                    long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                    System.out.printf("Cache SET for key %s took %d ms%n", key, duration);
                    return result;
                });
    }

    public CompletableFuture<String> getWithCachingAsync(String key, long expirationSeconds, CacheLoader loader) {
        return getAsync(key)
                .thenCompose(cachedValue -> {
                    if (cachedValue != null) {
                        return CompletableFuture.completedFuture(cachedValue);
                    } else {
                        return loader.load()
                                .thenCompose(loadedValue -> 
                                    setAsync(key, loadedValue, expirationSeconds)
                                        .thenApply(__ -> loadedValue)
                                );
                    }
                });
    }

    @FunctionalInterface
    public interface CacheLoader {
        CompletableFuture<String> load();
    }

    // Example usage
    public static void main(String[] args) {
        RedisCacheManager cacheManager = new RedisCacheManager("redis://localhost:6379");

        cacheManager.getWithCachingAsync("example_key", 60,
                () -> CompletableFuture.supplyAsync(() -> {
                    // Simulate expensive operation
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return "example_value";
                }))
                .thenAccept(value -> System.out.println("Retrieved value: " + value))
                .join();
    }
}

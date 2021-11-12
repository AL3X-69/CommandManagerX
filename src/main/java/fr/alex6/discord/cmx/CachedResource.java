package fr.alex6.discord.cmx;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class CachedResource<T> {
    private final T resource;
    private final long timestamp;

    protected CachedResource(T resource, long timestamp) {
        this.resource = resource;
        this.timestamp = timestamp;
    }

    public T getCachedResource() {
        return resource;
    }

    public long getCacheTimestamp() {
        return timestamp;
    }

    public LocalDateTime getCacheDateTime() {
        return LocalDateTime.ofEpochSecond(getCacheTimestamp(), 0, ZoneOffset.UTC);
    }

}

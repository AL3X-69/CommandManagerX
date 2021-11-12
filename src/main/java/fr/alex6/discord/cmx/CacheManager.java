package fr.alex6.discord.cmx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class CacheManager {
    private final File cacheFolder;
    private final ObjectMapper objectMapper;

    protected CacheManager(ObjectMapper objectMapper) {
        this.cacheFolder = new File("cache");
        if (!cacheFolder.exists()) //noinspection ResultOfMethodCallIgnored
            cacheFolder.mkdirs();
        this.objectMapper = objectMapper;
    }

    public CacheManager(ObjectMapper objectMapper, String folderPath) {
        this.cacheFolder = new File(folderPath);
        if (!cacheFolder.exists()) //noinspection ResultOfMethodCallIgnored
            cacheFolder.mkdirs();
        this.objectMapper = objectMapper;
    }

    public void cacheResource(String name, Object resource) throws IOException {
        if (objectMapper.canSerialize(resource.getClass())) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("time", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
            objectNode.set("resource", objectMapper.valueToTree(resource));
            File cacheFile = new File(cacheFolder.getAbsolutePath()+"/"+name+".json");
            if (!cacheFile.exists()) //noinspection ResultOfMethodCallIgnored
                cacheFile.createNewFile();
            objectMapper.writeValue(cacheFile, objectNode);
        } else {
            throw new IOException("Object is not serializable");
        }
    }

    public <T> CachedResource<T> getCachedResource(String name, Class<T> cachedClass) throws IOException {
        File cacheFile = new File(cacheFolder.getAbsolutePath()+"/"+name+".json");
        if (cacheFile.exists()) {
            JsonNode jsonNode = objectMapper.readTree(cacheFile);
            return new CachedResource<>(objectMapper.treeToValue(jsonNode.get("resource"), cachedClass), jsonNode.get("time").asLong());
        } else {
            return null;
        }
    }
}

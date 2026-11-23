package com.nvhuong.devworkbench.plugin.minio;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tools of the MinIO plugin: browse buckets/objects and read small text objects. Servers are addressed by the
 * profiles saved in the MinIO tool, so access/secret keys never pass through the AI client. A presigned link grants
 * access to anyone holding it, so creating one counts as a write tool.
 */
final class MinioMcpTools {
    static final String PROFILE_FILE = "connection-profiles.txt";
    private static final int DEFAULT_OBJECT_LIMIT = 200;
    private static final int MAX_OBJECT_LIMIT = 1000;
    private static final int DEFAULT_READ_BYTES = 64 * 1024;
    private static final int MAX_READ_BYTES = 1024 * 1024;

    record Profile(String name, String endpoint, String accessKey, String secretKey) { }

    private MinioMcpTools() {
    }

    static void register(PluginContext context) {
        for (McpToolContribution tool : tools(context.storage())) {
            context.registerMcpTool(tool);
        }
    }

    static List<McpToolContribution> tools(PluginStorage storage) {
        return List.of(
                new McpToolContribution("minio_list_profiles", "List MinIO connection profiles",
                        "Lists the MinIO/S3 profiles saved in the DevWorkbench MinIO tool. Keys are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listProfiles(storage)),
                new McpToolContribution("minio_list_buckets", "List MinIO buckets",
                        "Lists the buckets visible to a saved profile.",
                        schema(Map.of("profile", string("MinIO profile name (see minio_list_profiles).")), List.of("profile")),
                        true, arguments -> listBuckets(storage, arguments)),
                new McpToolContribution("minio_list_objects", "List MinIO objects",
                        "Lists objects of a bucket (recursive), optionally under a prefix, up to limit objects.",
                        schema(Map.of(
                                "profile", string("MinIO profile name."),
                                "bucket", string("Bucket name."),
                                "prefix", Map.of("type", "string", "description", "Optional object name prefix, e.g. reports/2026/."),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_OBJECT_LIMIT,
                                        "description", "Maximum objects to return (default 200).")),
                                List.of("profile", "bucket")),
                        true, arguments -> listObjects(storage, arguments)),
                new McpToolContribution("minio_read_object", "Read a MinIO object",
                        "Returns the content of an object as text when it is UTF-8 (first maxBytes bytes), otherwise its metadata only.",
                        schema(Map.of(
                                "profile", string("MinIO profile name."),
                                "bucket", string("Bucket name."),
                                "object", string("Object name (full path)."),
                                "maxBytes", Map.of("type", "integer", "minimum", 1, "maximum", MAX_READ_BYTES,
                                        "description", "Maximum bytes to read (default 65536).")),
                                List.of("profile", "bucket", "object")),
                        true, arguments -> readObject(storage, arguments)),
                new McpToolContribution("minio_presigned_url", "Create a MinIO download link",
                        "Creates a presigned GET URL for an object, valid for expirySeconds (default 3600, max 7 days). Anyone with the link can download the object, so this needs --allow-writes.",
                        schema(Map.of(
                                "profile", string("MinIO profile name."),
                                "bucket", string("Bucket name."),
                                "object", string("Object name."),
                                "expirySeconds", Map.of("type", "integer", "minimum", 60, "maximum", 604800)),
                                List.of("profile", "bucket", "object")),
                        false, arguments -> presignedUrl(storage, arguments)));
    }

    static Map<String, Object> listProfiles(PluginStorage storage) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Profile profile : readProfiles(storage)) {
            items.add(Map.of("name", profile.name(), "endpoint", profile.endpoint()));
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> listBuckets(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        List<Map<String, Object>> buckets = new ArrayList<>();
        for (Bucket bucket : client(profile).listBuckets()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", bucket.name());
            if (bucket.creationDate() != null) item.put("created", bucket.creationDate().toString());
            buckets.add(item);
        }
        return Map.of("profile", profile.name(), "buckets", buckets, "count", buckets.size());
    }

    static Map<String, Object> listObjects(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        String bucket = required(arguments, "bucket");
        String prefix = optional(arguments, "prefix");
        int limit = Math.max(1, Math.min(MAX_OBJECT_LIMIT, integer(arguments, "limit", DEFAULT_OBJECT_LIMIT)));
        ListObjectsArgs.Builder builder = ListObjectsArgs.builder().bucket(bucket).recursive(true);
        if (prefix != null) builder.prefix(prefix);
        List<Map<String, Object>> objects = new ArrayList<>();
        boolean truncated = false;
        for (Result<Item> result : client(profile).listObjects(builder.build())) {
            if (objects.size() >= limit) {
                truncated = true;
                break;
            }
            Item item = result.get();
            Map<String, Object> object = new LinkedHashMap<>();
            object.put("name", item.objectName());
            object.put("size", item.size());
            if (!item.isDir() && item.lastModified() != null) object.put("modified", item.lastModified().toString());
            objects.add(object);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", profile.name());
        response.put("bucket", bucket);
        response.put("objects", objects);
        response.put("count", objects.size());
        response.put("truncated", truncated);
        return response;
    }

    static Map<String, Object> readObject(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        String bucket = required(arguments, "bucket");
        String object = required(arguments, "object");
        int maxBytes = Math.max(1, Math.min(MAX_READ_BYTES, integer(arguments, "maxBytes", DEFAULT_READ_BYTES)));
        MinioClient client = client(profile);
        StatObjectResponse stat = client.statObject(StatObjectArgs.builder().bucket(bucket).object(object).build());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bucket", bucket);
        response.put("object", object);
        response.put("size", stat.size());
        response.put("contentType", stat.contentType());
        byte[] bytes = new byte[0];
        if (stat.size() > 0) {
            try (InputStream input = client.getObject(GetObjectArgs.builder().bucket(bucket).object(object)
                    .offset(0L).length(Math.min((long) maxBytes, stat.size())).build())) {
                bytes = input.readNBytes(maxBytes);
            }
        }
        String text = utf8(bytes);
        if (text != null) {
            response.put("text", text);
        } else {
            response.put("binary", true);
            response.put("base64Preview", Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(bytes, Math.min(bytes.length, 256))));
        }
        response.put("truncated", stat.size() > bytes.length);
        return response;
    }

    static Map<String, Object> presignedUrl(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        int expiry = Math.max(60, Math.min(604800, integer(arguments, "expirySeconds", 3600)));
        String url = client(profile).getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                .bucket(required(arguments, "bucket")).object(required(arguments, "object")).expiry(expiry).build());
        return Map.of("url", url, "expirySeconds", expiry);
    }

    /** The bytes as text if they are valid UTF-8 (a multi-byte character cut at the end is tolerated). */
    static String utf8(byte[] bytes) {
        for (int cut = 0; cut <= 3 && cut <= bytes.length; cut++) {
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes, 0, bytes.length - cut)).toString();
            } catch (CharacterCodingException ignored) {
                // try again without a possibly truncated trailing character
            }
        }
        return null;
    }

    private static MinioClient client(Profile profile) {
        return MinioClient.builder().endpoint(profile.endpoint()).credentials(profile.accessKey(), profile.secretKey()).build();
    }

    private static Profile profile(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        String name = required(arguments, "profile");
        return readProfiles(storage).stream()
                .filter(profile -> profile.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MinIO profile: " + name + ". Use minio_list_profiles."));
    }

    static List<Profile> readProfiles(PluginStorage storage) throws Exception {
        List<Profile> all = new ArrayList<>();
        for (String line : storage.readString(PROFILE_FILE).orElse("").lines().toList()) {
            String[] fields = line.split("\t", -1);
            if (fields.length == 4) {
                all.add(new Profile(decode(fields[0]), decode(fields[1]), decode(fields[2]), decode(fields[3])));
            }
        }
        return all;
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description, "minLength", 1);
    }

    private static String required(Map<String, Object> arguments, String name) {
        String value = optional(arguments, name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String optional(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private static int integer(Map<String, Object> arguments, String name, int defaultValue) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return defaultValue;
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
    }
}

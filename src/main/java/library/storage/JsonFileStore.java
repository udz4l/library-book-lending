package library.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonFileStore {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public JsonFileStore() {
        this(Path.of("data", "library.json"));
    }

    public JsonFileStore(Path file) {
        this.file = file;
    }

    public synchronized void save(String collection, String id, Object value) {
        try {
            ObjectNode root = readRoot();
            ObjectNode records = getCollection(root, collection);
            records.set(id, mapper.valueToTree(value));
            writeRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save library data.", e);
        }
    }

    public synchronized JsonNode find(String collection, String id) {
        ObjectNode root = readRoot();
        JsonNode records = root.get(collection);

        if (records == null) {
            return null;
        }

        return records.get(id);
    }

    public synchronized List<JsonNode> findAll(String collection) {
        ObjectNode root = readRoot();
        JsonNode records = root.get(collection);
        List<JsonNode> result = new ArrayList<>();

        if (records == null || !records.isObject()) {
            return result;
        }

        Iterator<JsonNode> values = records.elements();
        while (values.hasNext()) {
            result.add(values.next());
        }

        return result;
    }

    public synchronized void delete(String collection, String id) {
        try {
            ObjectNode root = readRoot();
            ObjectNode records = getCollection(root, collection);
            records.remove(id);
            writeRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not update library data.", e);
        }
    }

    private ObjectNode readRoot() {
        try {
            if (Files.notExists(file)) {
                return mapper.createObjectNode();
            }

            JsonNode root = mapper.readTree(file.toFile());
            return root != null && root.isObject()
                    ? (ObjectNode) root
                    : mapper.createObjectNode();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read library data.", e);
        }
    }

    private ObjectNode getCollection(ObjectNode root, String collection) {
        JsonNode existing = root.get(collection);

        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }

        ObjectNode records = mapper.createObjectNode();
        root.set(collection, records);
        return records;
    }

    private void writeRoot(ObjectNode root) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
    }
}
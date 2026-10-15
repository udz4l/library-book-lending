package library.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.storage.JsonFileStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkflowRepository {
    private final JsonFileStore store;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public WorkflowRepository(JsonFileStore store) {
        this.store = store;
    }

    public void save(LendingWorkflow workflow) {
        store.save("workflows", workflow.getId(), workflow);
    }

    public Optional<LendingWorkflow> findById(String id) {
        JsonNode node = store.find("workflows", id);
        return node == null ? Optional.empty() : Optional.of(read(node));
    }

    public List<LendingWorkflow> findAll() {
        List<LendingWorkflow> result = new ArrayList<>();
        for (JsonNode node : store.findAll("workflows")) {
            result.add(read(node));
        }
        return result;
    }

    private LendingWorkflow read(JsonNode node) {
        try {
            return mapper.treeToValue(node, LendingWorkflow.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read a workflow.", e);
        }
    }
}
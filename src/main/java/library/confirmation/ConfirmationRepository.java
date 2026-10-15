package library.confirmation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.storage.JsonFileStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfirmationRepository {
    private final JsonFileStore store;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public ConfirmationRepository(JsonFileStore store) {
        this.store = store;
    }

    public void save(LoanConfirmation confirmation) {
        store.save("confirmations", confirmation.getId(), confirmation);
    }

    public Optional<LoanConfirmation> findById(String id) {
        JsonNode node = store.find("confirmations", id);
        return node == null ? Optional.empty() : Optional.of(read(node));
    }

    public List<LoanConfirmation> findAll() {
        List<LoanConfirmation> result = new ArrayList<>();
        for (JsonNode node : store.findAll("confirmations")) {
            result.add(read(node));
        }
        return result;
    }

    private LoanConfirmation read(JsonNode node) {
        try {
            return mapper.treeToValue(node, LoanConfirmation.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read a confirmation.", e);
        }
    }
}
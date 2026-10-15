package library.lending;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.storage.JsonFileStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanRepository {
    private final JsonFileStore store;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public LoanRepository(JsonFileStore store) {
        this.store = store;
    }

    public void save(BookLoan loan) {
        store.save("loans", loan.getId(), loan);
    }

    public Optional<BookLoan> findById(String id) {
        JsonNode node = store.find("loans", id);
        return node == null ? Optional.empty() : Optional.of(read(node));
    }

    public List<BookLoan> findAll() {
        List<BookLoan> result = new ArrayList<>();
        for (JsonNode node : store.findAll("loans")) {
            result.add(read(node));
        }
        return result;
    }

    private BookLoan read(JsonNode node) {
        try {
            return mapper.treeToValue(node, BookLoan.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read a loan.", e);
        }
    }
}
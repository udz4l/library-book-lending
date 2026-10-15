package library.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.storage.JsonFileStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final JsonFileStore store;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public ReservationRepository(JsonFileStore store) {
        this.store = store;
    }

    public void save(BookReservation reservation) {
        store.save("reservations", reservation.getId(), reservation);
    }

    public Optional<BookReservation> findById(String id) {
        JsonNode node = store.find("reservations", id);
        return node == null ? Optional.empty() : Optional.of(read(node));
    }

    public List<BookReservation> findAll() {
        List<BookReservation> result = new ArrayList<>();
        for (JsonNode node : store.findAll("reservations")) {
            result.add(read(node));
        }
        return result;
    }

    private BookReservation read(JsonNode node) {
        try {
            return mapper.treeToValue(node, BookReservation.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read a reservation.", e);
        }
    }
}
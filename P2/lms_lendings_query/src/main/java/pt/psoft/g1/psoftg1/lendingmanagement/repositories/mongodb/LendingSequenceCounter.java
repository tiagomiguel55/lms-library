package pt.psoft.g1.psoftg1.lendingmanagement.repositories.mongodb;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document to store and atomically increment lending sequence counters.
 * Each year gets its own counter document.
 */
@Document(collection = "lending_sequence")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LendingSequenceCounter {
    @Id
    private String id;  // e.g., "lending_seq_2025"
    private int seq;    // current sequence number
}

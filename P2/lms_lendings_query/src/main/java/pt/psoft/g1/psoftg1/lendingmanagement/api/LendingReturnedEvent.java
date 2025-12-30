package pt.psoft.g1.psoftg1.lendingmanagement.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LendingReturnedEvent {
    private String lendingId;
    private String bookId;
    private String readerId;
    private String comment;
    private Integer grade;

    public LendingReturnedEvent(String lendingId, String bookId, String readerId, String comment, Integer grade) {
        this.lendingId = lendingId;
        this.bookId = bookId;
        this.readerId = readerId;
        this.comment = comment;
        this.grade = grade;
    }
}

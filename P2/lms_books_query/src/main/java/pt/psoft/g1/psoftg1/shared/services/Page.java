package pt.psoft.g1.psoftg1.shared.services;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Based on <a href=
 * "https://github.com/Yoh0xFF/java-spring-security-example">https://github.com/Yoh0xFF/java-spring-security-example</a>
 *
 */
@Data
public class Page {
    @Min(value = 1, message = "Paging must start with page 1")
    private int number;

    @Min(value = 1, message = "You can request minimum 1 records")
    @Max(value = 100, message = "You can request maximum 100 records")
    private int limit;

    public Page() {
        this(1, 10);
    }

    public Page(int number, int limit) {
        this.number = number;
        this.limit = limit;
    }

    public int getNumber() {
        return number;
    }

    public int getLimit() {
        return limit;
    }
}

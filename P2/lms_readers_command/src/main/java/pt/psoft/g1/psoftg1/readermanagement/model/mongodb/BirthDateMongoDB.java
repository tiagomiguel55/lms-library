package pt.psoft.g1.psoftg1.readermanagement.model.mongodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;

@NoArgsConstructor
@PropertySource({"classpath:config/library.properties"})
@Document(collection = "birth_date")
public class BirthDateMongoDB {

    @Field("birth_date")
    @Getter
    LocalDate birthDate;

    @Field("date_format_regex_pattern")
    private String dateFormatRegexPattern = "\\d{4}-\\d{2}-\\d{2}";

    @Value("${minimumReaderAge}")
    @Field("minimum_age")
    private int minimumAge;

    public BirthDateMongoDB(int year, int month, int day) {
        setBirthDate(year, month, day);
    }

    public BirthDateMongoDB(String birthDate) {
        if(!birthDate.matches(dateFormatRegexPattern)) {
            throw new IllegalArgumentException("Provided birth date is not in a valid format. Use yyyy-MM-dd");
        }

        String[] dateParts = birthDate.split("-");

        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int day = Integer.parseInt(dateParts[2]);

        setBirthDate(year, month, day);
    }

    private void setBirthDate(int year, int month, int day) {
        LocalDate minimumAgeDate = LocalDate.now().minusYears(minimumAge);
        LocalDate userDate = LocalDate.of(year, month, day);
        if(userDate.isAfter(minimumAgeDate)) {
            throw new AccessDeniedException("User must be, at least, " + minimumAge + "years old");
        }

        this.birthDate = userDate;
    }

    public String toString() {
        return String.format("%d-%d-%d", this.birthDate.getYear(), this.birthDate.getMonthValue(), this.birthDate.getDayOfMonth());
    }
}

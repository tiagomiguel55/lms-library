package pt.psoft.g1.psoftg1.readermanagement.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "phone_number")
public class PhoneNumberMongoDB {

    @Id
    private String id;

    @Field("phone_number")
    String phoneNumber;

    public PhoneNumberMongoDB(String phoneNumber) {
        setPhoneNumber(phoneNumber);
    }

    protected PhoneNumberMongoDB() {}

    private void setPhoneNumber(String number) {
        if(!(number.startsWith("9") || number.startsWith("2")) || number.length() != 9) {
            throw new IllegalArgumentException("Phone number is not valid: " + number);
        }

        this.phoneNumber = number;
    }

    public String toString() {
        return this.phoneNumber;
    }
}

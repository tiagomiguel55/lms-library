package pt.psoft.g1.psoftg1.readermanagement.model.relational;

import jakarta.persistence.Embeddable;

@Embeddable
public class PhoneNumberEntity {
    String phoneNumber;

    public PhoneNumberEntity(String phoneNumber) {
        setPhoneNumber(phoneNumber);
    }

    protected PhoneNumberEntity() {}

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

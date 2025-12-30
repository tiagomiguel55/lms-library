package pt.psoft.g1.psoftg1.readermanagement.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "email_addresses")
public class EmailAddressMongoDB {

    @Id
    private String id;

    @Field("email")
    String emailaddress;

    public EmailAddressMongoDB(String emailaddress) {
        this.emailaddress = emailaddress;
    }

    protected EmailAddressMongoDB() {}


}

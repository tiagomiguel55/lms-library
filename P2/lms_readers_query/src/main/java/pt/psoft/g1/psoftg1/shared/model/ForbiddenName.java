package pt.psoft.g1.psoftg1.shared.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
public class ForbiddenName{


    private Long pk;

    @Getter
    @Setter
    private String forbiddenName;

    public ForbiddenName(String name) {
        this.forbiddenName = name;
    }
}

@Data
@Schema(description = "A Genre for AMQP communication")
@NoArgsConstructor
public class GenreViewAMQP {
    @NotNull
    private String genre;

    @NotNull
    private Long version;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();

    public GenreViewAMQP(String genre) {
        this.genre = genre;
    }
}


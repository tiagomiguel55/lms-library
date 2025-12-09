public class AuthorViewAMQP {
    @NotNull
    private Long authorNumber;

    @NotNull
    private String name;

    private String bio;

    private String photoURI;

    @NotNull
    private Long version;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();

    public AuthorViewAMQP(Long authorNumber, String name, String bio, String photoURI) {
        this.authorNumber = authorNumber;
        this.name = name;
        this.bio = bio;
        this.photoURI = photoURI;
    }
}


package pt.psoft.g1.psoftg1.authormanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthorService authorService;

    @Mock
    private AuthorViewMapper authorViewMapper;

    @Mock
    private ConcurrencyService concurrencyService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AuthorController authorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authorController).build();
    }

    @Test
    void patchWithoutIfMatchHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/authors/{authorNumber}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAuthorPhotoReturnsImageBytes() throws Exception {
        Author author = new Author(1L, "Jane Doe", "Bio", "photo.png");
        when(authorService.findByAuthorNumber(anyLong())).thenReturn(Optional.of(author));
        byte[] payload = new byte[] {1, 2, 3};
        when(fileStorageService.getFile("photo.png")).thenReturn(payload);
        when(fileStorageService.getExtension("photo.png")).thenReturn(Optional.of("png"));

        mockMvc.perform(get("/api/authors/{authorNumber}/photo", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(payload));
    }
}

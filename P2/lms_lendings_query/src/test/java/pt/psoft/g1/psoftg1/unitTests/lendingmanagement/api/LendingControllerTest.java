package pt.psoft.g1.psoftg1.unitTests.lendingmanagement.api;

import com.google.rpc.context.AttributeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingController;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingView;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SearchLendingQuery;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SetLendingReturnedRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LendingController.class)
@ContextConfiguration(classes = {LendingController.class, LendingViewMapper.class})
class LendingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReaderService readerService;

    @MockBean
    private LendingService lendingService;

    @MockBean
    private UserService userService;

    @MockBean
    private ConcurrencyService concurrencyService;

    @MockBean
    private LendingViewMapper lendingViewMapper;

    private Lending lending;
    private LendingView lendingView;

    @BeforeEach
    void setUp() {
        lending = mock(Lending.class);
        lendingView = mock(LendingView.class);
        when(lending.getLendingNumber()).thenReturn("2024/001");
        when(lending.getVersion()).thenReturn(1L);

        when(lendingView.getLendingNumber()).thenReturn("2024/001");

    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetOverdueLendings_Success() throws Exception {



        // Mockando o serviço
        when(lendingService.getOverdue(any(Page.class))).thenReturn(List.of(lending));
        when(lendingViewMapper.toLendingView(anyList())).thenReturn(List.of(lendingView));

        // Execução do teste
        mockMvc.perform(get("/api/lendings/overdue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\": 0, \"size\": 10}")) // Modifique isso de acordo com o seu modelo de `Page`
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty());

        verify(lendingService, times(1)).getOverdue(any(Page.class));
        verify(lendingViewMapper, times(1)).toLendingView(anyList());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetOverdueLendings_NoLendingsFound() throws Exception {
        // Mockando o comportamento do serviço para retornar uma lista vazia
        when(lendingService.getOverdue(any(Page.class))).thenReturn(Collections.emptyList());

        // Execução do teste
        mockMvc.perform(get("/api/lendings/overdue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\": 0, \"size\": 10}"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    // Extraia a mensagem de erro da resposta
                    String errorMessage = result.getResponse().getErrorMessage();
                    // Verifique se a mensagem de erro contém a string esperada
                    assertTrue(errorMessage.contains("No lendinds to show."));
                }); // O corpo da resposta está vazio


        verify(lendingService, times(1)).getOverdue(any(Page.class));
    }



    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testFindLendingByNumber() throws Exception {

        Librarian librarian = mock(Librarian.class);




        when(lendingService.findByLendingNumber("2024/1")).thenReturn(Optional.of(lending));
        when(lendingViewMapper.toLendingView(any(Lending.class))).thenReturn(lendingView);
        when(userService.getAuthenticatedUser(any())).thenReturn(librarian);
        when(librarian.getAuthorities()).thenReturn(Set.of(new Role("LIBRARIAN")));



        mockMvc.perform(get("/api/lendings/2024/1")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json"));

        verify(lendingService, times(1)).findByLendingNumber("2024/1");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testFindLendingByNumberNotFound() throws Exception {
        when(lendingService.findByLendingNumber("2024/002")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/lendings/2024/002")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }



}
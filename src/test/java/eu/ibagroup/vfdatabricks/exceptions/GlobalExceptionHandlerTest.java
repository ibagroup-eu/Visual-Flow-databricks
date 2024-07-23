package eu.ibagroup.vfdatabricks.exceptions;

import eu.ibagroup.vfdatabricks.controllers.JobController;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    @Mock
    private JobController jobController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(jobController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testHandleBadRequestException() throws Exception {
        BadRequestException exception = new BadRequestException(MESSAGE);
        when(jobController.getAll("123")).thenThrow(exception);

        mockMvc.perform(get("/api/project/123/job")).andExpect(status().isBadRequest());
    }

    @Test
    void testHandleConstraintViolationException() throws Exception {
        ConstraintViolationException exception = new ConstraintViolationException(MESSAGE, null);
        when(jobController.getAll("123")).thenThrow(exception);

        mockMvc.perform(get("/api/project/123/job")).andExpect(status().isBadRequest());
    }
}

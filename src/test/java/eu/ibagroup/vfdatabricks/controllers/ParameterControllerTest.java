package eu.ibagroup.vfdatabricks.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterOverviewDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterValue;
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.ParameterService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParameterControllerTest {
    @Mock
    private ParameterService parameterService;
    @Mock
    private AuthenticationService authenticationService;

    private ParameterController parameterController;

    @BeforeEach
    void setUp() {
        parameterController = new ParameterController(parameterService, authenticationService);
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationService.getUserInfo()).thenReturn(expected);
    }

    @Test
    void testCreate() throws JsonProcessingException {
        ParameterDto parameterDto = ParameterDto
                .builder()
                .key("key")
                .secret(false)
                .value(ParameterValue.builder().build())
                .build();
        ResponseEntity<String> response = parameterController.create("projectId", "key", parameterDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Status must be OK");
        assertEquals("key", response.getBody(), "Body must be equals to key");

        verify(parameterService).create(anyString(), anyString(), any());
    }

    @Test
    void testUpdate() throws JsonProcessingException {
        ParameterDto parameterDto = ParameterDto
                .builder()
                .key("key")
                .secret(false)
                .value(ParameterValue.builder().build())
                .build();
        ResponseEntity<Void> response = parameterController.update("projectId", "key", parameterDto);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status must be OK");

        verify(parameterService).update(anyString(), anyString(), any());
    }

    @Test
    void testGet() throws IOException {
        ParameterDto parameterDto = ParameterDto
                .builder()
                .key("key")
                .secret(false)
                .value(ParameterValue.builder().build())
                .build();

        when(parameterService.get("project1", "key")).thenReturn(parameterDto);

        ParameterDto response = parameterController.get("project1", "key");

        assertEquals(parameterDto, response, "Response must be equal to dto");

        verify(parameterService).get(anyString(), anyString());
    }

    @Test
    void testGetAll() {
        when(parameterService.getAll("project1")).thenReturn(ParameterOverviewDto
                .builder()
                .params(List.of(ParameterDto.builder()
                            .key("key")
                            .secret(false)
                            .value(ParameterValue.builder().build())
                            .build(),
                        ParameterDto.builder()
                            .key("key1")
                            .secret(false)
                            .value(ParameterValue.builder().build())
                            .build()))
                .editable(true)
                .build());

        ParameterOverviewDto response = parameterController.getAll("project1");
        assertEquals(2, response.getParams().size(), "Parameters size must be 2");
        assertTrue(response.isEditable(), "Must be true");

        verify(parameterService).getAll(anyString());
    }

    @Test
    void testDelete() {
        doNothing().when(parameterService).delete("project1", "key");

        ResponseEntity<Void> response = parameterController.delete("project1", "key");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Status must be 204");

        verify(parameterService).delete(anyString(), anyString());
    }
}

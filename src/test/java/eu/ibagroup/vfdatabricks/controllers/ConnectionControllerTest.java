package eu.ibagroup.vfdatabricks.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionDto;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionOverviewDto;
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.ConnectionService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionControllerTest {

    @Mock
    private ConnectionService connectionService;
    @Mock
    private AuthenticationService authenticationServiceMock;
    private ConnectionController controller;

    @BeforeEach
    void setUp() {
        controller = new ConnectionController(connectionService, authenticationServiceMock);
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationServiceMock.getUserInfo()).thenReturn(expected);
    }

    @Test
    void testGetAll() throws InterruptedException {
        when(connectionService.getAll("projectId")).thenReturn(ConnectionOverviewDto
                .builder()
                .connections(List.of(ConnectionDto
                                .builder()
                                .key("key1")
                                .value(Map.of("key", "value"))
                                .build(),
                        ConnectionDto
                                .builder()
                                .key("key2")
                                .value(Map.of("key", "value"))
                                .build()))
                .editable(true)
                .build());

        ConnectionOverviewDto response = controller.getAll("projectId");
        assertEquals(2, response.getConnections().size(), "Connections size must be 2");
        assertTrue(response.isEditable(), "Must be true");

        verify(connectionService).getAll(anyString());
    }

    @Test
    void testCreate() throws JsonProcessingException {
        ConnectionDto connectionDto = ConnectionDto.builder().key("key").value(Map.of("key", "value")).build();
        when(connectionService.create("projectId", connectionDto)).thenReturn("connectionId");
        ResponseEntity<String> response = controller.create("projectId", connectionDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Status must be OK");
        assertEquals("connectionId", response.getBody(), "Body must be equals to connectionId");

        verify(connectionService).create(anyString(), any());
    }

    @Test
    void testUpdate() throws JsonProcessingException {
        ConnectionDto connectionDto = ConnectionDto.builder().key("key").value(Map.of("key", "value")).build();

        doNothing().when(connectionService).update("projectId", connectionDto);

        controller.update("projectId", "connectionId", connectionDto);

        verify(connectionService).update(anyString(), any());
    }

    @Test
    void testDelete() {
        doNothing().when(connectionService).delete("projectId", "connectionId");

        ResponseEntity<Void> response = controller.delete("projectId", "connectionId");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Status must be 204");

        verify(connectionService).delete(anyString(), anyString());
    }
}

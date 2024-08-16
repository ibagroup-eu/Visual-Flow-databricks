package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.DatabricksAPIService;
import eu.ibagroup.vfdatabricks.services.UtilsService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UtilControllerTest {

    @Mock
    private DatabricksAPIService databricksAPIService;
    @Mock
    private AuthenticationService authenticationServiceMock;
    private UtilController utilController;

    @BeforeEach
    void setUp() {
        utilController = new UtilController(databricksAPIService, authenticationServiceMock);
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationServiceMock.getUserInfo()).thenReturn(expected);
    }

    @Test
    void testGetClusterConfigFields() {
        when(databricksAPIService.getDatabricksClusterConfigFields("projectId")).thenReturn(Map.of());

        assertEquals(Map.of(), utilController.getClusterConfigFields("projectId"));

        verify(databricksAPIService).getDatabricksClusterConfigFields(anyString());
    }
}

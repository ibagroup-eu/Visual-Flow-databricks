package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    @Mock
    private AuthenticationService authenticationServiceMock;

    private UserController controller;
    private final UserInfo USER = UserInfo.builder().name("name").id("id").username("username").email("email").superuser(true).build();

    @BeforeEach
    void setUp() {
        controller = new UserController(authenticationServiceMock);
        when(authenticationServiceMock.getUserInfo()).thenReturn(USER);
    }

    @Test
    void testWhoAmI() {
        UserInfo actual = controller.getUser();
        assertEquals(USER, actual, "UserInfo must be equals to expected");
        verify(authenticationServiceMock, times(2)).getUserInfo();
    }
}

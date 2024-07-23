package eu.ibagroup.vfdatabricks.services.auth;

import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {
    private static final UserInfo USER_INFO = new UserInfo();
    private SecurityContext securityContextMock;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        securityContextMock = mock(SecurityContext.class, RETURNS_DEEP_STUBS);
        service = new AuthenticationService();
        USER_INFO.setId("id");
        USER_INFO.setEmail("email");
        USER_INFO.setName("name");
        USER_INFO.setUsername("username");
        USER_INFO.setSuperuser(true);
    }

    @Test
    void testGetUserInfo() {
        Authentication authentication = mock(Authentication.class);
        when(securityContextMock.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContextMock);
        when(securityContextMock.getAuthentication().getPrincipal()).thenReturn(USER_INFO);

        UserInfo fromContext = service.getUserInfo();
        assertEquals(USER_INFO, fromContext, "UserInfo must be equals to expected");

        verify(securityContextMock, times(2)).getAuthentication();
        verify(securityContextMock.getAuthentication()).getPrincipal();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testGetUserInfoAuthenticationException(boolean isNull) {
        Object returnValue = isNull ? null : "some str object";
        Authentication authentication = mock(Authentication.class);
        when(securityContextMock.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContextMock);
        when(securityContextMock.getAuthentication().getPrincipal()).thenReturn(returnValue);

        assertThrows(AuthenticationException.class, () -> service.getUserInfo(), "Expected exception must be thrown");
    }

    @Test
    void testSetUserInfo() {
        SecurityContextHolder.setContext(securityContextMock);
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        service.setUserInfo(USER_INFO);
        verify(securityContextMock).setAuthentication(captor.capture());

        UserInfo fromContext = (UserInfo) captor.getValue().getPrincipal();
        assertEquals(USER_INFO, fromContext, "UserInfo must be equals to expected");
    }
}

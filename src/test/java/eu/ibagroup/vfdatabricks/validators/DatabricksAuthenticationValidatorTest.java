package eu.ibagroup.vfdatabricks.validators;

import eu.ibagroup.vfdatabricks.dto.projects.DatabricksAuthentication;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabricksAuthenticationValidatorTest {

    private DatabricksAuthenticationValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new DatabricksAuthenticationValidator();
    }

    @Test
    void testIsValidNullAuthentication() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void testIsValidPATValidToken() {
        DatabricksAuthentication auth = DatabricksAuthentication.builder().build();
        auth.setAuthenticationType(DatabricksAuthentication.AuthenticationType.PAT);
        auth.setToken("valid_token");

        assertTrue(validator.isValid(auth, context));
    }

    @Test
    void testIsValidPATNullToken() {
        DatabricksAuthentication auth = DatabricksAuthentication.builder().build();
        auth.setAuthenticationType(DatabricksAuthentication.AuthenticationType.PAT);
        auth.setToken(null);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(auth, context));
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Personal Access Token must not be null");
        verify(violationBuilder).addPropertyNode("personalAccessToken");
    }

    @Test
    void testIsValidOAuthValidCredentials() {
        DatabricksAuthentication auth = DatabricksAuthentication.builder().build();
        auth.setAuthenticationType(DatabricksAuthentication.AuthenticationType.OAUTH);
        auth.setClientId("client_id");
        auth.setSecret("client_secret");

        assertTrue(validator.isValid(auth, context));
    }

    @Test
    void testIsValidOAuthNullClientId() {
        DatabricksAuthentication auth = DatabricksAuthentication.builder().build();
        auth.setAuthenticationType(DatabricksAuthentication.AuthenticationType.OAUTH);
        auth.setClientId(null);
        auth.setSecret("client_secret");
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(auth, context));
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Client ID must not be null");
        verify(violationBuilder).addPropertyNode("clientId");
    }

    @Test
    void testIsValidOAuthNullSecret() {
        DatabricksAuthentication auth = DatabricksAuthentication.builder().build();
        auth.setAuthenticationType(DatabricksAuthentication.AuthenticationType.OAUTH);
        auth.setClientId("client_id");
        auth.setSecret(null);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(auth, context));
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Secret must not be null");
        verify(violationBuilder).addPropertyNode("secret");
    }

    @Test
    void testIsValidInvalidAuthenticationType() {
        DatabricksAuthentication auth = DatabricksAuthentication.builder().build();
        auth.setAuthenticationType(null);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(auth, context));
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("AuthenticationType is invalid");
        verify(violationBuilder).addPropertyNode("authenticationType");
    }
}

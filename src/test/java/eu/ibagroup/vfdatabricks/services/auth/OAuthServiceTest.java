package eu.ibagroup.vfdatabricks.services.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
class OAuthServiceTest {
    private static final String AUTH_ID = "id";
    private static final String AUTH_USERNAME = "username";
    private static final String AUTH_NAME = "name";
    private static final String AUTH_EMAIL = "email";
    @Mock
    private RestTemplate restTemplateMock;
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthService(
                restTemplateMock,
                new MockEnvironment()
                        .withProperty("auth.id", AUTH_ID)
                        .withProperty("auth.username", AUTH_USERNAME)
                        .withProperty("auth.name", AUTH_NAME)
                        .withProperty("auth.email", AUTH_EMAIL),
                appProperties);
    }

    @Test
    void testGetUserInfoByToken() {
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpMethod> methodCaptor = ArgumentCaptor.forClass(HttpMethod.class);
        ArgumentCaptor<HttpEntity<Object>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        ArgumentCaptor<Class<JsonNode>> jsonCaptor = ArgumentCaptor.forClass(Class.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode nodes = mapper.createObjectNode();
        Map
                .of(AUTH_ID, "test", AUTH_USERNAME, "tester", AUTH_NAME, "abc", AUTH_EMAIL, "test@test.com")
                .forEach((k, v) -> nodes.set(k, new TextNode(v)));

        when(restTemplateMock.exchange(uriCaptor.capture(),
                methodCaptor.capture(),
                entityCaptor.capture(),
                jsonCaptor.capture())).thenReturn(ResponseEntity.ok(nodes));
        oAuthService.getUserInfoByToken("token");
        assertEquals("https://path/user", uriCaptor.getValue(), "Value must be equals to expected");
        assertEquals(HttpMethod.GET, methodCaptor.getValue(), "Value must be equals to expected");
        assertEquals("Bearer token",
                Objects.requireNonNull(entityCaptor.getValue().getHeaders().get("Authorization")).get(0),
                "Headers must be equal to expected");
        assertEquals(JsonNode.class, jsonCaptor.getValue(), "Argument should have JsonNode type");
    }
}

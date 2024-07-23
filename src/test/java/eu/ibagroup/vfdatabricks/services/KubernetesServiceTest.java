package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
public class KubernetesServiceTest {
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    @Mock
    private AuthenticationService authenticationServiceMock;
    private KubernetesService kubernetesService;
    private final KubernetesServer server = new KubernetesServer();

    @BeforeEach
    void setUp() {
        server.before();
        kubernetesService = new KubernetesService(appProperties, server.getClient());
    }

    @AfterEach
    void tearDown() {
        server.after();
    }

    @Test
    void testCreateOrUpdateSecret() {
        String namespace = "namespace";
        Secret secret = new SecretBuilder()
                .editOrNewMetadata()
                .withName("imagePullSecret")
                .endMetadata()
                .build();

        server
                .expect()
                .post()
                .withPath("/api/v1/namespaces/vf-dev-test/secrets")
                .andReturn(HttpURLConnection.HTTP_CREATED, null)
                .once();

        kubernetesService.createSecret(namespace, secret);
    }

    @Test
    void testGetParams() {
        Secret expected = new SecretBuilder()
                .editOrNewMetadata()
                .withName("imagePullSecret")
                .endMetadata()
                .build();

        server
                .expect()
                .get()
                .withPath("/api/v1/namespaces/vf-dev-test/secrets/imagePullSecret")
                .andReturn(HttpURLConnection.HTTP_OK, expected)
                .once();

        Secret result = kubernetesService.getSecret("imagePullSecret");
        assertEquals(expected, result, "Secret must be equals to expected");
    }

    @Test
    void testGetSecretsByLabels() {
        Secret secret = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("secret1")
                        .addToLabels("jobId", "name1")
                        .build())
                .build();

        server
                .expect()
                .get()
                .withPath("/api/v1/namespaces/vf-dev-test/secrets?labelSelector=jobId%3Dname1")
                .andReturn(HttpURLConnection.HTTP_OK, new SecretListBuilder().addToItems(secret).build())
                .once();

        List<Secret> result = kubernetesService.getSecretsByLabels(Map.of("jobId", "name1"));
        assertEquals(secret, result.get(0), "Secret must be equals to expected");
    }

    @Test
    void testDeleteSecretsByLabels() {
        String namespace = "namespace";
        server
                .expect()
                .delete()
                .withPath("/api/v1/namespaces/namespace/secrets?labelSelector=jobId%3Dname1")
                .andReturn(HttpURLConnection.HTTP_OK, null)
                .once();

        kubernetesService.deleteSecret(namespace);
    }
}

package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.dto.DatabricksOAuthResponseDto;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class DatabricksAuthorizationServiceTest {

    @Mock
    private KubernetesService kubernetesService;
    @Mock
    private RestTemplate databricksRestTemplate;
    @Mock
    private Secret secret;

    private DatabricksAuthorizationService databricksAuthorizationService;

    @BeforeEach
    void setUp() {
        Map<String, String> map = new HashMap<>();
        map.put("hash", "hash");
        map.put("token", Base64.getEncoder().encodeToString("token".getBytes()));
        map.put("host", Base64.getEncoder().encodeToString("host".getBytes()));
        when(secret.getData()).thenReturn(map);
        databricksAuthorizationService = new DatabricksAuthorizationService(kubernetesService, databricksRestTemplate);
    }

    @Test
    void testGetOAuthToken() {
        when(kubernetesService.getSecret("projectId")).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(DatabricksOAuthResponseDto.class))).thenReturn(ResponseEntity.ok(DatabricksOAuthResponseDto.builder().accessToken("token").build()));
        assertEquals("token", databricksAuthorizationService.getOAuthToken("projectId"));
    }

}

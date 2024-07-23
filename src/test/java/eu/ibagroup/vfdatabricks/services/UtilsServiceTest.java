package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.dto.NodeType;
import eu.ibagroup.vfdatabricks.dto.NodeTypeList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UtilsServiceTest {
    private static final String PROJECT_NAME = "project name";
    private static final String PROJECT_ID = "vf-project-name";
    private static final String HOST_VALUE = "aG9zdA==";
    private static final String TOKEN_VALUE = "dG9rZW4=";
    @Mock
    private RestTemplate databricksRestTemplate;
    @Mock
    private KubernetesService kubernetesService;
    private UtilsService utilsService;
    private final Secret secret = new SecretBuilder()
            .addToData(HOST, HOST_VALUE)
            .addToData(TOKEN, TOKEN_VALUE)
            .addToData(CLOUD, "QVdT")
            .editOrNewMetadata()
            .withName(PROJECT_ID)
            .addToAnnotations(NAME, PROJECT_NAME)
            .addToAnnotations(DESCRIPTION, DESCRIPTION)
            .endMetadata()
            .build();

    @BeforeEach
    void setUp() {
        this.utilsService = new UtilsService(databricksRestTemplate, kubernetesService);
    }

    @Test
    void testGetDatabricksClusterConfigFields() {
        when(kubernetesService.getSecret(PROJECT_ID)).thenReturn(secret);
        List<LinkedHashMap<String, String>> expectedPolicies = new ArrayList<>();
        LinkedHashMap<String, String> mapOfExpectedPolicies1 = new LinkedHashMap<>();
        expectedPolicies.add(mapOfExpectedPolicies1);
        LinkedHashMap<String, String> mapOfExpectedPolicies2 = new LinkedHashMap<>();
        mapOfExpectedPolicies2.put("someField", "someValue");
        expectedPolicies.add(mapOfExpectedPolicies2);
        mapOfExpectedPolicies1.put("policy_family_id", "job-cluster");
        Map<String, Object> expected = Map.of(
                "policies", expectedPolicies,
                "versions", "versionValue",
                "node_types", List.of(NodeType.builder().build()),
                "zones", "zoneValue",
                "instance_profiles", "instance_profilesValue");
        List<LinkedHashMap<String, String>> policies = new ArrayList<>();
        LinkedHashMap<String, String> mapOfPolicies1 = new LinkedHashMap<>();
        mapOfPolicies1.put("policy_family_id", "job-cluster");
        policies.add(mapOfPolicies1);
        LinkedHashMap<String, String> mapOfPolicies2 = new LinkedHashMap<>();
        mapOfPolicies2.put("someField", "someValue");
        policies.add(mapOfPolicies2);
        LinkedHashMap<String, String> mapOfPolicies3 = new LinkedHashMap<>();
        mapOfPolicies3.put("policy_family_id", "other");
        policies.add(mapOfPolicies3);
        when(databricksRestTemplate.exchange(
                eq(String.format("%s/%s/policies/clusters/list", decodeFromBase64(HOST_VALUE), DATABRICKS_JOBS_API_20)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("policies", policies)));

        when(databricksRestTemplate.exchange(
                eq(String.format("%s/%s/clusters/spark-versions", decodeFromBase64(HOST_VALUE), DATABRICKS_JOBS_API_20)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("versions", "versionValue")));

        when(databricksRestTemplate.exchange(
                eq(String.format("%s/%s/clusters/list-node-types", decodeFromBase64(HOST_VALUE), DATABRICKS_JOBS_API_20)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(NodeTypeList.class)))
                .thenReturn(ResponseEntity.ok(NodeTypeList.builder().nodeTypes(List.of(NodeType.builder().build())).build()));

        when(databricksRestTemplate.exchange(
                eq(String.format("%s/%s/clusters/list-zones", decodeFromBase64(HOST_VALUE), DATABRICKS_JOBS_API_20)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("zones", "zoneValue")));

        when(databricksRestTemplate.exchange(
                eq(String.format("%s/%s/instance-profiles/list", decodeFromBase64(HOST_VALUE), DATABRICKS_JOBS_API_20)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("instance_profiles", "instance_profilesValue")));

        assertEquals(expected,
                utilsService.getDatabricksClusterConfigFields(PROJECT_ID), "Objects must be equals");
    }
}

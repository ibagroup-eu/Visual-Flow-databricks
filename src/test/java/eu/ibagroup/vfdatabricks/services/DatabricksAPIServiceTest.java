package eu.ibagroup.vfdatabricks.services;

import com.google.common.cache.LoadingCache;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.NodeType;
import eu.ibagroup.vfdatabricks.dto.NodeTypeList;
import eu.ibagroup.vfdatabricks.dto.jobs.JobParams;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.*;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterValue;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineParams;
import eu.ibagroup.vfdatabricks.exceptions.ForRetryRestTemplateException;
import eu.ibagroup.vfdatabricks.model.Parameter;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static eu.ibagroup.vfdatabricks.dto.Constants.DATABRICKS_JOBS_API_20;
import static eu.ibagroup.vfdatabricks.services.UtilsService.decodeFromBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
class DatabricksAPIServiceTest {

    @Mock
    private KubernetesService kubernetesService;

    @Mock
    private RestTemplate databricksRestTemplate;

    @Autowired
    private ApplicationConfigurationProperties appProperties;

    @Mock
    private LoadingCache<String, String> tokenCache;


    private DatabricksAPIService databricksApiService;

    @Mock
    private Secret secret;

    private static final String PROJECT_ID = "vf-project-name";
    private static final String HOST_VALUE = "aG9zdA==";

    @BeforeEach
    void setUp() {
        Map<String, String> map = new HashMap<>();
        map.put("hash", "hash");
        map.put("token", Base64.getEncoder().encodeToString("token".getBytes()));
        map.put("host", Base64.getEncoder().encodeToString("host".getBytes()));
        map.put("cloud", Base64.getEncoder().encodeToString("AWS".getBytes()));
        map.put("authType", Base64.getEncoder().encodeToString("PAT".getBytes()));
        when(secret.getData()).thenReturn(map);
        databricksApiService = new DatabricksAPIService(kubernetesService, databricksRestTemplate, appProperties, tokenCache);
    }

    @Test
    void shouldRunJobSuccessfully() {
        DatabricksJobStorageRunDto body = new DatabricksJobStorageRunDto();
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        DatabricksRunIdDto expected = new DatabricksRunIdDto();
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(DatabricksRunIdDto.class)))
                .thenReturn( ResponseEntity.ok(expected));
        JobParams params = new JobParams();
        params.setIntervals("5");
        params.setUpTo("10");

        DatabricksRunIdDto result = databricksApiService.runJob("projectId", body, params).join();

        assertEquals(expected, result);
        verify(databricksRestTemplate).exchange(eq("host/api/2.1/jobs/runs/submit"), eq(HttpMethod.POST), any(), eq(DatabricksRunIdDto.class));
    }
    @Test
    void shouldRunJobSuccessfullyException() {
        DatabricksJobStorageRunDto body = new DatabricksJobStorageRunDto();
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        DatabricksRunIdDto expected = new DatabricksRunIdDto();
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(DatabricksRunIdDto.class)))
                .thenThrow(new ForRetryRestTemplateException("message")).thenReturn( ResponseEntity.ok(expected));
        PipelineParams params = new PipelineParams();
        params.setIntervals("5");
        params.setUpTo("10");

        DatabricksRunIdDto result = databricksApiService.runJob("projectId", body, params).join();

        assertEquals(expected, result);
        verify(databricksRestTemplate, times(2)).exchange(eq("host/api/2.1/jobs/runs/submit"), eq(HttpMethod.POST), any(), eq(DatabricksRunIdDto.class));
    }

    @Test
    void shouldCancelJobSuccessfully() {
        String projectId = "projectId";
        long runId = 123L;
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        databricksApiService.cancelJob(projectId, runId);

        verify(databricksRestTemplate).exchange(eq("host/api/2.1/jobs/runs/cancel"), eq(HttpMethod.POST), any(), eq(Void.class));
    }

    @Test
    void shouldCheckJobStatusSuccessfully() {
        String projectId = "projectId";
        long runId = 123L;

        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        DatabricksJobRunDto expected = new DatabricksJobRunDto();
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(DatabricksJobRunDto.class)))
                .thenReturn(ResponseEntity.ok(expected));

        DatabricksJobRunDto result = databricksApiService.checkJobStatus(projectId, runId);

        assertEquals(expected, result);
        verify(databricksRestTemplate).exchange(eq("host/api/2.1/jobs/runs/get?run_id=123"), eq(HttpMethod.GET), any(), eq(DatabricksJobRunDto.class));
    }

    @Test
    void testGetClusterInfo() {
        String projectId = "projectId";
        long runId = 123L;

        when(kubernetesService.getSecret(anyString())).thenReturn(secret);

        DatabricksJobClusterDto expected = new DatabricksJobClusterDto();
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(
                DatabricksJobClusterDto.class)))
                .thenReturn(ResponseEntity.ok(expected));


        DatabricksJobClusterDto result = databricksApiService.getClusterInfo(projectId, runId);

        assertEquals(expected, result);
        verify(databricksRestTemplate).exchange(eq("host/api/2.1/jobs/runs/get?run_id=123"), eq(HttpMethod.GET), any(), eq(DatabricksJobClusterDto.class));
    }

    @Test
    void testGetJobLogs() {
        String projectId = "projectId";
        String clusterId = "clusterID";

        when(kubernetesService.getSecret(anyString())).thenReturn(secret);


        DatabricksJobLogDto expected = new
                DatabricksJobLogDto();
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(

                DatabricksJobLogDto.class)))
                .thenReturn(ResponseEntity.ok(expected));



        DatabricksJobLogDto result = databricksApiService.getJobLogs(projectId, clusterId);

        assertEquals(expected, result);
        verify(databricksRestTemplate).exchange(eq("host/api/2.0/dbfs/read?path=/logStore/log/" + clusterId + "/driver/log4j-active.log"), eq(HttpMethod.GET), any(), eq(
                DatabricksJobLogDto.class));
    }

    @Test
    void testUploadFile() {
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(eq("host/api/2.0/fs/directoriespath"),
                eq(HttpMethod.PUT),
                any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        byte[] fileBytes = "testBytes".getBytes();
        databricksApiService.uploadFile("projectId", "path", fileBytes, "fileName");
        verify(databricksRestTemplate, times(1)).exchange(eq("host/api/2.0/fs/directoriespath"), eq(HttpMethod.PUT), any(), eq(
                Object.class));

    }

    @Test
    void testCreateDirectory() {
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(
                Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        databricksApiService.createDirectory("projectId", "path");
        verify(databricksRestTemplate, times(1)).exchange(eq("host/api/2.0/fs/directoriespath"), eq(HttpMethod.PUT), any(), eq(
                Object.class));

    }

    @Test
    void testCreateSecretScope() {
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        databricksApiService.createSecretScope("projectId");
        verify(databricksRestTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(
                Object.class));
    }

    @Test
    void testDeleteSecretScope() {
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        databricksApiService.deleteSecretScope("projectId");
        verify(databricksRestTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(
                Object.class));
    }

    @Test
    void testAddSecret() {
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        databricksApiService.addSecret("projectId", Parameter.builder().value(ParameterValue.builder().text("123").build()).build());
        verify(databricksRestTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(
                Object.class));
    }

    @Test
    void testDeleteSecret() {
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        databricksApiService.deleteSecret("projectId", "id");
        verify(databricksRestTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(
                Object.class));
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
                databricksApiService.getDatabricksClusterConfigFields(PROJECT_ID), "Objects must be equals");
    }

}
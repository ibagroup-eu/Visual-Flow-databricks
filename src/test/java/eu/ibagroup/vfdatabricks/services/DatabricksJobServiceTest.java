package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.jobs.JobParams;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.*;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineParams;
import eu.ibagroup.vfdatabricks.exceptions.ForRetryRestTemplateException;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
class DatabricksJobServiceTest {

    @Mock
    private KubernetesService kubernetesService;

    @Mock
    private RestTemplate databricksRestTemplate;

    @Autowired
    private ApplicationConfigurationProperties appProperties;


    private DatabricksJobService databricksApiService;

    @Mock
    private Secret secret;

    @BeforeEach
    void setUp() {
        Map<String, String> map = new HashMap<>();
        map.put("hash", "hash");
        map.put("token", Base64.getEncoder().encodeToString("token".getBytes()));
        map.put("host", Base64.getEncoder().encodeToString("host".getBytes()));
        when(secret.getData()).thenReturn(map);
        databricksApiService = new DatabricksJobService(kubernetesService, databricksRestTemplate, appProperties);
    }

    @Test
    void shouldRunJobSuccessfully() {
        DatabricksJobStorageRunDto body = new DatabricksJobStorageRunDto();
        when(kubernetesService.getSecret(anyString())).thenReturn(secret);
        DatabricksRunIdDto expected = new DatabricksRunIdDto();
        when(databricksRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(DatabricksRunIdDto.class)))
                .thenReturn( ResponseEntity.ok(expected));

        DatabricksRunIdDto result = databricksApiService.runJob("projectId", body, new JobParams()).join();

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

        DatabricksRunIdDto result = databricksApiService.runJob("projectId", body, new PipelineParams()).join();

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

}
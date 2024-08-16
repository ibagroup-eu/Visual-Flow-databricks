package eu.ibagroup.vfdatabricks.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.jobs.*;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.*;
import eu.ibagroup.vfdatabricks.exceptions.ForRetryRestTemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
class JobServiceTest {
    private static final String PROJECT_ID = "vf-project-name";
    private static final String JOB_ID = "test-job";
    @Mock
    private DatabricksAPIService databricksApiService;
    @Mock
    private MapperService mapperService;
    @Mock
    private RestTemplate restTemplate;
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    @Mock
    private AsyncJobCheckService asyncJobCheckService;
    private JobService jobService;
    private SecurityContext securityContextMock;

    @BeforeEach
    void setUp() {
        securityContextMock = mock(SecurityContext.class, RETURNS_DEEP_STUBS);
        this.jobService = new JobService(mapperService, databricksApiService, appProperties, restTemplate, asyncJobCheckService);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContextMock);
        when(securityContextMock.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void testGetAll() throws InterruptedException {
        JobOverviewListDto expected = JobOverviewListDto.builder().jobs(List.of(JobOverviewDto.builder().build())).build();

        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                eq(JobOverviewListDto.class)))
                .thenReturn(ResponseEntity.ok(JobOverviewListDto.builder().jobs(List.of(JobOverviewDto.builder().status("Pending").build())).build()));
        when(asyncJobCheckService.checkAndUpdateStatus(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(DatabricksJobRunDto.builder().build()));

        assertEquals(expected, jobService.getAll(PROJECT_ID), "Objects must be equal");
        verify(restTemplate).getForEntity(anyString(), eq(JobOverviewListDto.class));
    }

    @Test
    void testGetAndFetchStatus() throws InterruptedException {
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID
                )),
                eq(JobDto.class))).thenReturn(ResponseEntity.ok(JobDto.builder().runId(123L).status("Pending").build()));
        when(asyncJobCheckService.checkAndUpdateStatus(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(DatabricksJobRunDto.builder().build()));

        assertEquals(JobDto.builder().build(), jobService.getAndFetchStatus(PROJECT_ID, JOB_ID), "Objects must be equal");
        verify(restTemplate).getForEntity(anyString(), eq(JobDto.class));
    }

    @Test
    void testGetAndFetchStatusWithHistory() throws InterruptedException, IOException {
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID
                )),
                eq(JobDto.class))).thenReturn(ResponseEntity.ok(JobDto.builder().id(JOB_ID).runId(123L).status("Pending").build()));
        when(asyncJobCheckService.checkAndUpdateStatus(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(DatabricksJobRunDto.builder().state(DatabricksJobState.builder().resultState("Succeeded").build()).startTime(1231432L).endTime(1231434L).build()));
        when(restTemplate.postForEntity(eq(String.format("%s/%s/%s/%s/job/%s/status?status=%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID,
                        "Pending")), any(), any())).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));

        Path file = Path.of("", "src/test/resources").resolve("logs.json");
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.readValue(Files.readString(file), JsonNode.class).get("data").asText();
        when(databricksApiService.getClusterInfo(PROJECT_ID, 123L))
                .thenReturn(DatabricksJobClusterDto.builder()
                        .tasks(List.of(DatabricksJobClusterDto.Task.builder()
                                .clusterInstance(DatabricksJobClusterDto.ClusterInstance.
                                        builder()
                                        .clusterId("clusterId")
                                        .build())
                                .build()))
                        .build());

        when(databricksApiService.getJobLogs(PROJECT_ID, "clusterId"))
                .thenReturn(DatabricksJobLogDto.builder().data(data).build());
        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/history/job/%s/log",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        JOB_ID)),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(""));

        assertEquals(JobDto.builder().build(), jobService.getAndFetchStatus(PROJECT_ID, JOB_ID), "Objects must be equal");
        verify(restTemplate, times(2)).getForEntity(anyString(), eq(JobDto.class));
    }

    @Test
    void testCreate() {
        JobDto jobDto = JobDto.builder().build();

        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/%s/job",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                any(),
                eq(String.class))).thenReturn(ResponseEntity.ok(JOB_ID));

        assertEquals(JOB_ID, jobService.create(PROJECT_ID, jobDto), "Objects must be equal");
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testUpdate() {
        JobDto jobDto = JobDto.builder().build();

        jobService.update(PROJECT_ID, JOB_ID, jobDto);
        verify(restTemplate).postForEntity(
                eq("https://localhost:8080/vf/be/api/project/vf-project-name/job/test-job"),
                any(),
                eq(Void.class)
        );
    }

    @Test
    void testDelete() {
        jobService.delete(PROJECT_ID, JOB_ID);

        verify(restTemplate).delete(anyString(), eq(Object.class));
    }

    @Test
    void testRun() {
        JobParams params = JobParams.builder().intervals("2").upTo("10").build();
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID
                )),
                eq(JobDto.class)))
                .thenReturn(ResponseEntity.ok(JobDto
                        .builder()
                        .params(params)
                        .build()));
        when(databricksApiService.runJob(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        DatabricksRunIdDto.builder().runId(123).build()
                ));

        jobService.run(PROJECT_ID, JOB_ID);

        verify(restTemplate).getForEntity(anyString(), eq(JobDto.class));
        verify(databricksApiService).runJob(eq(PROJECT_ID), any(), eq(params));
    }

    @Test
    void testRunException() {
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID
                )),
                eq(JobDto.class)))
                .thenReturn(ResponseEntity.ok(JobDto
                        .builder()
                        .params(JobParams.builder().intervals("2").upTo("10").build())
                        .build()));
        when(databricksApiService.runJob(anyString(), any(), any() ))
                .thenReturn(CompletableFuture.failedFuture(new ForRetryRestTemplateException("message")));

        jobService.run(PROJECT_ID, JOB_ID);

        verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(Void.class));

    }

    @Test
    void testStop() {
        int runId = 123;
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID
                )),
                eq(JobDto.class)))
                .thenReturn(ResponseEntity.ok(JobDto.builder().runId(runId).build()));

        jobService.stop(PROJECT_ID, JOB_ID);

        verify(restTemplate).getForEntity(anyString(), eq(JobDto.class));
        verify(databricksApiService).cancelJob(PROJECT_ID, runId);
    }

    @Test
    void testGetJobHistory() {
        JobHistoryDto[] jobHistoryDtos = {JobHistoryDto.builder().jobId("123456789").build()};

        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/history/job/%s",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        "123456789")),
                eq(JobHistoryDto[].class))).thenReturn(ResponseEntity.ok(jobHistoryDtos));

        assertEquals("123456789",
                jobService.getJobHistory("123456789").get(0).getJobId(), "Objects must be equals");
    }

    @Test
    void testGetJobLogs() throws IOException {
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        "projectId",
                        "jobId"
                )),
                eq(JobDto.class))).thenReturn(ResponseEntity.ok(JobDto.builder().runId(123L).status("Pending").build()));

        Path file = Path.of("", "src/test/resources").resolve("logs.json");
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.readValue(Files.readString(file), JsonNode.class).get("data").asText();
        when(databricksApiService.getClusterInfo("projectId", 123L))
                .thenReturn(DatabricksJobClusterDto.builder()
                        .tasks(List.of(DatabricksJobClusterDto.Task.builder()
                                .clusterInstance(DatabricksJobClusterDto.ClusterInstance.
                                        builder()
                                        .clusterId("clusterId")
                                        .build())
                                .build()))
                        .build());

        when(databricksApiService.getJobLogs("projectId", "clusterId"))
                .thenReturn(DatabricksJobLogDto.builder().data(data).build());


        assertEquals("SparkHadoopUtil: Installing CredentialsScopeFilesystem for scheme s3. Previous value: com.databricks.common.filesystem.LokiFileSystem",
                jobService.getJobLogs("projectId", "jobId").get(0).getMessage(), "Objects must be equals");
    }

    @Test
    void testGetJobLogsWhenFailed() {
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        "projectId",
                        "jobId"
                )),
                eq(JobDto.class))).thenReturn(ResponseEntity.ok(JobDto.builder().runId(123L).status("Failed").build()));

        when(restTemplate.getForEntity(eq(String.format("%s/%s/%s/history/job/%s/log/last",
                appProperties.getHistoryService().getHost(),
                CONTEXT_PATH_HISTORY,
                HISTORY_SERVICE_API,
                "jobId")), eq(JobLogDto[].class)))
                .thenReturn(ResponseEntity.ok(new JobLogDto[]{JobLogDto.builder().message("SparkHadoopUtil: Installing CredentialsScopeFilesystem for scheme s3. Previous value: com.databricks.common.filesystem.LokiFileSystem").build()}));



        assertEquals("SparkHadoopUtil: Installing CredentialsScopeFilesystem for scheme s3. Previous value: com.databricks.common.filesystem.LokiFileSystem",
                jobService.getJobLogs("projectId", "jobId").get(0).getMessage(), "Objects must be equals");
    }

    @Test
    void testUpdateJobStatus() {
        CommonDto dto = CommonDto.builder().status("status").build();
        jobService.updateJobStatus("id", dto);
        verify(restTemplate).postForEntity("https://localhost:8080/vf/be/api/project/id/job/null/status?status=status", dto, Void.class);
    }

    @Test
    void testSaveHistory() throws IOException {
        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/job/%s",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID,
                        JOB_ID
                )),
                eq(JobDto.class))).thenReturn(ResponseEntity.ok(JobDto.builder().runId(123L).status("Pending").build()));

        Path file = Path.of("", "src/test/resources").resolve("logs.json");
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.readValue(Files.readString(file), JsonNode.class).get("data").asText();
        when(databricksApiService.getClusterInfo(PROJECT_ID, 123L))
                .thenReturn(DatabricksJobClusterDto.builder()
                        .tasks(List.of(DatabricksJobClusterDto.Task.builder()
                                .clusterInstance(DatabricksJobClusterDto.ClusterInstance.
                                        builder()
                                        .clusterId("clusterId")
                                        .build())
                                .build()))
                        .build());

        when(databricksApiService.getJobLogs(PROJECT_ID, "clusterId"))
                .thenReturn(DatabricksJobLogDto.builder().data(data).build());
        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/history/job/%s/log",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        JOB_ID)),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(""));
        jobService.saveHistory(DatabricksJobRunDto.builder().startTime(1716097075861L).endTime(1716097407535L).build(), CommonDto.builder().id(JOB_ID).build(), PROJECT_ID);
        verify(restTemplate).postForEntity(eq(String.format("%s/%s/%s/history/job",
                appProperties.getHistoryService().getHost(),
                CONTEXT_PATH_HISTORY,
                HISTORY_SERVICE_API)), any(JobHistoryDto.class), eq(String.class));
    }

    @Test
    void testGetJobLogsHistory() {
        JobLogDto[] jobLogDtos = {JobLogDto.builder().message("message").build()};

        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/history/job/%s/log/%s",
                        appProperties.getHistoryService().getHost(),
                        CONTEXT_PATH_HISTORY,
                        HISTORY_SERVICE_API,
                        "jobId",
                        "logId")),
                eq(JobLogDto[].class))).thenReturn(ResponseEntity.ok(jobLogDtos));

        assertEquals("message",
                jobService.getJobLogsHistory("jobId", "logId").get(0).getMessage(), "Objects must be equals");
    }

    @Test
    void testSaveCustomJobLog() {
        jobService.saveCustomJobLog("jobId", List.of());
        verify(restTemplate).postForEntity(eq(String.format("%s/%s/%s/history/job/%s/log",
                appProperties.getHistoryService().getHost(),
                CONTEXT_PATH_HISTORY,
                HISTORY_SERVICE_API,
                "jobId")), eq(List.of()), eq(String.class));
    }
}

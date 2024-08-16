package eu.ibagroup.vfdatabricks.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.*;
import eu.ibagroup.vfdatabricks.dto.notifications.EmailNotification;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    AsyncJobCheckService asyncJobCheckService;
    @Mock
    MapperService mapperService;
    @Mock
    DatabricksAPIService databricksApiService;
    @Mock
    private RestTemplate restTemplate;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private ApplicationConfigurationProperties appProperties;
    @Mock
    private JobService jobService;
    @Mock
    private CronCheckService cronCheckService;
    @Mock
    private SchedulerService schedulerService;

    @Spy
    @InjectMocks
    private PipelineService pipelineService;

    @Test
    void shouldCreatePipelineSuccessfully() {
        String projectId = "projectId";
        PipelineDto pipelineRequestDto = new PipelineDto();
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Pipeline Created Successfully"));

        String result = pipelineService.create(projectId, pipelineRequestDto);

        assertEquals("Pipeline Created Successfully", result);
    }

    @Test
    void shouldGetPipelineByIdSuccessfully() {
        String projectId = "projectId";
        String id = "id";
        PipelineDto pipelineDto = new PipelineDto();
        when(restTemplate.getForEntity(anyString(), eq(PipelineDto.class)))
                .thenReturn(ResponseEntity.ok(pipelineDto));
        when(asyncJobCheckService.checkAndUpdateStatus(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PipelineDto result = pipelineService.getByIdAndFetchStatus(projectId, id);

        assertEquals(pipelineDto, result);
    }

    @Test
    void shouldUpdatePipelineSuccessfully() {
        String projectId = "projectId";
        String id = "id";
        PipelineDto pipelineDto = new PipelineDto();

        pipelineService.update(projectId, id, pipelineDto);

        verify(restTemplate).put(argThat((String uri) -> uri.endsWith("/vf/be/api/project/projectId/pipeline/id")), eq(pipelineDto));
    }

    @Test
    void shouldDeletePipelineSuccessfully() {

        String projectId = "projectId";
        String id = "id";

        pipelineService.delete(projectId, id);

        verify(restTemplate).delete(argThat((String uri) -> uri.endsWith("/vf/be/api/project/projectId/pipeline/id")));
        verify(schedulerService).deleteCron(projectId, id);
    }

    @Test
    void shouldPatchPipelineSuccessfully() {
        String projectId = "projectId";
        CommonDto pipelineRequestDto = new CommonDto();
        pipelineRequestDto.setId("id");

        pipelineService.patch(projectId, pipelineRequestDto);

        verify(restTemplate).patchForObject(anyString(), any(CommonDto.class), eq(Void.class));
    }

    @Test
    void shouldRunPipelineSuccessfully() throws JsonProcessingException {
        String projectId = "projectId";
        String id = "id";
        PipelineDto pipelineDto = new PipelineDto();
        JsonNode definition = objectMapper.readTree("{\"graph\": [{\"id\":\"id\", \"vertex\":true, \"value\": {\"operation\": \"JOB\", \"name\": \"name\", \"jobId\": \"jobId\"}}]}");
        pipelineDto.setDefinition(definition);
        pipelineDto.setParams(PipelineParams.builder()
                .email(EmailNotification.builder().build())
                .build());
        when(restTemplate.getForEntity(anyString(), eq(PipelineDto.class)))
                .thenReturn(ResponseEntity.ok(pipelineDto));
        when(jobService.getJob(anyString(), anyString())).thenReturn(new JobDto());
        when(mapperService.mapJobDtoToDatabricksJobTask(any(JobDto.class), anyString()))
                .thenReturn(new DatabricksJobTask());
        when(databricksApiService.runJob(anyString(), any(DatabricksJobStorageRunDto.class), any()))
                .thenReturn(CompletableFuture.completedFuture(new DatabricksRunIdDto()));
        when(asyncJobCheckService.checkAndUpdateStatus(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        pipelineService.run(projectId, id);

        verify(restTemplate, times(2)).put(anyString(), any(PipelineDto.class));
    }

    @Test
    void shouldTerminatePipelineSuccessfully() {
        String projectId = "projectId";
        String id = "id";
        PipelineDto pipelineDto = new PipelineDto();
        pipelineDto.setRunId(123);
        when(restTemplate.getForEntity(anyString(), eq(PipelineDto.class)))
                .thenReturn(ResponseEntity.ok(pipelineDto));

        pipelineService.terminate(projectId, id);

        verify(databricksApiService).cancelJob(projectId, 123);
    }

    @Test
    void shouldUpdateJobStatusesWhenPipelineIsNotInstanceOfPipelineDto() throws JsonProcessingException {
        String pipelineId = "id";
        PipelineOverviewDto pipeline = PipelineOverviewDto.builder().id(pipelineId).build();
        DatabricksJobRunDto result = DatabricksJobRunDto.builder()
                .tasks(List.of(
                        DatabricksJobTask.builder()
                                .taskKey("name-id")
                                .state(new DatabricksJobState())
                                .build()
                ))
                .build();

        JsonNode definition = objectMapper.readTree("{\"graph\": [{\"id\":\"id\", \"vertex\":true, \"value\": {\"operation\": \"JOB\", \"name\": \"name\", \"jobId\": \"jobId\"}}]}");

        PipelineDto pipelineDto = PipelineDto.builder().definition(definition).build();
        doReturn(pipelineDto).when(pipelineService).getById(anyString(), anyString());
        when(mapperService.mapStatus(any())).thenReturn("status");

        String projectId = "projectId";
        pipelineService.updateJobStatuses(projectId, pipeline, result);

        assertEquals("Draft", pipeline.getJobsStatuses().get("id"));
        verify(pipelineService, times(1)).getById(projectId, pipelineId);
        verify(pipelineService, times(1)).patch(projectId, pipeline);
    }

}
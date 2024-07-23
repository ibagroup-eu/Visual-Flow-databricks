package eu.ibagroup.vfdatabricks.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobClusterDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobLogDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineHistoryResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private RestTemplate restTemplate;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private ApplicationConfigurationProperties appProperties;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private DatabricksJobService databricksJobService;
    @InjectMocks
    private HistoryService historyService;

    @Test
    void shouldGetPipelineHistorySuccessfully() {
        String pipelineId = "pipelineId";
        PipelineHistoryResponseDto[] pipelineHistoryResponseDtoArray = new PipelineHistoryResponseDto[1];
        when(restTemplate.getForEntity(anyString(), eq(PipelineHistoryResponseDto[].class)))
                .thenReturn(ResponseEntity.ok(pipelineHistoryResponseDtoArray));

        List<PipelineHistoryResponseDto> result = historyService.getPipelineHistory(pipelineId);

        assertEquals(Arrays.asList(pipelineHistoryResponseDtoArray), result);
    }

    @Test
    void testGetPipelineLogs() throws IOException {
        when(pipelineService.getById(eq("projectId"),
                eq("pipelineId")
        )).thenReturn(PipelineDto.builder().runId(123L).status("Pending").build());

        Path file = Path.of("", "src/test/resources").resolve("logs.json");
        String data = objectMapper.readValue(Files.readString(file), JsonNode.class).get("data").asText();
        when(databricksJobService.getClusterInfo("projectId", 123L))
                .thenReturn(DatabricksJobClusterDto.builder()
                        .tasks(List.of(DatabricksJobClusterDto.Task.builder()
                                .jobName("jobName")
                                .clusterInstance(DatabricksJobClusterDto.ClusterInstance.
                                        builder()
                                        .clusterId("clusterId")
                                        .build())
                                .build()))
                        .build());

        when(databricksJobService.getJobLogs("projectId", "clusterId"))
                .thenReturn(DatabricksJobLogDto.builder().data(data).build());


        List<JobLogDto> actual = historyService.getPipelineLogs("projectId", "pipelineId", "jobName");
        assertEquals("SparkHadoopUtil: Installing CredentialsScopeFilesystem for scheme s3. Previous value: com.databricks.common.filesystem.LokiFileSystem",
                actual.get(0).getMessage(), "Objects must be equals");
    }

}
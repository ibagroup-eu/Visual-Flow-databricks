package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportRequestDto;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportRequestDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportResponseDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewListDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    private static final String PROJECT_ID = "vf-project-name";
    public static final String CRON_SCHEDULE = "9 * * * *";
    public static final String PIPELINE_NAME = "Test";
    public static final String PIPELINE_ID = "ID";
    @Mock(answer = RETURNS_DEEP_STUBS)
    private ApplicationConfigurationProperties appProperties;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private CronCheckService checkService;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        this.transferService = new TransferService(restTemplate, appProperties, schedulerService, pipelineService,
                checkService);
    }

    @Test
    void testExporting() {
        ExportRequestDto request = new ExportRequestDto();
        ResponseEntity<ExportResponseDto> response = ResponseEntity.ok(
                new ExportResponseDto(new HashSet<>(), new HashSet<>()));

        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/%s/exportResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                any(),
                eq(ExportResponseDto.class)))
                .thenReturn(response);
        doNothing().when(checkService).checkAndUpdateCron(PROJECT_ID, response.getBody().getPipelines());
        assertEquals(response, transferService.exporting(PROJECT_ID, request),
                "Response should be the same as expected");
        verify(restTemplate).postForEntity(anyString(), any(), eq(ExportResponseDto.class));
    }

    @Test
    void testImporting() {
        ImportRequestDto request = new ImportRequestDto(List.of(PipelineDto.builder().name(PIPELINE_NAME)
                .cron(true).cronExpression(CRON_SCHEDULE).build()),
                List.of());
        ResponseEntity<ImportResponseDto> response = ResponseEntity.ok(new ImportResponseDto());

        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/%s/importResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                any(),
                eq(ImportResponseDto.class)))
                .thenReturn(response);
        when(pipelineService.getAll(PROJECT_ID, Set.of(PIPELINE_NAME))).thenReturn(PipelineOverviewListDto.builder()
                .pipelines(List.of(PipelineOverviewDto.builder().id(PIPELINE_ID).build()))
                .build());
        when(schedulerService.exists(PROJECT_ID, PIPELINE_ID)).thenReturn(CompletableFuture.completedFuture(true));
        doNothing().when(schedulerService).updateCron(PROJECT_ID, PIPELINE_ID, CronPipelineDto.builder().schedule(CRON_SCHEDULE).build());
        assertEquals(response, transferService.importing(PROJECT_ID, request),
                "Response should be the same as expected");
        verify(restTemplate).postForEntity(anyString(), any(), eq(ImportResponseDto.class));
    }
}

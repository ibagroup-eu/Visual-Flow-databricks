package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineHistoryResponseDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewListDto;
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.HistoryService;
import eu.ibagroup.vfdatabricks.services.PipelineService;
import eu.ibagroup.vfdatabricks.services.SchedulerService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineControllerTest {

    @Mock
    private PipelineService pipelineService;
    @Mock
    private HistoryService historyService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private SchedulerService schedulerService;

    @InjectMocks
    private PipelineController controller;

    @BeforeEach
    void setUp() {
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationService.getUserInfo()).thenReturn(expected);
    }

    @Test
    void shouldCreatePipelineSuccessfully() {
        PipelineDto pipelineRequestDto = new PipelineDto();
        when(pipelineService.create(anyString(), any(PipelineDto.class))).thenReturn("pipelineId");

        ResponseEntity<String> response = controller.create("projectId", pipelineRequestDto);

        assertEquals("pipelineId", response.getBody());
        verify(pipelineService).create(anyString(), any(PipelineDto.class));
    }

    @Test
    void shouldGetPipelineSuccessfully() {
        PipelineDto pipelineDto = new PipelineDto();
        when(pipelineService.getByIdAndFetchStatus(anyString(), anyString())).thenReturn(pipelineDto);

        PipelineDto response = controller.get("projectId", "pipelineId");

        assertEquals(pipelineDto, response);
        verify(pipelineService).getByIdAndFetchStatus(anyString(), anyString());
    }

    @Test
    void shouldUpdatePipelineSuccessfully() {
        PipelineDto pipelineDto = new PipelineDto();
        doNothing().when(pipelineService).update(anyString(), anyString(), any(PipelineDto.class));

        controller.update("projectId", "pipelineId", pipelineDto);

        verify(pipelineService).update(anyString(), anyString(), any(PipelineDto.class));
    }

    @Test
    void shouldDeletePipelineSuccessfully() {
        doNothing().when(pipelineService).delete(anyString(), anyString());

        ResponseEntity<Void> response = controller.delete("projectId", "pipelineId");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(pipelineService).delete(anyString(), anyString());
    }

    @Test
    void shouldGetAllPipelinesSuccessfully() {
        PipelineOverviewListDto pipelineOverviewListDto =  PipelineOverviewListDto.builder().build();
        when(pipelineService.getAll(anyString(), isNull())).thenReturn(pipelineOverviewListDto);

        PipelineOverviewListDto response = controller.getAll("projectId", null);

        assertEquals(pipelineOverviewListDto, response);
        verify(pipelineService).getAll(anyString(), isNull());
    }

    @Test
    void shouldRunPipelineSuccessfully() {
        doNothing().when(pipelineService).run(anyString(), anyString());

        controller.run("projectId", "pipelineId");

        verify(pipelineService).run(anyString(), anyString());
    }

    @Test
    void shouldTerminatePipelineSuccessfully() {
        doNothing().when(pipelineService).terminate(anyString(), anyString());

        controller.terminate("projectId", "pipelineId");

        verify(pipelineService).terminate(anyString(), anyString());
    }

    @Test
    void shouldStopPipelineSuccessfully() {
        doNothing().when(pipelineService).stop(anyString(), anyString());

        controller.stop("projectId", "pipelineId");

        verify(pipelineService).stop(anyString(), anyString());
    }

    @Test
    void shouldGetPipelineHistorySuccessfully() {
        List<PipelineHistoryResponseDto> pipelineHistoryResponseDtoList = List.of(PipelineHistoryResponseDto.builder().build());
        when(historyService.getPipelineHistory(anyString())).thenReturn(pipelineHistoryResponseDtoList);

        List<PipelineHistoryResponseDto> response = controller.getPipelineHistory("projectId", "pipelineId");

        assertEquals(pipelineHistoryResponseDtoList, response);
        verify(historyService).getPipelineHistory(anyString());
    }

    @Test
    void testGetLogs() {
        when(historyService.getPipelineLogs("projectId", "pipelineId", "jobName")).thenReturn(List.of(JobLogDto.builder().build()));

        controller.getLogs("projectId", "pipelineId", "jobName");
        verify(historyService).getPipelineLogs(anyString(), anyString(), anyString());
    }

    @Test
    void testCreateCron() {

        CronPipelineDto cronPipelineDto = CronPipelineDto.builder().build();

        controller.createCron("projectId", "id", cronPipelineDto);

        verify(authenticationService, times(2)).getUserInfo();
    }

    @Test
    void testDeleteCron() {

        ResponseEntity<Void> response = controller.deleteCron("projectId", "id");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Status must be 204");
        verify(authenticationService, times(2)).getUserInfo();
    }

    @Test
    void testGetCronPipeline() {

        controller.getCronPipeline("projectId", "id");

        verify(authenticationService).getUserInfo();
    }

    @Test
    void testUpdateCron() {
        CronPipelineDto cronPipelineDto = CronPipelineDto.builder().build();

        controller.updateCron("projectId", "id", cronPipelineDto);

        verify(authenticationService, times(2)).getUserInfo();
    }
}
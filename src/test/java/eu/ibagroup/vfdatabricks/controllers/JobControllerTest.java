package eu.ibagroup.vfdatabricks.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.dto.jobs.*;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.JobLogDto;
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.JobService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobService jobService;
    @Mock
    private AuthenticationService authenticationServiceMock;
    private JobController controller;

    @BeforeEach
    void setUp() {
        controller = new JobController(jobService, authenticationServiceMock);
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationServiceMock.getUserInfo()).thenReturn(expected);
    }

    @Test
    void testGetAll() throws InterruptedException {
        when(jobService.getAll("projectId")).thenReturn(JobOverviewListDto
                .builder()
                .jobs(List.of(JobOverviewDto
                                .builder()
                                .pipelineInstances(List.of())
                                .build(),
                        JobOverviewDto
                                .builder()
                                .pipelineInstances(List.of())
                                .build()))
                .editable(true)
                .build());

        JobOverviewListDto response = controller.getAll("projectId");
        assertEquals(2, response.getJobs().size(), "Jobs size must be 2");
        assertTrue(response.isEditable(), "Must be true");

        verify(jobService).getAll(anyString());
    }

    @Test
    void testGet() throws IOException, InterruptedException {
        JobDto dto = JobDto
                .builder()
                .lastModified("lastModified")
                .definition(new ObjectMapper().readTree("{\"graph\":[]}".getBytes()))
                .name("name")
                .build();

        when(jobService.getAndFetchStatus("projectId", "jobId")).thenReturn(dto);

        JobDto response = controller.get("projectId", "jobId");

        assertEquals(dto, response, "Response must be equal to dto");

        verify(jobService).getAndFetchStatus(anyString(), anyString());
    }

    @Test
    void testCreate() throws JsonProcessingException {
        JobDto jobRequestDto = JobDto
                .builder()
                .definition(new ObjectMapper().readTree("{\"graph\":[]}"))
                .name("newName")
                .build();
        when(jobService.create("projectId", jobRequestDto)).thenReturn("jobId");
        ResponseEntity<String> response = controller.create("projectId", jobRequestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Status must be OK");
        assertEquals("jobId", response.getBody(), "Body must be equals to jobId");

        verify(jobService).create(anyString(), any());
    }

    @Test
    void testUpdate() throws JsonProcessingException {
        JobDto jobDto = JobDto
                .builder()
                .definition(new ObjectMapper().readTree("{\"graph\":[]}"))
                .name("newName")
                .build();
        doNothing().when(jobService).update("projectId", "jobId", jobDto);

        controller.update("projectId", "jobId", jobDto);

        verify(jobService).update(anyString(), anyString(), any());
    }

    @Test
    void testDelete() {
        doNothing().when(jobService).delete("projectId", "jobId");

        ResponseEntity<Void> response = controller.delete("projectId", "jobId");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Status must be 204");

        verify(jobService).delete(anyString(), anyString());
    }

    @Test
    void testRun() {
        doNothing().when(jobService).run("projectId", "jobId");
        controller.run("projectId", "jobId");
        verify(jobService).run(anyString(), anyString());
    }

    @Test
    void testStop() {
        doNothing().when(jobService).stop("projectId", "jobId");
        controller.stop("projectId", "jobId");
        verify(jobService).stop(anyString(), anyString());
    }

    @Test
    void testGetHistory() {
        List<JobHistoryDto> dtoList = List.of(JobHistoryDto
                .builder()
                .type("job")
                .status("Succeeded")
                .startedAt("2022-08-24T09:45:09Z")
                .finishedAt("2022-08-24T09:46:19Z")
                .startedBy("jane-doe")
                .build());
        when(jobService.getJobHistory("jobId")).thenReturn(dtoList);

        List<JobHistoryDto> response = controller.getHistory("projectId", "jobId");

        assertEquals(dtoList, response, "Response must be equal to dto");

        verify(jobService).getJobHistory(anyString());
    }

    @Test
    void testGetLogs() {
        when(jobService.getJobLogs("projectId", "jobId")).thenReturn(List.of(JobLogDto.builder().build()));

        controller.getLogs("projectId", "jobId");
        verify(jobService).getJobLogs(anyString(), anyString());
    }

    @Test
    void testGetLogsHistory() {
        List<JobLogDto> dtoList = List.of(JobLogDto.builder().build());
        when(jobService.getJobLogsHistory("jobId", "logId")).thenReturn(dtoList);

        List<JobLogDto> response = controller.getLogsHistory("projectId", "jobId", "logId");

        assertEquals(dtoList, response, "Response must be equal to dto");

        verify(jobService).getJobLogsHistory(anyString(), anyString());
    }
}

package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.dto.jobs.CommonDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobRunDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobState;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static eu.ibagroup.vfdatabricks.services.UtilsService.toFormattedString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AsyncJobCheckServiceTest {
    private static final String PROJECT_ID = "vf-project-name";

    @Mock
    private DatabricksJobService databricksApiService;
    @Mock
    private MapperService mapperService;
    @InjectMocks
    private AsyncJobCheckService asyncJobCheckService;

    @Test
    void testCheckAndUpdateStatus() throws InterruptedException, ExecutionException {
        CommonDto dto = CommonDto.builder()
                .startedAt(toFormattedString(123L))
                .finishedAt(toFormattedString(123L))
                .runId(123L)
                .status("Running")
                .build();

        String status = "state2";
        DatabricksJobRunDto jobRunDto = DatabricksJobRunDto.builder()
                .startTime(Instant.parse("2024-05-22T10:14:00.000z").toEpochMilli())
                .endTime(Instant.parse("2024-05-23T10:14:00.000z").toEpochMilli())
                .state(DatabricksJobState.builder().resultState(status).build())
                .build();
        when(databricksApiService.checkJobStatus(anyString(), anyLong()))
                .thenReturn(jobRunDto);
        when(mapperService.mapStatus(any())).thenReturn(status);

        CompletableFuture<DatabricksJobRunDto> actual = asyncJobCheckService.checkAndUpdateStatus(PROJECT_ID, dto);
        assertEquals(jobRunDto, actual.get(), "Objects must be equal");
        assertThat(dto, allOf(
                hasProperty("status", is(status)),
                hasProperty("startedAt", Matchers.startsWith("2024-05-22")),
                hasProperty("finishedAt", Matchers.startsWith("2024-05-23"))

        ));
    }
}

package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import eu.ibagroup.vfdatabricks.dto.pipelines.PipelineOverviewDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CronCheckServiceTest {

    @Mock
    private SchedulerService schedulerService;

    @InjectMocks
    private CronCheckService cronCheckService;


    @Test
    public void testUpdateCronDetailsExists() {
        String projectId = "project1";
        PipelineOverviewDto pipelineOverviewDto = new PipelineOverviewDto();
        pipelineOverviewDto.setId("pipeline1");
        CronPipelineDto cronPipelineDto = new CronPipelineDto();
        cronPipelineDto.setSuspend(true);

        when(schedulerService.getCron(projectId, "pipeline1")).thenReturn(cronPipelineDto);
        when(schedulerService.exists(projectId, "pipeline1")).thenReturn(CompletableFuture.completedFuture(true));
        cronCheckService.checkAndUpdateCron(projectId, Collections.singleton(pipelineOverviewDto));

        assertTrue(pipelineOverviewDto.isCron());
        assertTrue(pipelineOverviewDto.isCronSuspend());
    }

}
package eu.ibagroup.vfdatabricks.services.quartz;

import eu.ibagroup.vfdatabricks.services.PipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CronJobTest {

    @Mock
    private PipelineService pipelineService;
    @Mock
    private JobExecutionContext jobExecutionContext;
    @Mock
    private JobDetail jobDetail;
    @Mock
    private JobDataMap jobDataMap;

    @InjectMocks
    private CronJob cronJob;

    @BeforeEach
    void setUp() {
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    }

    @Test
    @DisplayName("Execute job with valid project and pipeline IDs")
    void testExecuteJobShouldRunPipeline() {
        String projectId = "projectId";
        when(jobDataMap.getString("projectId")).thenReturn(projectId);
        String pipelineId = "pipelineId";
        when(jobDataMap.getString("pipelineId")).thenReturn(pipelineId);

        cronJob.executeInternal(jobExecutionContext);

        verify(pipelineService, times(1)).run(projectId, pipelineId);
    }
}
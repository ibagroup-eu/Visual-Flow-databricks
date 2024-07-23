package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.dto.pipelines.CronPipelineDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private SchedulerFactoryBean schedulerFactoryBean;
    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private SchedulerService schedulerService;

    @BeforeEach
    void setUp() throws SchedulerException {
        when(schedulerFactoryBean.getScheduler()).thenReturn(scheduler);
    }

    @Test
    void testCreateCronShouldScheduleJob() throws SchedulerException {
        schedulerService.createCron("project1", "pipeline1",  CronPipelineDto.builder().schedule("0/5 * * * *").build());
        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    void testDeleteCronShouldCallDeleteJob() throws SchedulerException {
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);
        assertDoesNotThrow(() -> schedulerService.deleteCron("project1", "pipeline1"));
    }

    @Test
    void testExistsShouldReturnTrue() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        assertTrue(schedulerService.exists("project1", "pipeline1").join());
    }

    @Test
    void testUpdateCronShouldRescheduleJob() throws SchedulerException {
        CronTrigger existingTrigger = mock(CronTrigger.class);
        doReturn("0/5 * * * * ?").when(existingTrigger).getCronExpression();
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existingTrigger);

        CronPipelineDto cronPipelineDto = CronPipelineDto.builder().schedule("0/10 * * * *").build();
        assertDoesNotThrow(() -> schedulerService.updateCron("project1", "pipeline1", cronPipelineDto));

        verify(scheduler).rescheduleJob(any(), any());
    }

    @Test
    void testUpdateCronShouldShouldPauseTriggerWhenSuspended() throws SchedulerException {
        doNothing().when(scheduler).pauseTrigger(any(TriggerKey.class));
        CronTrigger existingTrigger = mock(CronTrigger.class);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existingTrigger);

        CronPipelineDto cronPipelineDto = CronPipelineDto.builder().suspend(true).build();
        schedulerService.updateCron("project1", "pipeline1", cronPipelineDto);

        verify(scheduler).pauseTrigger(any(TriggerKey.class));
    }

    @Test
    void testGetCronShouldReturnCronPipelineDto() throws SchedulerException {
        CronTrigger cronTrigger = mock(CronTrigger.class);
        doReturn("0 0/5 * * * ?").when(cronTrigger).getCronExpression();
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(cronTrigger);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.PAUSED);

        CronPipelineDto actual = schedulerService.getCron("project1", "pipeline1");
        CronPipelineDto expected = CronPipelineDto.builder().schedule("0/5 * * * *").suspend(true).build();
        assertEquals(expected, actual);
    }

}

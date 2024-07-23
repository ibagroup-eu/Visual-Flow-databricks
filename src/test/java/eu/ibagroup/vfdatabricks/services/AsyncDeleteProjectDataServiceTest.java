package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.jobs.JobOverviewDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobOverviewListDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
public class AsyncDeleteProjectDataServiceTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private RestTemplate restTemplate;
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    private AsyncDeleteProjectDataService asyncDeleteProjectDataService;
    @Mock
    private HashOperations hashOperations;

    @BeforeEach
    void setUp() {
        this.asyncDeleteProjectDataService = new AsyncDeleteProjectDataService(redisTemplate, restTemplate, appProperties);
    }

    @Test
    void testDeleteProjectData() {
        when(restTemplate.getForEntity(
                        eq(String.format("%s/%s/%s/%s/job",
                                appProperties.getJobStorage().getHost(),
                                CONTEXT_PATH,
                                JOB_STORAGE_API,
                                "projectId")),
                        eq(JobOverviewListDto.class)))
                .thenReturn(ResponseEntity.ok(JobOverviewListDto.builder().jobs(List.of(JobOverviewDto.builder().status("Pending").build())).build()));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(any())).thenReturn(Map.of("123", "json"));
        when(hashOperations.delete(any(), any())).thenReturn(1L);
        asyncDeleteProjectDataService.deleteProjectData("projectId");
        verify(hashOperations).delete(any(), any());
    }
}

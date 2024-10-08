package eu.ibagroup.vfdatabricks.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterValue;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static eu.ibagroup.vfdatabricks.dto.Constants.DESCRIPTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParameterServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations hashOperations;

    @Mock
    private DatabricksAPIService databricksAPIService;

    private ParameterService parameterService;

    private static final String PROJECT_ID = "vf-project-name";


    @BeforeEach
    void setUp() {
        parameterService = new ParameterService(redisTemplate, new ObjectMapper(), databricksAPIService);
    }

    @Test
    void testCreate() throws JsonProcessingException {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        doNothing().when(hashOperations).put(anyString(), anyString(), any());
        parameterService.create("vf-project-name", "key", ParameterDto.builder()
                                                                     .key("key")
                                                                     .secret(false)
                                                                     .value(ParameterValue.builder().text("123").build())
                                                                     .build());
        verify(hashOperations).put(eq("projectParams:vf-project-name"), anyString(), any());
    }

    @Test
    void testUpdate() throws JsonProcessingException {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(any(), any())).thenReturn(1L);
        doNothing().when(hashOperations).put(anyString(), anyString(), any());
        parameterService.update("vf-project-name", "key", ParameterDto.builder()
                .key("key")
                .secret(false)
                .value(ParameterValue.builder().text("123").build())
                .build());
        verify(hashOperations).put(eq("projectParams:vf-project-name"), anyString(), any());
    }

    @Test
    void testGet() throws IOException {
        Path file = Path.of("", "src/test/resources").resolve("params.json");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        String jsonParameter = Files.readString(file);
        when(hashOperations.get(any(), any())).thenReturn(jsonParameter);
        parameterService.get("vf-project-name", "key");
        verify(hashOperations).get(any(), any());
    }

    @Test
    void testGetAll() throws IOException {
        Path file = Path.of("", "src/test/resources").resolve("params.json");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        String jsonParameter = Files.readString(file);
        when(hashOperations.entries(any())).thenReturn(Map.of("projectParams:vf-project-name:params:param1", jsonParameter, "projectParams:vf-project-name:params:param2", jsonParameter));
        parameterService.getAll("projectId");
        verify(hashOperations).entries(any());
    }

    @Test
    void testDelete() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(any(), any())).thenReturn(1L);
        parameterService.delete(PROJECT_ID, "key");
        verify(hashOperations).delete(any(), any());
    }

}

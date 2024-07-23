package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionDto;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionOverviewDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
class ConnectionServiceTest {
    private static final String PROJECT_ID = "vf-project-name";
    private static final String CONNECTION_ID = "test-connection";
    @Mock
    private RestTemplate restTemplate;
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    private ConnectionService connectionService;


    @BeforeEach
    void setUp() {
        this.connectionService = new ConnectionService(appProperties, restTemplate);
    }

    @Test
    void testGetAll() throws InterruptedException {
        ConnectionOverviewDto expected = ConnectionOverviewDto.builder().editable(true)
                .connections(List.of(ConnectionDto.builder().key("key1").value(Map.of("key", "value")).build())).build();

        when(restTemplate.getForEntity(
                eq(String.format("%s/%s/%s/%s/connections",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                eq(ConnectionOverviewDto.class)))
                .thenReturn(ResponseEntity.ok(expected));
        assertEquals(expected, connectionService.getAll(PROJECT_ID), "Objects must be equal");
        verify(restTemplate).getForEntity(anyString(), eq(ConnectionOverviewDto.class));
    }


    @Test
    void testCreate() {
        ConnectionDto connectionDto = ConnectionDto.builder().key("key1").value(Map.of("key", "value")).build();

        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/%s/connection",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                any(),
                eq(String.class))).thenReturn(ResponseEntity.ok(CONNECTION_ID));

        assertEquals(CONNECTION_ID, connectionService.create(PROJECT_ID, connectionDto), "Objects must be equal");
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testUpdate() {
        ConnectionDto connectionDto = ConnectionDto.builder().key("key1").value(Map.of("key", "value")).build();

        connectionService.update(PROJECT_ID, connectionDto);
        verify(restTemplate).put(
                anyString(),
                any(),
                eq(Void.class)
        );
    }

    @Test
    void testDelete() {
        connectionService.delete(PROJECT_ID, CONNECTION_ID);

        verify(restTemplate).delete(anyString(), eq(Object.class));
    }

}


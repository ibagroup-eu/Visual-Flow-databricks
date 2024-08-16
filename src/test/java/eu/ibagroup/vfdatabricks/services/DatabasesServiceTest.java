package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.connections.ConnectionDto;
import eu.ibagroup.vfdatabricks.dto.databases.PingStatusDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterOverviewDto;
import eu.ibagroup.vfdatabricks.dto.parameters.ParameterValue;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
public class DatabasesServiceTest {

    @Mock
    private ParameterService parameterService;
    @Mock
    private ConnectionService connectionService;
    @Mock
    private RestTemplate restTemplate;
    private DatabasesService databasesService;
    @Autowired
    private ApplicationConfigurationProperties appProperties;

    @BeforeEach
    public void setUp() {
        databasesService = new DatabasesService(connectionService, parameterService,
                restTemplate, appProperties);
    }

    @SneakyThrows
    @Test
    public void testGetConnection() {
        String projectId = "test";
        String connectionName = "con";
        Map<String, String> connectionVals = Map.of("db", "#db#");
        ParameterDto paramDto = ParameterDto.builder().key("db").value(ParameterValue.builder()
                .text("value")
                .build()).secret(false).build();
        ParameterOverviewDto mockParam = mock(ParameterOverviewDto.class);
        when(parameterService.getAll(projectId)).thenReturn(mockParam);
        when(mockParam.getParams()).thenReturn(List.of(paramDto));
        ConnectionDto connectDto = ConnectionDto.builder().key("db2")
                .value(connectionVals).build();
        when(connectionService.get(projectId, connectionName)).thenReturn(connectDto);
        ConnectionDto result = databasesService.getConnection(projectId, connectionName);
        assertEquals(connectDto.getValue(), result.getValue(),
                "The expected connection should be equal to actual one!");
    }

    @Test
    void testPing() {
        DatabasesService spy = spy(databasesService);
        String projectId = "project";
        String connectionName = "con";
        ConnectionDto connectionDto = new ConnectionDto();
        PingStatusDto status = PingStatusDto.builder().status(true).build();
        doReturn(connectionDto).when(spy).getConnection(projectId, connectionName);
        when(restTemplate.postForEntity(appProperties.getDbService().getHost(), connectionDto, PingStatusDto.class)).
                thenReturn(new ResponseEntity<>(status, HttpStatus.OK));
        PingStatusDto actual = spy.ping(projectId, connectionName);
        assertEquals(actual, status, "Ping() should return true as ping status!");
        verify(spy).getConnection(projectId, connectionName);
        verify(restTemplate).postForEntity(appProperties.getDbService().getHost(), connectionDto, PingStatusDto.class);
    }

    @Test
    void testPingWithParams() {
        DatabasesService spy = spy(databasesService);
        String projectId = "project";
        PingStatusDto status = PingStatusDto.builder().status(true).build();
        ConnectionDto connectionDto = new ConnectionDto();
        doReturn(connectionDto).when(spy).replaceParams(projectId, connectionDto);
        when(restTemplate.postForEntity(appProperties.getDbService().getHost(), connectionDto, PingStatusDto.class)).
                thenReturn(new ResponseEntity<>(status, HttpStatus.OK));
        PingStatusDto actual = spy.ping(projectId, connectionDto);
        assertEquals(actual, status, "Ping() should return true as a ping status!");
        verify(restTemplate).postForEntity(appProperties.getDbService().getHost(), connectionDto, PingStatusDto.class);
    }
}

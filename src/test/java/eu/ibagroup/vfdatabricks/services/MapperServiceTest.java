package eu.ibagroup.vfdatabricks.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.GraphDto;
import eu.ibagroup.vfdatabricks.dto.jobs.HistoryResponseDto;
import eu.ibagroup.vfdatabricks.dto.jobs.JobDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.*;
import eu.ibagroup.vfdatabricks.dto.notifications.EmailNotification;
import eu.ibagroup.vfdatabricks.dto.jobs.JobParams;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.apache.hc.client5.http.utils.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
class MapperServiceTest {

    private static final String PROJECT_NAME = "project name";
    private static final String PROJECT_ID = "projectId";
    private static final String HOST_VALUE = "aG9zdA==";
    private static final String TOKEN_VALUE = "dG9rZW4=";
    private static final String PATH_TO_FILE_VALUE = "L1ZvbHVtZXMvc2FsZXMvZGltcy9pbmdlc3Rpb25fem9uZQ==";
    private static final String CLOUD_VALUE = "Y2xvdWQ=";

    private static final String INPUT_GRAPH = """
            {
              "graph": [
                {
                   "id": "-jRjFu5yR",
                   "vertex": true,
                  "value": {
                    "label": "Read",
                    "text": "stage",
                    "desc": "description",
                    "type": "read"
                  }
                },
                {
                   "id": "cyVyU8Xfw",
                   "vertex": true,
                  "value": {
                    "label": "Write",
                    "text": "stage",
                    "desc": "description",
                    "type": "write"
                  }
                },
                {
                  "value": {},
                  "id": "4",
                  "edge": true,
                  "parent": "1",
                  "source": "-jRjFu5yR",
                  "target": "cyVyU8Xfw",
                  "successPath": true,
                  "mxObjectId": "mxCell#8"
                }
              ]
            }""";
    @Spy
    private final ObjectMapper MAPPER = new ObjectMapper();
    @Mock
    private KubernetesService kubernetesService;
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    private MapperService mapperService;

    @BeforeEach
    void setUp() {
        mapperService = new MapperService(kubernetesService, MAPPER);
    }

    @Test
    void testMapJobRunsToHistory() {
        DatabricksJobRunListDto jobRunListDto = DatabricksJobRunListDto.builder()
                .runs(List.of(DatabricksJobRunDto.builder()
                        .jobId(123)
                        .creatorUserName("creatorName")
                        .startTime(45000L)
                        .endTime(80000L)
                        .state(DatabricksJobState.builder().lifeCycleState(RUNNING_DB_STATUS).build())
                        .runId(1234)
                        .build()))
                .build();
        List<HistoryResponseDto> expected = List.of(HistoryResponseDto.builder()
                .id("123")
                .type(JOB_TYPE)
                .startedBy("creatorName")
                .startedAt(DATE_TIME_FORMATTER.format(
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(45000L), ZoneId.systemDefault())))
                .finishedAt(DATE_TIME_FORMATTER.format(
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(80000L), ZoneId.systemDefault())))
                .status(RUNNING_VF_STATUS).logId("1234")
                .build());

        assertEquals(expected, mapperService.mapJobRunsToHistory(jobRunListDto), "Objects must be equal");
    }

    @Test
    void testMapStatus() {
        DatabricksJobState databricksJobState = DatabricksJobState.builder().lifeCycleState(PENDING_DB_STATUS).resultState(SUCCESS_DB_STATUS).build();
        String result = mapperService.mapStatus(databricksJobState);
        assertEquals(PENDING_VF_STATUS, result, "Status must be equal");
        databricksJobState.setLifeCycleState(RUNNING_DB_STATUS);
        result = mapperService.mapStatus(databricksJobState);
        assertEquals(RUNNING_VF_STATUS, result, "Status must be equal");
        databricksJobState.setLifeCycleState("smth");
        result = mapperService.mapStatus(databricksJobState);
        assertEquals(SUCCEEDED_VF_STATUS, result, "Status must be equal");
        databricksJobState.setResultState("smth");
        result = mapperService.mapStatus(databricksJobState);
        assertEquals(FAILED_VF_STATUS, result, "Status must be equal");


    }

    @Test
    void testMapRequestToJobRun() throws JsonProcessingException {
        Secret secret = new SecretBuilder()
                .addToData(HOST, HOST_VALUE)
                .addToData(TOKEN, TOKEN_VALUE)
                .addToData(PATH_TO_FILE, PATH_TO_FILE_VALUE)
                .addToData(CLOUD, CLOUD_VALUE)
                .editOrNewMetadata()
                .withName(PROJECT_ID)
                .addToAnnotations(NAME, PROJECT_NAME)
                .addToAnnotations(DESCRIPTION, DESCRIPTION)
                .endMetadata()
                .build();
        when(kubernetesService.getSecret(PROJECT_ID)).thenReturn(secret);
        DatabricksJobStorageRunDto expected = DatabricksJobStorageRunDto.builder()
                .runName("name")
                .tasks(List.of(
                        DatabricksJobTask.builder()
                                .taskKey("name")
                                .newCluster(
                                        DatabricksJobNewCluster.builder()
                                                .sparkEnvVars(Map.of(JNAME, "zulu11-ca-amd64",
                                                        VISUAL_FLOW_CONFIGURATION_TYPE, "Databricks",
                                                        VISUAL_FLOW_DATABRICKS_SECRET_SCOPE, PROJECT_ID,
                                                        JOB_CONFIG_FIELD, Base64.encodeBase64String(GraphDto
                                                                .parseGraph(MAPPER.readTree(INPUT_GRAPH))
                                                                .toString().
                                                                getBytes(StandardCharsets.UTF_8)),
                                                        JOB_DEFINITION_FIELD, Base64.encodeBase64String(
                                                                MAPPER.readTree(INPUT_GRAPH)
                                                                        .toString()
                                                                        .getBytes(StandardCharsets.UTF_8)),
                                                        UP_TO, "600",
                                                        INTERVALS, "30"))
                                                .clusterLogConfig(DatabricksJobNewCluster.ClusterLogConfig.builder()
                                                        .dbfs(DatabricksJobNewCluster.ClusterLogConfig.DBFS.builder()
                                                                .destination("dbfs:/logStore/log")
                                                                .build())
                                                        .build())
                                                .build()
                                )
                                .sparkJarTask(
                                        DatabricksJobSparkJarTask.builder()
                                                .mainClassName("by.iba.vf.spark.transformation.TransformationJob")
                                                .build()
                                )
                                .libraries(List.of(DatabricksJobTask.Library.builder().jar("/Volumes/sales/dims/ingestion_zone/spark-transformations-0.1-jar-with-dependencies.jar").build()))
                                .build()
                ))
                .build();
        JobDto jobDto = JobDto.builder()
                .name("name")
                .definition(MAPPER.readTree(INPUT_GRAPH))
                .params(JobParams.builder()
                        .driverCores("1")
                        .driverMemory("r")
                        .executorMemory("m")
                        .upTo("600")
                        .intervals("30")
                        .clusterDatabricksSchema(Map.of())
                        .build())
                .build();
        DatabricksJobStorageRunDto actual = mapperService.mapRequestToJobRun(jobDto, PROJECT_ID);
        assertEquals(expected, actual, "Objects must be equal");
    }

    @Test
    void shouldMapEmailNotificationsWhenEmailNotificationIsNull() {
        EmailNotification emailNotification = null;

        DatabricksEmailNotifications result = mapperService.mapEmailNotifications(emailNotification);

        assertNull(result);
    }

    @Test
    void shouldMapEmailNotificationsWhenFailureNotifyIsTrue() {
        EmailNotification emailNotification = new EmailNotification();
        emailNotification.setFailureNotify(true);
        emailNotification.setRecipients(List.of("recipient1", "recipient2"));

        DatabricksEmailNotifications result = mapperService.mapEmailNotifications(emailNotification);

        assertEquals(emailNotification.getRecipients(), result.getOnSuccess());
        assertEquals(emailNotification.getRecipients(), result.getOnFailure());
    }

    @Test
    void shouldMapEmailNotificationsWhenFailureNotifyIsFalse() {
        EmailNotification emailNotification = new EmailNotification();
        emailNotification.setFailureNotify(false);
        emailNotification.setRecipients(List.of("recipient1", "recipient2"));

        DatabricksEmailNotifications result = mapperService.mapEmailNotifications(emailNotification);

        assertNull(result.getOnSuccess());
        assertNull(result.getOnFailure());
    }

    @Test
    void toAlphaNumericShouldReplaceWithUnderscore() {
        String actual = MapperService.toAlphaNumeric("task-name ~!@$%^&*()+=`:;'\"\\|/?,.|[]{}<>");
        assertEquals("task_name_", actual);
    }

    private static Stream<Arguments> statusProvider() {
        return Stream.of(
                Arguments.of(List.of(SUCCEEDED_VF_STATUS, RUNNING_VF_STATUS), RUNNING_VF_STATUS),
                Arguments.of(List.of(PENDING_VF_STATUS,SUCCEEDED_VF_STATUS), PENDING_VF_STATUS),
                Arguments.of(List.of(FAILED_VF_STATUS, SUCCEEDED_VF_STATUS), FAILED_VF_STATUS),
                Arguments.of(List.of(SUCCEEDED_VF_STATUS), SUCCEEDED_VF_STATUS),
                Arguments.of(List.of("UNKNOWN_STATUS"), DRAFT_VF_STATUS)
        );
    }

    @ParameterizedTest
    @MethodSource("statusProvider")
    void resolveOverallStatusTest(Collection<String> inputStatuses, String expectedStatus) {
        assertEquals(expectedStatus, MapperService.resolveOverallStatus(inputStatuses));
    }

    @Test
    void shouldPrepareSparkEnvSuccessfully() throws JsonProcessingException {
        JobDto jobDto = JobDto.builder()
                .definition(MAPPER.readTree("{\"graph\": [{\"value\": {\"key\": \"#value#\"}, \"vertex\": true}]}"))
                .params(JobParams.builder().upTo("upTo").intervals("intervals").build())
                .build();

        Map<String, String> result = mapperService.prepareSparkEnv(jobDto, PROJECT_ID, Map.of());

        assertThat(result, allOf(
                hasEntry("VISUAL_FLOW_DATABRICKS_SECRET_SCOPE", PROJECT_ID),
                hasEntry("JNAME","zulu11-ca-amd64"),
                hasEntry("INTERVALS","intervals"),
                hasEntry("JOB_CONFIG","eyJub2RlcyI6W3siaWQiOm51bGwsInZhbHVlIjp7ImtleSI6IiN2YWx1ZSMifSwiZWRnZXMiOm51bGx9XSwiZWRnZXMiOltdfQ=="),
                hasEntry("JOB_DEFINITION","eyJncmFwaCI6W3sidmFsdWUiOnsia2V5IjoiI3ZhbHVlIyJ9LCJ2ZXJ0ZXgiOnRydWV9XX0="),
                hasEntry("UP_TO","upTo"),
                hasEntry("VISUAL_FLOW_CONFIGURATION_TYPE", "Databricks")
        ));
    }
}

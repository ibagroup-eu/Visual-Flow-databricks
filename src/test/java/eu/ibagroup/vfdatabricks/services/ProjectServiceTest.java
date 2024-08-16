package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DataBricksSecretScopeDto;
import eu.ibagroup.vfdatabricks.dto.projects.*;
import io.fabric8.kubernetes.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static eu.ibagroup.vfdatabricks.dto.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ApplicationConfigurationProperties.class)
public class ProjectServiceTest {
    private static final String PROJECT_NAME = "project name";
    private static final String PROJECT_ID = "vf-project-name";
    private static final String HOST_VALUE = "aG9zdA==";
    private static final String TOKEN_VALUE = "dG9rZW4=";
    private static final String AUTH_VALUE = "UEFU";
    private static final String PATH_TO_FILE_VALUE = "cGF0aFRvRmlsZQ==";
    private static final String CLOUD_VALUE = "Y2xvdWQ=";
    @Mock
    private KubernetesService kubernetesService;
    @Mock
    private AsyncUploadJarService asyncUploadJarService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    AsyncDeleteProjectDataService asyncDeleteProjectDataService;
    @Mock
    private DatabricksAPIService databricksAPIService;
    @Autowired
    private ApplicationConfigurationProperties appProperties;
    private ProjectService projectService;
    private final Secret secret = new SecretBuilder()
            .addToData(HOST, HOST_VALUE)
            .addToData(TOKEN, TOKEN_VALUE)
            .addToData(AUTHENTICATION_TYPE, AUTH_VALUE)
            .addToData(PATH_TO_FILE, PATH_TO_FILE_VALUE)
            .addToData(CLOUD, CLOUD_VALUE)
            .editOrNewMetadata()
            .withName(PROJECT_ID)
            .addToAnnotations(NAME, PROJECT_NAME)
            .addToAnnotations(DESCRIPTION, DESCRIPTION)
            .endMetadata()
            .addToData(HASH, "OGVlY2MyOTRkM2VjZGZiYWFiYTc0NjQ3Y2RhMjgxM2Y=")
            .addToData(UPDATING, "dHJ1ZQ==")
            .build();

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(appProperties, kubernetesService, asyncDeleteProjectDataService, asyncUploadJarService, databricksAPIService);
    }

    @Test
    void testCreate() throws IOException {
        byte[] fileBytes = "8eecc294d3ecdfbaaba74647cda2813f".getBytes();
        try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.readAllBytes(Path.of(anyString()))).thenReturn(fileBytes);
            ProjectRequestDto projectDto =
                    ProjectRequestDto.builder().jarHash("temp").isUpdating("true").name(PROJECT_NAME).description(DESCRIPTION).host(HOST_VALUE)
                            .authentication(DatabricksAuthentication.builder().authenticationType(DatabricksAuthentication.AuthenticationType.PAT).token(TOKEN_VALUE).build()).pathToFile(PATH_TO_FILE_VALUE).cloud(CLOUD_VALUE).build();
            when(asyncUploadJarService.uploadJarFileToDatabricks(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
            projectService.create(projectDto);

            verify(kubernetesService).createSecret(eq("vf-dev-test-project-name"), any());
        }
    }

    @Test
    void testCreateException() throws IOException {
        byte[] fileBytes = "8eecc294d3ecdfbaaba74647cda2813f".getBytes();
        try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.readAllBytes(Path.of(anyString()))).thenReturn(fileBytes);
            ProjectRequestDto projectDto =
                    ProjectRequestDto.builder().jarHash("temp").isUpdating("true").name(PROJECT_NAME).description(DESCRIPTION).host(HOST_VALUE)
                            .authentication(DatabricksAuthentication.builder().authenticationType(DatabricksAuthentication.AuthenticationType.PAT).token(TOKEN_VALUE).build()).pathToFile(PATH_TO_FILE_VALUE).cloud(CLOUD_VALUE).build();
            when(asyncUploadJarService.uploadJarFileToDatabricks(anyString(), anyString())).thenReturn(CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("Something went wrong");
            }));
            projectService.create(projectDto);

            verify(kubernetesService).createSecret(eq("vf-dev-test-project-name"), any());
        }
    }

    @Test
    void testGet() {
        when(kubernetesService.getSecret(PROJECT_ID)).thenReturn(secret);

        ProjectResponseDto result = projectService.get(PROJECT_ID);

        assertEquals(ProjectResponseDto.builder()
                .id(PROJECT_ID)
                .host(HOST)
                .authentication(DatabricksAuthentication.builder().authenticationType(DatabricksAuthentication.AuthenticationType.PAT).token(TOKEN).build())
                .description(DESCRIPTION)
                .name(PROJECT_NAME)
                .pathToFile(PATH_TO_FILE)
                .cloud(CLOUD)
                .editable(true)
                .locked(false)
                .demo(false)
                .isUpdating("true")
                .build(), result, "Project must be equals to expected");
        verify(kubernetesService).getSecret(PROJECT_ID);
    }

    @Test
    void testGetAll() {
        when(kubernetesService.getSecretsByLabels(Map.of(TYPE, PROJECT))).thenReturn(List.of(secret));

        ProjectOverviewListDto result = projectService.getAll();

        ProjectOverviewListDto projectOverviewDto = ProjectOverviewListDto
                .builder()
                .projects(List.of(ProjectOverviewDto.builder()
                        .id(PROJECT_ID)
                        .name(PROJECT_NAME)
                        .description(DESCRIPTION)
                        .pathToFile(PATH_TO_FILE)
                        .cloud(CLOUD)
                        .jarHash("8eecc294d3ecdfbaaba74647cda2813f")
                        .isUpdating("true")
                        .authentication(DatabricksAuthentication.builder().authenticationType(DatabricksAuthentication.AuthenticationType.PAT).token(TOKEN).build())
                        .isLocked(false)
                        .build()))
                .editable(true)
                .build();

        assertEquals(projectOverviewDto.getProjects().get(0).getJarHash(), result.getProjects().get(0).getJarHash(), "Project must be equals to expected");
        verify(kubernetesService).getSecretsByLabels(any());
    }

    @Test
    void testUpdate() throws IOException {
        byte[] fileBytes = "8eecc294d3ecdfbaaba74647cda2813f".getBytes();
        try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.readAllBytes(Path.of(anyString()))).thenReturn(fileBytes);
            projectService.update(PROJECT_ID,
                    ProjectRequestDto.builder()
                            .description(DESCRIPTION)
                            .name(PROJECT_NAME)
                            .host(HOST)
                            .authentication(DatabricksAuthentication.builder().authenticationType(DatabricksAuthentication.AuthenticationType.PAT).token(TOKEN_VALUE).build())
                            .pathToFile(PATH_TO_FILE)
                            .cloud(CLOUD)
                            .isUpdating("true")
                            .build());

            verify(kubernetesService).updateSecret(eq(PROJECT_ID), any());
        }
    }

    @Test
    void testDelete() {
        projectService.delete(PROJECT_ID);
        verify(kubernetesService).deleteSecret(PROJECT_ID);
    }
}

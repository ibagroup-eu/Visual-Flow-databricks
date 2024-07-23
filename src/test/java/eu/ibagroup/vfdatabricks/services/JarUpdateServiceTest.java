package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JarUpdateServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ApplicationConfigurationProperties appProperties;

    @Mock
    private AsyncUploadJarService asyncUploadJarService;

    @InjectMocks
    private JarUpdateService jarUpdateService;

    @Test
    void updateJar_fileNotFound() throws IOException {
        when(appProperties.getJarHash()).thenReturn("non_existing_file_path");

        jarUpdateService.updateJar();

        verify(projectService, never()).getAll();
    }

    @Test
    void updateJar_fileExists() throws IOException {
        String jarHashFromFileSystem = "123456";
        Path path = Paths.get("existing_file_path");
        Files.writeString(path, jarHashFromFileSystem);

        when(appProperties.getJarHash()).thenReturn(path.toString());

        ProjectOverviewListDto projectOverviewListDto = new ProjectOverviewListDto();
        ProjectOverviewDto project = new ProjectOverviewDto();
        project.setId("1");
        project.setHost("localhost");
        project.setPathToFile("/path/to/file");
        projectOverviewListDto.setProjects(List.of(project));

        when(projectService.getAll()).thenReturn(projectOverviewListDto);
        when(asyncUploadJarService.uploadJarFileToDatabricks(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        jarUpdateService.updateJar();

        verify(projectService, times(1)).getAll();
        verify(asyncUploadJarService).uploadJarFileToDatabricks(anyString(), anyString());
    }

    @Test
    void updateJar_projectsNeedUpdate() throws IOException {
        String jarHashFromFileSystem = "123456";
        Path path = Paths.get("existing_file_path");
        Files.writeString(path, jarHashFromFileSystem);

        when(appProperties.getJarHash()).thenReturn(path.toString());

        ProjectOverviewListDto projectOverviewListDto = new ProjectOverviewListDto();
        ProjectOverviewDto project1 = new ProjectOverviewDto();
        project1.setId("1");
        project1.setHost("localhost");
        project1.setPathToFile("/path/to/file");
        project1.setJarHash("654321");

        ProjectOverviewDto project2 = new ProjectOverviewDto();
        project2.setId("2");
        project2.setHost("localhost");
        project2.setPathToFile("/path/to/another_file");

        projectOverviewListDto.setProjects(List.of(project1, project2));

        when(projectService.getAll()).thenReturn(projectOverviewListDto);
        when(asyncUploadJarService.uploadJarFileToDatabricks(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        jarUpdateService.updateJar();

        verify(projectService, times(4)).update(anyString(), any(ProjectRequestDto.class));
        verify(asyncUploadJarService, times(2)).uploadJarFileToDatabricks(anyString(), anyString());
    }
}

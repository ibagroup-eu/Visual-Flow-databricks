package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectOverviewListDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectRequestDto;
import eu.ibagroup.vfdatabricks.dto.projects.ProjectResponseDto;
import eu.ibagroup.vfdatabricks.model.auth.UserInfo;
import eu.ibagroup.vfdatabricks.services.ProjectService;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;
    @Mock
    private AuthenticationService authenticationService;

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectController(projectService, authenticationService);
        UserInfo expected = new UserInfo();
        expected.setName("name");
        expected.setId("id");
        expected.setUsername("username");
        expected.setEmail("email");
        when(authenticationService.getUserInfo()).thenReturn(expected);
    }

    @Test
    void testGetAll() {
        ProjectOverviewListDto expected = ProjectOverviewListDto.builder().projects(List.of(
                ProjectOverviewDto.builder().name("name 1").build(),
                ProjectOverviewDto.builder().name("name 2").build())).editable(true).build();
        when(projectService.getAll()).thenReturn(expected);
        ProjectOverviewListDto actual = controller.getAll();
        assertEquals(expected, actual, "Project list must be equals to expected");
        verify(projectService).getAll();
    }

    @Test
    void testUpdate() throws IOException {
        ProjectRequestDto projectDto = ProjectRequestDto.builder().build();
        controller.update("test", projectDto);
        verify(projectService).update("test", projectDto);
    }

    @Test
    void testDelete() {
        String name = "name";
        doNothing().when(projectService).delete(name);
        assertEquals(ResponseEntity.status(HttpStatus.NO_CONTENT).build(),
                controller.delete(name),
                "Status must be 204");

        verify(projectService).delete(name);
    }

    @Test
    void testCreate() throws IOException {
        String name = "name";
        String description = "description";

        ProjectRequestDto projectDto = ProjectRequestDto.builder().name(name).description(description).build();

        when(projectService.create(projectDto)).thenReturn(name);

        ResponseEntity<String> result = controller.create(projectDto);

        assertEquals(HttpStatus.CREATED, result.getStatusCode(), "Status must be OK");
        assertEquals(name, result.getBody(), "Body must be equals to name");

        verify(projectService).create(projectDto);
    }

    @Test
    void testGetById() {
        String name = "name";
        String description = "description";
        Namespace namespace = new Namespace();
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(name);
        objectMeta.setAnnotations(Map.of(description, description));
        namespace.setMetadata(objectMeta);
        ProjectResponseDto expected = ProjectResponseDto
                .builder()
                .name(name)
                .description(description)
                .build();
        when(projectService.get(name)).thenReturn(expected);

        ProjectResponseDto response = controller.get(name);

        assertEquals(expected, response, "Response must be equals to expected");
        verify(projectService).get(name);
    }

}


package eu.ibagroup.vfdatabricks;

import eu.ibagroup.vfdatabricks.controllers.JobController;
import eu.ibagroup.vfdatabricks.controllers.ProjectController;
import eu.ibagroup.vfdatabricks.services.*;
import eu.ibagroup.vfdatabricks.services.auth.AuthenticationService;
import eu.ibagroup.vfdatabricks.services.auth.OAuthService;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class VfDatabricksApplicationTests {

    @Autowired
    private JarUpdateService jarUpdateService;
    @Autowired
    private StartupListener startupListener;
    @Autowired
    private JobController jobController;
    @Autowired
    private ProjectController projectController;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private OAuthService oAuthService;
    @Autowired
    private JobService jobService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private MapperService mapperService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RestTemplate authRestTemplate;
    @MockBean
    private KubernetesService kubernetesService;
    @Autowired
    private AsyncUploadJarService asyncUploadJarService;

    @Test
    void contextLoads() throws IOException {
        when(kubernetesService.getSecretsByLabels(anyMap())).thenReturn(new ArrayList<>());
        assertNotNull(asyncUploadJarService);
        assertNotNull(jarUpdateService);
        assertNotNull(startupListener);
        assertNotNull(kubernetesService);
        assertNotNull(jobController);
        assertNotNull(projectController);
        assertNotNull(authenticationService);
        assertNotNull(oAuthService);
        assertNotNull(jobService);
        assertNotNull(projectService);
        assertNotNull(mapperService);
        assertNotNull(restTemplate);
        assertNotNull(authRestTemplate);
    }
}

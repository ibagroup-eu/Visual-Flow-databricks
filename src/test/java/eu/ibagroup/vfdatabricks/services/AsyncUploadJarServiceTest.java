package eu.ibagroup.vfdatabricks.services;

import com.google.common.cache.LoadingCache;
import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static eu.ibagroup.vfdatabricks.dto.Constants.JAR_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AsyncUploadJarServiceTest {

    @Mock
    private DatabricksJobService databricksApiService;

    @Mock
    private LoadingCache<String, byte[]> jarFileCache;

    @Mock
    private ApplicationConfigurationProperties appProperties;

    @InjectMocks
    private AsyncUploadJarService asyncUploadJarService;

    @BeforeEach
    public void setUp() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
    }

    @Test
    public void testUploadJarFileToDatabricks() throws ExecutionException, InterruptedException {
        String projectId = "testProjectId";
        String path = "testPath";
        byte[] fileBytes = "testBytes".getBytes();
        String jarFilePath = "path/to/jar";

        try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.readAllBytes(Path.of(jarFilePath))).thenReturn(fileBytes);

            CompletableFuture<Object> future = asyncUploadJarService.uploadJarFileToDatabricks(projectId, path);

            assertNull(future.get());
            verify(databricksApiService).uploadFile(eq(projectId), eq(path), any(), eq(JAR_FILE_NAME));
        }
    }
}

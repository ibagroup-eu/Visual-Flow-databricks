package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.config.ApplicationConfigurationProperties;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportRequestDto;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportRequestDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static eu.ibagroup.vfdatabricks.dto.Constants.CONTEXT_PATH;
import static eu.ibagroup.vfdatabricks.dto.Constants.JOB_STORAGE_API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    private static final String PROJECT_ID = "vf-project-name";
    @Mock(answer = RETURNS_DEEP_STUBS)
    private ApplicationConfigurationProperties appProperties;
    @Mock
    private RestTemplate restTemplate;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        this.transferService = new TransferService(restTemplate, appProperties);
    }

    @Test
    void testExporting() {
        ExportRequestDto request = new ExportRequestDto();
        ResponseEntity<ExportResponseDto> response = ResponseEntity.ok(new ExportResponseDto());

        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/%s/exportResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                any(),
                eq(ExportResponseDto.class)))
                .thenReturn(response);

        assertEquals(response, transferService.exporting(PROJECT_ID, request),
                "Response should be the same as expected");
        verify(restTemplate).postForEntity(anyString(), any(), eq(ExportResponseDto.class));
    }

    @Test
    void testImporting() {
        ImportRequestDto request = new ImportRequestDto();
        ResponseEntity<ImportResponseDto> response = ResponseEntity.ok(new ImportResponseDto());

        when(restTemplate.postForEntity(
                eq(String.format("%s/%s/%s/%s/importResources",
                        appProperties.getJobStorage().getHost(),
                        CONTEXT_PATH,
                        JOB_STORAGE_API,
                        PROJECT_ID)),
                any(),
                eq(ImportResponseDto.class)))
                .thenReturn(response);

        assertEquals(response, transferService.importing(PROJECT_ID, request),
                "Response should be the same as expected");
        verify(restTemplate).postForEntity(anyString(), any(), eq(ImportResponseDto.class));
    }
}

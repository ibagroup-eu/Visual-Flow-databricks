package eu.ibagroup.vfdatabricks.controllers;

import eu.ibagroup.vfdatabricks.dto.exporting.ExportRequestDto;
import eu.ibagroup.vfdatabricks.dto.exporting.ExportResponseDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportRequestDto;
import eu.ibagroup.vfdatabricks.dto.importing.ImportResponseDto;
import eu.ibagroup.vfdatabricks.services.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransformControllerTest {

    private static final String PROJECT_ID = "projectId";
    @Mock
    private TransferService transferService;
    private TransferController transferController;

    @BeforeEach
    void setUp() {
        transferController = new TransferController(transferService);
    }

    @Test
    void testExporting() {
        ExportRequestDto request = new ExportRequestDto();
        ResponseEntity<ExportResponseDto> response = ResponseEntity.ok(new ExportResponseDto());
        when(transferService.exporting(PROJECT_ID, request)).thenReturn(response);
        assertEquals(response, transferController.exporting(PROJECT_ID, request),
                "Response should be the same as expected.");
        verify(transferService).exporting(anyString(), any(ExportRequestDto.class));
    }

    @Test
    void testImporting() {
        ImportRequestDto request = new ImportRequestDto();
        ResponseEntity<ImportResponseDto> response = ResponseEntity.ok(new ImportResponseDto());
        when(transferService.importing(PROJECT_ID, request)).thenReturn(response);
        assertEquals(response, transferController.importing(PROJECT_ID, request),
                "Response should be the same as expected.");
        verify(transferService).importing(anyString(), any(ImportRequestDto.class));
    }
}

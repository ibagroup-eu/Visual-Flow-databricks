package eu.ibagroup.vfdatabricks.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class RestTemplateResponseErrorHandlerTest {

    private RestTemplateResponseErrorHandler errorHandler;
    private ClientHttpResponse clientHttpResponse;

    @BeforeEach
    public void setUp() {
        errorHandler = new RestTemplateResponseErrorHandler(Arrays.asList(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.SERVICE_UNAVAILABLE.value()));
        clientHttpResponse = Mockito.mock(ClientHttpResponse.class);
    }

    @Test
    public void testHasErrorWithRetryStatusCode() throws IOException {
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        assertTrue(errorHandler.hasError(clientHttpResponse));
    }

    @Test
    public void testHasErrorWithNonRetryStatusCode() throws IOException {
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        assertFalse(errorHandler.hasError(clientHttpResponse));
    }

    @Test
    public void testHandleErrorWithRetryStatusCode() throws IOException {
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
        when(clientHttpResponse.getBody()).thenReturn(new InputStream() {
            private String response = "Service is unavailable";
            private byte[] buffer = response.getBytes(StandardCharsets.UTF_8);
            private int index = 0;

            @Override
            public int read() {
                return index < buffer.length ? buffer[index++] : -1;
            }
        });

        ForRetryRestTemplateException exception = assertThrows(ForRetryRestTemplateException.class, () -> errorHandler.handleError(clientHttpResponse));
        assertEquals("Service is unavailable", exception.getMessage());
    }

    @Test
    public void testHandleErrorWithNonRetryStatusCode() throws IOException {
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

        when(clientHttpResponse.getBody()).thenReturn(new InputStream() {
            private String response = "Service is unavailable";
            private byte[] buffer = response.getBytes(StandardCharsets.UTF_8);
            private int index = 0;

            @Override
            public int read() {
                return index < buffer.length ? buffer[index++] : -1;
            }
        });

        assertThrows(RestTemplateException.class, () -> errorHandler.handleError(clientHttpResponse));
    }
}

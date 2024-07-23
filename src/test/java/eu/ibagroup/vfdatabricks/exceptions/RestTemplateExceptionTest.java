package eu.ibagroup.vfdatabricks.exceptions;

import org.junit.jupiter.api.Test;

import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE;
import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestTemplateExceptionTest {

    @Test
    void testRestTemplateException() {
        RestTemplateException restTemplateException = new RestTemplateException(MESSAGE);
        assertEquals(MESSAGE, restTemplateException.getMessage(), MESSAGE_CONDITION);
    }

    @Test
    void testRestTemplateExceptionWithParams() {
        RestTemplateException restTemplateException = new RestTemplateException(MESSAGE,
                new Throwable("cause is a test"));
        assertEquals(MESSAGE, restTemplateException.getMessage(), MESSAGE_CONDITION);
        assertEquals("cause is a test", restTemplateException.getCause().getMessage(),
                MESSAGE_CONDITION);
    }
}

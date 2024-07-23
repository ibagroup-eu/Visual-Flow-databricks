package eu.ibagroup.vfdatabricks.exceptions;

import org.junit.jupiter.api.Test;

import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE;
import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ForRetryRestTemplateExceptionTest {

    @Test
    void testForRetryRestTemplateException() {
        ForRetryRestTemplateException forRetryRestTemplateException = new ForRetryRestTemplateException(MESSAGE);
        assertEquals(MESSAGE, forRetryRestTemplateException.getMessage(), MESSAGE_CONDITION);
    }

    @Test
    void testForRetryRestTemplateExceptionWithParams() {
        ForRetryRestTemplateException forRetryRestTemplateException = new ForRetryRestTemplateException(MESSAGE,
                new Throwable("cause is a test"));
        assertEquals(MESSAGE, forRetryRestTemplateException.getMessage(), MESSAGE_CONDITION);
        assertEquals("cause is a test", forRetryRestTemplateException.getCause().getMessage(),
                MESSAGE_CONDITION);
    }
}

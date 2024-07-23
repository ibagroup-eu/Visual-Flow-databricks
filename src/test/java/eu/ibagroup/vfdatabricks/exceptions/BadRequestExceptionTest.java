package eu.ibagroup.vfdatabricks.exceptions;

import org.junit.jupiter.api.Test;

import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE;
import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BadRequestExceptionTest {

    @Test
    void testBadRequestException() {
        BadRequestException badRequestException = new BadRequestException(MESSAGE);
        assertEquals(MESSAGE, badRequestException.getMessage(), MESSAGE_CONDITION);
    }

    @Test
    void testBadRequestExceptionWithParams() {
        BadRequestException badRequestException = new BadRequestException(MESSAGE, new Throwable("cause is a test"));
        assertEquals(MESSAGE, badRequestException.getMessage(), MESSAGE_CONDITION);
        assertEquals("cause is a test", badRequestException.getCause().getMessage(),
                MESSAGE_CONDITION);
    }
}

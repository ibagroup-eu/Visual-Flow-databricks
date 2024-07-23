package eu.ibagroup.vfdatabricks.exceptions;

import org.junit.jupiter.api.Test;

import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE;
import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalProcessingExceptionTest {

    @Test
    void testInternalProcessingExceptionTest() {
        InternalProcessingException internalProcessingException = new InternalProcessingException(MESSAGE);
        assertEquals(MESSAGE, internalProcessingException.getMessage(), MESSAGE_CONDITION);
    }
}

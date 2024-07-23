package eu.ibagroup.vfdatabricks.exceptions;

import org.junit.jupiter.api.Test;

import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE;
import static eu.ibagroup.vfdatabricks.exceptions.ExceptionsConstants.MESSAGE_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationExceptionTest {
    @Test
    void testConfigurationException() {
        ConfigurationException configurationException = new ConfigurationException(MESSAGE);
        assertEquals(MESSAGE, configurationException.getMessage(), MESSAGE_CONDITION);
    }

    @Test
    void testConfigurationExceptionWithParams() {
        Exception ex = new Exception();
        ConfigurationException configurationException = new ConfigurationException(MESSAGE, ex);

        assertEquals(MESSAGE, configurationException.getMessage(), MESSAGE_CONDITION);
        assertEquals(ex, configurationException.getCause(), "Cause must be equals to expected");
    }
}

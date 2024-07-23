package eu.ibagroup.vfdatabricks;

import eu.ibagroup.vfdatabricks.util.CronExpressionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CronExpressionUtilsTest {

    @Test
    void testUnixToQuartz() {
        String actual = CronExpressionUtils.unixToQuartz("15 18-20/1 * * 1");
        assertEquals("0 15 18-20/1 ? * 2 *", actual);
    }

    @Test
    void testQuartzToUnix() {
        String actual = CronExpressionUtils.quartzToUnix("0 15 18-20/1 ? * 2 *");
        assertEquals("15 18-20/1 * * 1", actual);
    }
}
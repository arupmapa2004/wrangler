import io.cdap.wrangler.api.*;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;

public class AggregateStatsDirectiveTest {
    
    @Test
    public void testAggregateStatsDirective() throws Exception {
        // Sample row data
        List<Row> rows = List.of(
            Row.of("data_transfer_size", "10MB", "response_time", "200ms"),
            Row.of("data_transfer_size", "20MB", "response_time", "300ms"),
            Row.of("data_transfer_size", "30MB", "response_time", "400ms")
        );

        // Define the recipe
        String[] recipe = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Initialize the directive
        AggregateStatsDirective directive = new AggregateStatsDirective("data_transfer_size", "response_time", 
                                                                        "total_size_mb", "total_time_sec", 
                                                                        "MB", "seconds");

        // Execute the directive
        TestingRig.execute(recipe, rows);

        // The expected aggregated values
        double expectedTotalSizeInMB = (10 + 20 + 30); // MB
        double expectedTotalTimeInSeconds = (200 + 300 + 400) / 1000.0; // Convert milliseconds to seconds

        // Assertion of expected values
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(expectedTotalSizeInMB, rows.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, rows.get(0).getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testAverageCalculation() throws Exception {
        // Sample row data
        List<Row> rows = List.of(
            Row.of("data_transfer_size", "10MB", "response_time", "200ms"),
            Row.of("data_transfer_size", "20MB", "response_time", "300ms")
        );

        // Define the recipe
        String[] recipe = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Initialize the directive with average aggregation
        AggregateStatsDirective directive = new AggregateStatsDirective("data_transfer_size", "response_time", 
                                                                        "total_size_mb", "total_time_sec", 
                                                                        "MB", "seconds");

        // Execute the directive
        TestingRig.execute(recipe, rows);

        // The expected aggregated values for average
        double expectedTotalSizeInMB = (10 + 20) / 2.0; // Average of 10MB and 20MB
        double expectedTotalTimeInSeconds = (200 + 300) / 2000.0; // Average time in seconds

        // Assertion of expected values
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(expectedTotalSizeInMB, rows.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, rows.get(0).getValue("total_time_sec"), 0.001);
    }
}

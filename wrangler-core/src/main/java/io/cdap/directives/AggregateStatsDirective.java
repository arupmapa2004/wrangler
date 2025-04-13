package io.cdap.wrangler.directive;

import io.cdap.wrangler.api.*;

import java.util.List;
import java.util.Map;

public class AggregateStatsDirective implements Directive {
    private final String sizeColumn;
    private final String timeColumn;
    private final String sizeTargetColumn;
    private final String timeTargetColumn;
    private final String sizeUnit;
    private final String timeUnit;

    private long totalSizeBytes = 0;
    private long totalTimeNs = 0;
    private int rowCount = 0;

    public AggregateStatsDirective(String sizeColumn, String timeColumn, String sizeTargetColumn, String timeTargetColumn,
                                   String sizeUnit, String timeUnit) {
        this.sizeColumn = sizeColumn;
        this.timeColumn = timeColumn;
        this.sizeTargetColumn = sizeTargetColumn;
        this.timeTargetColumn = timeTargetColumn;
        this.sizeUnit = sizeUnit;
        this.timeUnit = timeUnit;
    }

    @Override
    public void define() {
        // Optionally specify that this directive needs 4 arguments
    }

    @Override
    public void initialize(ExecutionContext context) {
        // Initialization logic if needed
    }

    @Override
    public void execute(List<Row> rows, ExecutionContext context) {
        for (Row row : rows) {
            // Assuming the column names are given as strings
            String sizeStr = row.getValue(sizeColumn).toString();
            String timeStr = row.getValue(timeColumn).toString();

            // Convert size to bytes and time to nanoseconds (this will depend on the units provided)
            long sizeInBytes = convertToBytes(sizeStr);
            long timeInNs = convertToNanoseconds(timeStr);

            // Aggregate the values
            totalSizeBytes += sizeInBytes;
            totalTimeNs += timeInNs;
            rowCount++;
        }
    }

    @Override
    public void finalize(ExecutionContext context) {
        // Convert the aggregated values to the required output units
        double totalSize = convertToSizeUnit(totalSizeBytes);
        double totalTime = convertToTimeUnit(totalTimeNs);

        // Create the final row with aggregated results
        Row resultRow = Row.of(sizeTargetColumn, totalSize, timeTargetColumn, totalTime);

        // Output resultRow to the context or further downstream processing
        context.write(resultRow);
    }

    private long convertToBytes(String sizeStr) {
        // Conversion logic for size (e.g., "10kb", "1MB", "5GB")
        ByteSize byteSize = new ByteSize(sizeStr);
        return byteSize.toBytes();
    }

    private long convertToNanoseconds(String timeStr) {
        // Conversion logic for time (e.g., "5ms", "1s")
        TimeDuration timeDuration = new TimeDuration(timeStr);
        return timeDuration.toNanoseconds();
    }

    private double convertToSizeUnit(long sizeInBytes) {
        // Convert bytes to the specified size unit (e.g., MB, GB)
        if ("MB".equalsIgnoreCase(sizeUnit)) {
            return sizeInBytes / (1024.0 * 1024.0); // Using 1024 as the base for MB
        } else if ("GB".equalsIgnoreCase(sizeUnit)) {
            return sizeInBytes / (1024.0 * 1024.0 * 1024.0); // GB conversion
        }
        // Default to bytes
        return sizeInBytes;
    }

    private double convertToTimeUnit(long timeInNs) {
        // Convert nanoseconds to the specified time unit (e.g., seconds, minutes)
        if ("seconds".equalsIgnoreCase(timeUnit)) {
            return timeInNs / 1_000_000_000.0; // Convert to seconds
        } else if ("minutes".equalsIgnoreCase(timeUnit)) {
            return timeInNs / 60_000_000_000.0; // Convert to minutes
        }
        // Default to nanoseconds
        return timeInNs;
    }
}

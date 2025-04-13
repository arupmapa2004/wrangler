package io.cdap.wrangler.directive;

import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveContext;
import io.cdap.wrangler.api.DirectiveInfo;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.Store;
import io.cdap.wrangler.api.annotations.Category;
import io.cdap.wrangler.api.annotations.Description;
import io.cdap.wrangler.api.annotations.Name;
import io.cdap.wrangler.api.annotations.Storable;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.utils.ByteSize;
import io.cdap.wrangler.utils.TimeDuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Directive to aggregate byte size and time duration values across rows.
 */
@Name("aggregate-stats")
@Category("aggregate")
@Description("Aggregates byte size and time duration columns, with optional unit conversion and average calculation.")
@Storable(false)
public class AggregateStats implements Directive {
    private String sourceSizeCol;
    private String sourceTimeCol;
    private String targetSizeCol;
    private String targetTimeCol;
    private String sizeUnit = "B";       // Default output size unit
    private String timeUnit = "ns";      // Default output time unit
    private String aggregationType = "total"; // "total" or "average"

    private Store store;

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder()
            .define("sourceSizeCol", ColumnName.class)
            .define("sourceTimeCol", ColumnName.class)
            .define("targetSizeCol", ColumnName.class)
            .define("targetTimeCol", ColumnName.class)
            .defineOptional("sizeUnit", Text.class)
            .defineOptional("timeUnit", Text.class)
            .defineOptional("aggregationType", Text.class);
        return builder.build();
    }

    @Override
    public void initialize(DirectiveContext ctx) throws Exception {
        List<Object> args = ctx.getArguments();
        sourceSizeCol = ((ColumnName) args.get(0)).value();
        sourceTimeCol = ((ColumnName) args.get(1)).value();
        targetSizeCol = ((ColumnName) args.get(2)).value();
        targetTimeCol = ((ColumnName) args.get(3)).value();

        if (args.size() > 4) sizeUnit = ((Text) args.get(4)).value().toUpperCase();
        if (args.size() > 5) timeUnit = ((Text) args.get(5)).value().toLowerCase();
        if (args.size() > 6) aggregationType = ((Text) args.get(6)).value().toLowerCase();
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }

    @Override
    public void execute(ExecutorContext ctx, Row row, List<Row> collector) throws Exception {
        if (store == null) {
            store = ctx.getStore();
        }

        Object sizeObj = row.getValue(sourceSizeCol);
        Object timeObj = row.getValue(sourceTimeCol);

        if (sizeObj != null && sizeObj instanceof String) {
            ByteSize size = new ByteSize((String) sizeObj);
            store.increment("totalSizeBytes", size.getBytes());
        }

        if (timeObj != null && timeObj instanceof String) {
            TimeDuration duration = new TimeDuration((String) timeObj);
            store.increment("totalTimeNs", duration.getNanos());
            store.increment("rowCount", 1L);
        }
    }

    @Override
    public List<Row> finalize(ExecutorContext ctx) throws Exception {
        long totalBytes = store.getOrDefault("totalSizeBytes", 0L);
        long totalNanos = store.getOrDefault("totalTimeNs", 0L);
        long rowCount = store.getOrDefault("rowCount", 1L);

        double outputSize = convertBytes(totalBytes, sizeUnit);
        double outputTime = "average".equals(aggregationType)
                ? convertTime(totalNanos / (double) rowCount, timeUnit)
                : convertTime(totalNanos, timeUnit);

        Row resultRow = new Row();
        resultRow.add(targetSizeCol, outputSize);
        resultRow.add(targetTimeCol, outputTime);

        return Collections.singletonList(resultRow);
    }

    private double convertBytes(long bytes, String unit) {
        switch (unit) {
            case "KB": return bytes / 1024.0;
            case "MB": return bytes / (1024.0 * 1024);
            case "GB": return bytes / (1024.0 * 1024 * 1024);
            case "B":
            default: return bytes;
        }
    }

    private double convertTime(double nanos, String unit) {
        switch (unit) {
            case "ms": return nanos / 1_000_000.0;
            case "s": return nanos / 1_000_000_000.0;
            case "m": return nanos / (60_000_000_000.0);
            case "ns":
            default: return nanos;
        }
    }
}

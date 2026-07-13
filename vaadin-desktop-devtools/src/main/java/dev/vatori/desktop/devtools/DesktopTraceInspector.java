package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public final class DesktopTraceInspector {
    private static final Pattern TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DIRECTION = Pattern.compile("\"direction\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<TraceLineInspector> LINE_INSPECTORS = List.of(
            RichTextEditorTraceInspector::inspectLine,
            CoreTraceInspector::inspectLine,
            FrameworkBootstrapTraceInspector::inspectLine,
            FrameworkEventTraceInspector::inspectLine,
            FrameworkResourceTraceInspector::inspectLine,
            FrameworkOutcomeTraceInspector::inspectLine,
            ThemeMatrixTraceInspector::inspectLine,
            FlowClientTraceInspector::inspectLine,
            FrontendResourcePipelineTraceInspector::inspectLine,
            FrontendManifestTraceInspector::inspectLine,
            VaadinComponentResourceTraceInspector::inspectLine,
            ProductionOwnerTraceInspector::inspectLine,
            FrontendBuildResourceExchangeTraceInspector::inspectLine,
            FrontendBuildResourceStageTraceInspector::inspectLine,
            FrontendBuildDomTraceInspector::inspectLine,
            GeneratedLoadOnDemandChunkTraceInspector::inspectLine,
            FlowClientEngineDomTraceInspector::inspectLine,
            ComponentResourceMatrixStageTraceInspector::inspectLine,
            ComponentResourceMatrixDomTraceInspector::inspectLine,
            FrameworkValidationDomTraceInspector::inspectLine);

    private DesktopTraceInspector() {
    }

    public static void main(String[] args) {
        System.exit(TraceInspectorCli.run(args));
    }

    public static TraceSummary inspect(Path path) throws IOException {
        TraceSummary summary = new TraceSummary();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                String line = normalize(rawLine);
                if (line.isBlank()) {
                    continue;
                }
                summary.total++;
                String type = first(TYPE, line, "unknown");
                String direction = first(DIRECTION, line, "unknown");
                summary.types.merge(type, 1, Integer::sum);
                summary.directions.merge(direction, 1, Integer::sum);
                for (TraceLineInspector inspector : LINE_INSPECTORS) {
                    inspector.inspect(summary, line);
                }
            }
        }
        return summary;
    }

    @FunctionalInterface
    private interface TraceLineInspector {
        void inspect(TraceSummary summary, String line);
    }
}

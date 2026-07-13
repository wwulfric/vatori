package dev.vatori.desktop.flow.ipc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record GeneratedLoadOnDemandChunkFileRegistry(
        String chunkId,
        String chunkImportSpecifier,
        String chunkArtifactPath,
        String chunkContentHash,
        String registrySource,
        String mappingSource,
        String generatedImportsArtifact,
        boolean noOpLoadOnDemand,
        String noChunkGeneratedReason,
        String sourceBuildMode,
        String sourceArtifactFamily,
        String loadOnDemandTableShape,
        String chunkFileResolver,
        String chunkFileStatus,
        int chunkFileBytes,
        String chunkFileSha256,
        String failureLayer,
        String nextResolutionStep,
        String sideEffectProof) {

    static final String REGISTRY_SOURCE = "generated-flow-imports-body-parser-v1_12";
    static final String MAPPING_SOURCE = "real-generated-flow-imports-loadondemand-table-v1_12";
    static final String CHUNK_FILE_RESOLVER = "generated-flow-imports-relative-chunk-file-resolver-v1_12";

    private static final String CHUNK_FILE_NOT_GENERATED_REASON =
            "real generated-flow-imports loadOnDemand table is a Promise.resolve(0) no-op; this Vaadin build did not generate a ./chunks/chunk-*.js branch for the current app";
    private static final String CHUNK_FILE_NEXT_RESOLUTION_STEP =
            "make the smoke app produce a real Vaadin generated lazy chunk or route the next build's ./chunks/chunk-*.js branch through this resolver";
    private static final Pattern CHUNK_IMPORT =
            Pattern.compile("key\\s*={2,3}\\s*['\"]([^'\"]+)['\"][\\s\\S]*?pending\\.push\\(import\\(['\"](\\./chunks/[^'\"]+\\.js)['\"]\\)\\)");
    private static final Pattern NOOP_LOAD_ON_DEMAND =
            Pattern.compile("const\\s+loadOnDemand\\s*=\\s*\\([^)]*\\)\\s*=>\\s*\\{\\s*return\\s+Promise\\.resolve\\(0\\);?\\s*\\}");

    static GeneratedLoadOnDemandChunkFileRegistry parse(String generatedImportsArtifact, String body) {
        String sourceBuildMode = sourceBuildMode(generatedImportsArtifact);
        String sourceArtifactFamily = "generated-lazy";
        String safeBody = body == null ? "" : body;
        Matcher chunkMatcher = CHUNK_IMPORT.matcher(safeBody);
        if (chunkMatcher.find()) {
            String chunkId = chunkMatcher.group(1);
            String chunkImportSpecifier = chunkMatcher.group(2);
            String chunkArtifactPath = resolveRelativeChunkPath(generatedImportsArtifact, chunkImportSpecifier);
            byte[] chunkBody = FlowIpcClasspathResources.bytes(chunkArtifactPath);
            if (chunkBody.length > 0) {
                String sha256 = FlowIpcDiagnostics.sha256(chunkBody);
                return new GeneratedLoadOnDemandChunkFileRegistry(chunkId,
                        chunkImportSpecifier,
                        chunkArtifactPath,
                        sha256,
                        REGISTRY_SOURCE,
                        MAPPING_SOURCE,
                        generatedImportsArtifact,
                        false,
                        "",
                        sourceBuildMode,
                        sourceArtifactFamily,
                        "branch-table-dynamic-import",
                        CHUNK_FILE_RESOLVER,
                        "available",
                        chunkBody.length,
                        sha256,
                        "",
                        "",
                        "chunk-file-body-available-for-staged-import");
            }
            return new GeneratedLoadOnDemandChunkFileRegistry(chunkId,
                    chunkImportSpecifier,
                    chunkArtifactPath,
                    "",
                    REGISTRY_SOURCE,
                    MAPPING_SOURCE,
                    generatedImportsArtifact,
                    false,
                    "",
                    sourceBuildMode,
                    sourceArtifactFamily,
                    "branch-table-dynamic-import",
                    CHUNK_FILE_RESOLVER,
                    "not-found",
                    0,
                    "",
                    "chunk-file-not-found",
                    "add the generated chunk artifact to the no-localhost resolver classpath or staging source",
                    "chunk-branch-observed-but-artifact-body-missing");
        }
        boolean noOp = NOOP_LOAD_ON_DEMAND.matcher(safeBody).find();
        return new GeneratedLoadOnDemandChunkFileRegistry("",
                "",
                "",
                "",
                REGISTRY_SOURCE,
                MAPPING_SOURCE,
                generatedImportsArtifact,
                noOp,
                noOp ? CHUNK_FILE_NOT_GENERATED_REASON
                        : "generated-flow-imports did not match a supported loadOnDemand chunk table shape",
                sourceBuildMode,
                sourceArtifactFamily,
                noOp ? "no-op-promise-resolve-0" : "unrecognized",
                CHUNK_FILE_RESOLVER,
                noOp ? "not-generated" : "unrecognized",
                0,
                "",
                noOp ? "chunk-file-not-generated" : "chunk-registry-unrecognized",
                noOp ? CHUNK_FILE_NEXT_RESOLUTION_STEP
                        : "extend the parser for the generated-flow-imports table shape emitted by this Vaadin version",
                noOp ? "no-chunk-generated-by-real-generated-flow-imports-no-op-table"
                        : "loadondemand-table-shape-unrecognized");
    }

    static int countChunkReferences(String body) {
        if (body == null || body.isBlank()) {
            return 0;
        }
        int count = 0;
        Matcher matcher = CHUNK_IMPORT.matcher(body);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String sourceBuildMode(String path) {
        if (path.startsWith("vaadin-dev-bundle/")) {
            return "vaadin-dev-bundle-production-build";
        }
        if (path.startsWith("META-INF/vatori/generated/")) {
            return "repository-generated-flow-fixture";
        }
        return "classpath-resource";
    }

    private static String resolveRelativeChunkPath(String generatedImportsArtifact, String specifier) {
        int lastSlash = generatedImportsArtifact.lastIndexOf('/');
        String parent = lastSlash < 0 ? "" : generatedImportsArtifact.substring(0, lastSlash + 1);
        String relative = specifier.startsWith("./") ? specifier.substring(2) : specifier;
        return parent + relative;
    }
}

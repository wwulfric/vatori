package dev.vatori.desktop.cli.maven;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

/** Exports Maven's effective selected reactor model for dev-vite observation. */
public final class DevelopmentModelLifecycleParticipant
        extends AbstractMavenLifecycleParticipant {
    public static final String OUTPUT_PROPERTY =
            "vatori.development.model.output";
    public static final String HEADER =
            "VATORI_MAVEN_DEVELOPMENT_MODEL\t1";

    @Override
    public void afterProjectsRead(MavenSession session)
            throws MavenExecutionException {
        String configured = session.getUserProperties()
                .getProperty(OUTPUT_PROPERTY, "");
        if (configured.isBlank()) {
            return;
        }

        Path output = Path.of(configured)
                .toAbsolutePath().normalize();
        try {
            writeModel(output, session.getProjects());
        } catch (IOException exception) {
            throw new MavenExecutionException(
                    "Cannot export the effective Maven development model to "
                            + output,
                    exception);
        }
    }

    private static void writeModel(
            Path output,
            List<MavenProject> projects) throws IOException {
        Set<Path> modules = new LinkedHashSet<>();
        Set<Path> roots = new LinkedHashSet<>();
        Set<Path> poms = new LinkedHashSet<>();
        for (MavenProject project : projects) {
            Path directory = path(project.getBasedir());
            if (directory != null && !"pom".equals(project.getPackaging())) {
                modules.add(directory);
                if (project.getBuild() != null) {
                    addPath(roots,
                            project.getBuild().getSourceDirectory());
                }
                for (String sourceRoot : project.getCompileSourceRoots()) {
                    addPath(roots, sourceRoot);
                }
                for (Resource resource : project.getResources()) {
                    addPath(roots, resource.getDirectory());
                }
            }
            addPath(poms, project.getFile());
            for (MavenProject parent = project.getParent();
                 parent != null;
                 parent = parent.getParent()) {
                addPath(poms, parent.getFile());
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        append(lines, "module", modules);
        append(lines, "root", roots);
        append(lines, "pom", poms);
        Files.createDirectories(output.getParent());
        Path temporary = output.resolveSibling(
                output.getFileName() + ".tmp");
        Files.write(
                temporary,
                lines,
                StandardCharsets.UTF_8);
        try {
            Files.move(
                    temporary,
                    output,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporary,
                    output,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void append(
            List<String> lines,
            String type,
            Set<Path> paths) {
        paths.stream()
                .sorted(Comparator.comparing(Path::toString))
                .map(path -> type + "\t" + encode(path))
                .forEach(lines::add);
    }

    private static String encode(Path path) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                path.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void addPath(Set<Path> paths, java.io.File file) {
        if (file != null) {
            paths.add(file.toPath().toAbsolutePath().normalize());
        }
    }

    private static void addPath(Set<Path> paths, String value) {
        if (value != null && !value.isBlank()) {
            paths.add(Path.of(value).toAbsolutePath().normalize());
        }
    }

    private static Path path(java.io.File file) {
        return file == null ? null
                : file.toPath().toAbsolutePath().normalize();
    }
}

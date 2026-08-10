package dev.vatori.desktop.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

record ApplicationBundleInputs(
        Path macosIcon,
        Path commonResources,
        Path macosResources) {

    private static final Path DESKTOP_ROOT = Path.of("src", "main", "desktop");

    static ApplicationBundleInputs discover(Path module)
            throws VaadinDesktopCli.CliException {
        Path root = module.toAbsolutePath().normalize().resolve(DESKTOP_ROOT);
        Path macosRoot = root.resolve("macos");
        return new ApplicationBundleInputs(
                optionalFile(
                        macosRoot.resolve("AppIcon.icns"),
                        "macOS application icon"),
                optionalDirectory(
                        root.resolve("resources"),
                        "common application resources"),
                optionalDirectory(
                        macosRoot.resolve("resources"),
                        "macOS application resources"));
    }

    List<String> macosBuilderArguments() {
        return List.of(
                value(macosIcon),
                value(commonResources),
                value(macosResources));
    }

    private static Path optionalFile(Path path, String description)
            throws VaadinDesktopCli.CliException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
            throw new VaadinDesktopCli.CliException(
                    1, description + " must be a regular, non-symlink file: " + path);
        }
        return path.toAbsolutePath().normalize();
    }

    private static Path optionalDirectory(Path path, String description)
            throws VaadinDesktopCli.CliException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (Files.isSymbolicLink(path) || !Files.isDirectory(path)) {
            throw new VaadinDesktopCli.CliException(
                    1, description + " must be a non-symlink directory: " + path);
        }
        try (var paths = Files.walk(path)) {
            Path symlink = paths.filter(Files::isSymbolicLink).findFirst().orElse(null);
            if (symlink != null) {
                throw new VaadinDesktopCli.CliException(
                        1, description + " must be self-contained; symlink found: " + symlink);
            }
        } catch (IOException exception) {
            throw new VaadinDesktopCli.CliException(
                    1, "Cannot inspect " + description + ": " + exception.getMessage());
        }
        return path.toAbsolutePath().normalize();
    }

    private static String value(Path path) {
        return path == null ? "" : path.toString();
    }
}

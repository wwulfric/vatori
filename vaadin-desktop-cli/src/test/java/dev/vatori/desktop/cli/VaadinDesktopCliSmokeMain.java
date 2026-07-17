package dev.vatori.desktop.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class VaadinDesktopCliSmokeMain {
    private VaadinDesktopCliSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path target = Files.createTempDirectory("vatori-cli-jars");
        try {
            expectJarSelectionFailure(target, "zero runnable JARs");
            Path first = target.resolve("application.jar");
            writeJar(first, "example.Main");
            if (!VaadinDesktopCli.findRunnableJar(target).equals(first)) {
                throw new IllegalStateException("CLI selected the wrong runnable JAR");
            }
            writeJar(target.resolve("plain.jar"), null);
            if (!VaadinDesktopCli.findRunnableJar(target).equals(first)) {
                throw new IllegalStateException("CLI treated a plain JAR as runnable");
            }
            writeJar(target.resolve("second.jar"), "example.SecondMain");
            expectJarSelectionFailure(target, "multiple runnable JARs");
        } finally {
            try (var paths = Files.walk(target)) {
                for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int helpStatus = VaadinDesktopCli.run(
                new String[]{"help"}, Map.of(),
                new PrintStream(output, true, StandardCharsets.UTF_8), System.err);
        if (helpStatus != 0 || !output.toString(StandardCharsets.UTF_8).contains("dev-compat")) {
            throw new IllegalStateException("CLI help contract is incomplete");
        }
        int unknownStatus = VaadinDesktopCli.run(
                new String[]{"unknown"}, Map.of(), System.out, System.err);
        if (unknownStatus == 0) {
            throw new IllegalStateException("CLI accepted an unknown command");
        }
        System.out.println("Vaadin Desktop CLI contract passed");
    }

    private static void expectJarSelectionFailure(Path target, String scenario) {
        try {
            VaadinDesktopCli.findRunnableJar(target);
            throw new IllegalStateException("CLI accepted " + scenario);
        } catch (VaadinDesktopCli.CliException expected) {
            // Expected strict unique runnable JAR contract.
        }
    }

    private static void writeJar(Path path, String mainClass) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (mainClass != null) {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        }
        try (JarOutputStream ignored = new JarOutputStream(
                Files.newOutputStream(path), manifest)) {
            // Manifest-only fixtures are sufficient for runnable JAR selection.
        }
    }
}

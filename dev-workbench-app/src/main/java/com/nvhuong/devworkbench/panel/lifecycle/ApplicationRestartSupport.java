package com.nvhuong.devworkbench.panel.lifecycle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** Builds a restart plan for the current launcher and starts it after this process exits. */
public final class ApplicationRestartSupport {
    private static final String RESTART_LAUNCHER_CLASS = RestartLauncher.class.getName();

    private ApplicationRestartSupport() {
    }

    public static RestartPlan prepare() {
        ProcessHandle.Info info = ProcessHandle.current().info();
        Optional<String> command = info.command();
        String classPath = System.getProperty("java.class.path");
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                isWindows() ? "java.exe" : "java");
        if (command.isEmpty() || classPath == null || classPath.isBlank() || !javaExecutable.toFile().isFile()) {
            return null;
        }
        return new RestartPlan(ProcessHandle.current().pid(), javaExecutable.toString(), classPath,
                command.get(), info.arguments().orElseGet(() -> new String[0]));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static final class RestartPlan {
        private final long parentPid;
        private final String javaExecutable;
        private final String classPath;
        private final String applicationCommand;
        private final List<String> applicationArguments;

        private RestartPlan(long parentPid, String javaExecutable, String classPath,
                            String applicationCommand, String[] applicationArguments) {
            this.parentPid = parentPid;
            this.javaExecutable = javaExecutable;
            this.classPath = classPath;
            this.applicationCommand = applicationCommand;
            this.applicationArguments = List.of(applicationArguments);
        }

        public void startAfterExit() throws IOException {
            List<String> command = new ArrayList<>();
            command.add(javaExecutable);
            command.add("-cp");
            command.add(classPath);
            command.add(RESTART_LAUNCHER_CLASS);
            command.add(Long.toString(parentPid));
            command.add(encode(applicationCommand));
            command.add(Integer.toString(applicationArguments.size()));
            applicationArguments.stream().map(ApplicationRestartSupport::encode).forEach(command::add);

            new ProcessBuilder(command)
                    .directory(Path.of(System.getProperty("user.dir")).toFile())
                    .redirectInput(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

package com.nvhuong.devworkbench.panel.lifecycle;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Waits for the previous GUI process to release its single-instance lock, then relaunches it. */
public final class RestartLauncher {
    private static final Duration WAIT_LIMIT = Duration.ofMinutes(2);
    private static final long POLL_INTERVAL_MILLIS = 250;

    private RestartLauncher() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            return;
        }
        long parentPid = Long.parseLong(args[0]);
        String applicationCommand = decode(args[1]);
        int argumentCount = Integer.parseInt(args[2]);
        if (argumentCount < 0 || args.length != argumentCount + 3) {
            return;
        }

        long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
        while (ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false)) {
            if (System.nanoTime() >= deadline) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        List<String> command = new ArrayList<>(argumentCount + 1);
        command.add(applicationCommand);
        for (int index = 0; index < argumentCount; index++) {
            command.add(decode(args[index + 3]));
        }
        new ProcessBuilder(command).start();
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

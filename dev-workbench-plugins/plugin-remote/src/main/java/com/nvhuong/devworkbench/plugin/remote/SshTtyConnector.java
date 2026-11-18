package com.nvhuong.devworkbench.plugin.remote;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Bridges an SSH PTY to JediTerm while preserving UTF-8 decoder state across network packets. */
final class SshTtyConnector implements TtyConnector {
    private final Session.Shell shell;
    private final Reader reader;
    private final OutputStream output;
    private final String name;

    SshTtyConnector(Session.Shell shell, String name) {
        this.shell = shell;
        this.reader = new InputStreamReader(shell.getInputStream(), StandardCharsets.UTF_8);
        this.output = shell.getOutputStream();
        this.name = name;
    }

    @Override public int read(char[] buffer, int offset, int length) throws IOException {
        return reader.read(buffer, offset, length);
    }

    @Override public void write(byte[] bytes) throws IOException {
        output.write(bytes);
        output.flush();
    }

    @Override public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override public boolean isConnected() { return shell.isOpen(); }

    @Override public void resize(TermSize size) {
        try { shell.changeWindowDimensions(size.getColumns(), size.getRows(), 0, 0); }
        catch (Exception ignored) { }
    }

    @Override public int waitFor() throws InterruptedException {
        try { shell.join(); return 0; }
        catch (Exception exception) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            return 1;
        }
    }

    @Override public boolean ready() throws IOException { return reader.ready(); }
    @Override public String getName() { return name; }

    @Override public void close() {
        try { shell.close(); } catch (Exception ignored) { }
    }
}

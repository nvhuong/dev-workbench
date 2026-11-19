package com.nvhuong.devworkbench.plugin.capture;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CaptureProxyRawTunnelTest {
    private CaptureProxyService proxy;
    private ServerSocket echoServer;

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (proxy != null) {
            proxy.stop();
        }
        if (echoServer != null) {
            echoServer.close();
        }
    }

    /**
     * A CONNECT tunnel carrying neither TLS nor HTTP (SSH, database or other TCP protocols, HTTP/2 prior knowledge)
     * used to be treated as TLS, so the MITM handshake failed and the connection was dropped. It must be relayed.
     */
    @Test(timeOut = 120_000)
    public void relaysUnknownProtocolInsideConnectTunnel() throws Exception {
        echoServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread echo = new Thread(() -> {
            try (Socket socket = echoServer.accept()) {
                socket.getInputStream().transferTo(socket.getOutputStream());
            } catch (Exception ignored) {
                // server closed
            }
        }, "raw-tunnel-echo");
        echo.setDaemon(true);
        echo.start();

        int proxyPort;
        try (ServerSocket probe = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            proxyPort = probe.getLocalPort();
        }
        proxy = new CaptureProxyService();
        proxy.start("127.0.0.1", proxyPort, false, "");

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxyPort)) {
            client.setSoTimeout(60_000);
            OutputStream out = client.getOutputStream();
            InputStream in = client.getInputStream();
            String authority = "127.0.0.1:" + echoServer.getLocalPort();
            out.write(("CONNECT " + authority + " HTTP/1.1\r\nHost: " + authority + "\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            String connectResponse = readHeaders(in);
            assertTrue(connectResponse.startsWith("HTTP/1.1 200"), connectResponse);

            byte[] payload = "SSH-2.0-DevWorkbenchTest\r\n".getBytes(StandardCharsets.US_ASCII);
            out.write(payload);
            out.flush();
            byte[] echoed = in.readNBytes(payload.length);
            assertEquals(new String(echoed, StandardCharsets.US_ASCII), new String(payload, StandardCharsets.US_ASCII),
                    "unknown protocol bytes must be relayed through the CONNECT tunnel");
        }
    }

    private static String readHeaders(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        byte[] terminator = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        while (matched < terminator.length) {
            int next = in.read();
            if (next < 0) {
                break;
            }
            buffer.write(next);
            matched = next == terminator[matched] ? matched + 1 : (next == terminator[0] ? 1 : 0);
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }
}

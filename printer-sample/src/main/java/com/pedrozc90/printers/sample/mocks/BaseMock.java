package com.pedrozc90.printers.sample.mocks;

import com.pedrozc90.printers.core.objects.RawPacket;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.pedrozc90.printers.core.objects.ControlCmd.EOF;

public abstract class BaseMock implements AutoCloseable {

    protected final Logger logger;
    protected final ExecutorService clients = Executors.newCachedThreadPool();

    protected final Charset charset;
    protected ServerSocket server;
    protected Thread thread;

    public BaseMock(final Charset charset) {
        this.logger = Logger.getLogger(getClass());
        this.charset = charset;
        init();
    }

    /**
     * Returns the host name of the bound socket (useful when you want to connect to it from another JVM).
     */
    public String getHost() {
        if (server == null) return "localhost";
        return server.getInetAddress().getHostAddress();
    }

    /**
     * Returns the actual bound port (useful when you requested port 0).
     */
    public int getPort() {
        if (server == null) return 0;
        return server.getLocalPort();
    }

    private void init() {
        try {
            server = new ServerSocket(0);
            server.setReuseAddress(true);

            final Acceptor acceptor = new Acceptor(server, charset);
            thread = new Thread(acceptor, "sato-mock-acceptor");
            thread.setDaemon(true);
            thread.start();

            logger.debugf("Listening on port %d", getPort());
        } catch (IOException e) {
            logger.error("Error starting server", e);
        }
    }

    @Override
    public void close() throws IOException {
        logger.debugf("Stopping...");

        try {
            if (server != null && !server.isClosed()) {
                try {
                    server.close();
                } finally {
                    server = null;
                }
            }
        } catch (IOException e) {
            logger.error("Error stopping server", e);
        }

        clients.shutdownNow();

        try {
            final boolean terminated = clients.awaitTermination(500, TimeUnit.MILLISECONDS);
            if (terminated) {
                logger.debugf("Client executor terminated");
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }

        logger.debugf("Stopped");
    }

    /**
     * Parse commands from raw bytes, handling both framed (STX... ETX) and unframed messages
     */
    public abstract List<RawPacket> parseData(final byte[] data, final Charset charset);

    /**
     * Handle individual command and send appropriate response
     */
    public abstract RawPacket handlePacket(final RawPacket packet) throws IOException;

    private class Acceptor implements Runnable {

        private final ServerSocket socket;
        private final Charset charset;

        Acceptor(final ServerSocket socket, final Charset charset) {
            this.socket = socket;
            this.charset = charset;
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    final Socket client = socket.accept();
                    logger.debugf("Accepted client %s", client.getRemoteSocketAddress());

                    final Runnable handler = new MockRunnable(client, charset);
                    clients.execute(handler);
                }
            } catch (IOException e) {
                if (socket.isClosed()) {
                    // normal shutdown
                    logger.debug("Socket closed, stopping acceptor");
                } else {
                    logger.error("Accept error", e);
                }
            }
        }
    }

    public class MockRunnable implements Runnable {

        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private final Charset charset;

        public MockRunnable(final Socket socket, final Charset charset) throws IOException {
            this.socket = socket;
            this.charset = charset;
            this.socket.setSoTimeout(0); // blocking
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        @Override
        public void run() {
            try {
                final byte[] tmp = new byte[1024];

                // read loop - process commands as they arrive
                int read;
                while ((read = in.read(tmp)) != EOF) {
                    if (read > 0) {
                        final byte[] data = Arrays.copyOf(tmp, read);
                        processData(data);
                    }
                }
            } catch (IOException e) {
                // connection closed or error
                logger.error("Client disconnected", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }

        /**
         * Process received data and extract commands
         */
        private void processData(final byte[] data) throws IOException {
            logger.debugf("Received data: %s", Arrays.toString(data));

            final List<RawPacket> packets = parseData(data, charset);

            for (RawPacket packet : packets) {
                logger.infof("Processing command: %s", packet);
                final RawPacket response = handlePacket(packet);
                if (response != null) {
                    send(response);
                }
            }
        }

        /**
         * Send raw bytes to the client
         */
        protected void send(final RawPacket payload) throws IOException {
            final byte[] bytes = payload.getBytes();
            out.write(bytes);
            out.flush();
            logger.debugf("Sent: '%s'", payload);
        }

    }

}

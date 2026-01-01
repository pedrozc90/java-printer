package com.contare.printers.utils;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A simple test helper that runs a ServerSocket and dispatches accepted sockets to handlers.
 * Tests enqueue one handler per expected incoming connection using enqueueHandler(...).
 * <p>
 * Usage pattern:
 * PrinterServer server = new PrinterServer(); // chooses ephemeral port
 * server.enqueueHandler(socket -> { /* interact with socket * / });
 * int port = server.getPort();
 */
public class PrinterServer implements AutoCloseable {

    private final BlockingQueue<Consumer<Socket>> handlers = new LinkedBlockingQueue<>();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    private final ServerSocket _server;
    private final Thread _thread;
    private volatile boolean running = true;

    public PrinterServer() throws IOException {
        this(0);
    }

    public PrinterServer(int port) throws IOException {
        _server = new ServerSocket(port);

        _thread = new Thread(new Acceptor(_server), "printer-server-accept");
        _thread.setDaemon(true);
        _thread.start();
    }

    /**
     * Enqueue a handler that will be used for the next accepted connection.
     * The handler receives the accepted Socket and should manage I/O and closing behavior.
     */
    public void enqueueHandler(final Consumer<Socket> handler) {
        handlers.add(handler);
    }

    /**
     * Get the server listening port (useful when constructed with port 0).
     */
    public int getPort() {
        return _server.getLocalPort();
    }

    /**
     * Stop the server and release resources.
     */
    public void stop() throws IOException {
        running = false;
        try {
            if (!_server.isClosed()) {
                _server.close();
            }
        } finally {
            if (!clientPool.isShutdown()) {
                clientPool.shutdownNow();
            }
        }
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private class Acceptor implements Runnable {

        private final Logger logger = Logger.getLogger(Acceptor.class);

        private final ServerSocket _server;

        public Acceptor(final ServerSocket server) {
            this._server = server;
        }

        @Override
        public void run() {
            try {
                while (running && !_server.isClosed()) {
                    Socket socket = null;
                    try {
                        socket = _server.accept();

                        // Wait a bounded time for a handler to be provided for this connection.
                        // If none is provided, close the socket and continue to next accept.
                        Consumer<Socket> handler = handlers.poll(5, TimeUnit.SECONDS);
                        if (handler == null) {
                            try {
                                socket.close();
                            } catch (IOException ignored) {
                                // ignored
                            }
                            continue;
                        }

                        final Socket clientSocket = socket;
                        clientPool.submit(() -> {
                            try {
                                handler.accept(clientSocket);
                            } catch (Exception e) {
                                // allow tests to observe errors via futures; but print stack for debugging
                                logger.error("Error", e);
                            } finally {
                                try {
                                    clientSocket.close();
                                } catch (IOException ignored) {
                                    // ignored
                                }
                            }
                        });

                    } catch (SocketException e) {
                        if (running) {
                            logger.error("Socket error", e);
                        }
                        break;
                    } catch (InterruptedException e) {
                        logger.error("Interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    } catch (IOException e) {
                        // continue accepting unless server is shutting down
                        logger.error("Error accepting connection", e);
                    }
                }
            } finally {
                // cleanup pool
                clientPool.shutdownNow();
            }
        }

    }

}

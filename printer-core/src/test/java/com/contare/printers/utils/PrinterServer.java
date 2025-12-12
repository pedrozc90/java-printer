package com.contare.printers.utils;

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

    private final ServerSocket serverSocket;
    private final BlockingQueue<Consumer<Socket>> handlers = new LinkedBlockingQueue<>();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final Thread acceptThread;
    private volatile boolean running = true;

    public PrinterServer() throws IOException {
        this(0);
    }

    public PrinterServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "printer-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        try {
            while (running && !serverSocket.isClosed()) {
                Socket sock = null;
                try {
                    sock = serverSocket.accept();

                    // Wait a bounded time for a handler to be provided for this connection.
                    // If none is provided, close the socket and continue to next accept.
                    Consumer<Socket> handler = handlers.poll(5, TimeUnit.SECONDS);
                    if (handler == null) {
                        try {
                            sock.close();
                        } catch (IOException ignored) {
                        }
                        continue;
                    }

                    final Socket clientSocket = sock;
                    clientPool.submit(() -> {
                        try {
                            handler.accept(clientSocket);
                        } catch (Exception e) {
                            // allow tests to observe errors via futures; but print stack for debugging
                            e.printStackTrace();
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException ignored) {
                            }
                        }
                    });

                } catch (SocketException se) {
                    if (running) {
                        se.printStackTrace();
                    }
                    break;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    // continue accepting unless server is shutting down
                }
            }
        } finally {
            // cleanup pool
            clientPool.shutdownNow();
        }
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
        return serverSocket.getLocalPort();
    }

    /**
     * Stop the server and release resources.
     */
    public void stop() throws IOException {
        running = false;
        try {
            serverSocket.close();
        } finally {
            clientPool.shutdownNow();
        }
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}

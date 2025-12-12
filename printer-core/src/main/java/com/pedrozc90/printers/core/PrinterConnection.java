package com.pedrozc90.printers.core;

import com.pedrozc90.printers.core.exceptions.PrinterConnectionException;
import com.pedrozc90.printers.core.objects.RawPacket;
import lombok.Data;
import org.jboss.logging.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight TCP connection helper for networked label printers.
 *
 * <p>Manages a {@link Socket} with buffered input/output streams and exposes
 * helpers to connect, send bytes/strings and read responses. IO errors are
 * surfaced as {@link IOException} so callers can handle them appropriately.</p>
 */
@Data
public class PrinterConnection implements Closeable {

    // control characters
    private static final int EOF = -1;      // end of file for InputStream.read() comparisons
    private static final byte STX = 0x02;   // start of text: first character of message text, and may be used to terminate the message heading.
    private static final byte ETX = 0x03;   // end of text: in message transmission, delimits the end of the message.

    private final Logger logger = Logger.getLogger(PrinterConnection.class);

    private final String ip;
    private final Integer port;
    private final Charset charset;

    private final Object lock = new Object();
    private Socket _socket;
    private BufferedInputStream _input;
    private BufferedOutputStream _output;

    public PrinterConnection(final String ip, final Integer port, final Charset charset) {
        this.ip = Objects.requireNonNull(ip, "IP address must not be null");
        this.port = Objects.requireNonNull(port, "Port must not be null");
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
    }

    public PrinterConnection(final String ip, final Integer port) {
        this(ip, port, StandardCharsets.UTF_8);
    }

    /**
     * Returns the connection state of the printer.
     *
     * @return true if the printer socket was successfully connected, otherwise false.
     */
    public boolean isConnected() {
        return (_socket != null && _socket.isConnected());
    }

    /**
     * Returns the close state of the printer.
     *
     * @return true if the printer socker has been closed.
     */
    public boolean isClosed() {
        return (_socket != null && _socket.isClosed());
    }

    /**
     * Connects to the printer using a tcp socket.
     *
     * @param timeout - connection timeout in milliseconds. if 0 means infinite timeout.
     * @throws PrinterConnectionException
     */
    public void connect(final int timeout) throws PrinterConnectionException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }

        synchronized (lock) {
            try {
                // create tcp socket
                _socket = new Socket();
                _socket.connect(new InetSocketAddress(ip, port), timeout);
                _socket.setSoTimeout(5_000);
                _socket.setTcpNoDelay(true);

                // create buffered streams for efficient IO
                _input = new BufferedInputStream(_socket.getInputStream());
                _output = new BufferedOutputStream(_socket.getOutputStream());

                logger.debugf("Connected to printer %s:%d (timeout = %d ms)", ip, port, timeout);
            } catch (SocketTimeoutException e) {
                throw new PrinterConnectionException(e, "Socket timeout connecting to printer %s:%d", ip, port);
            } catch (SocketException e) {
                throw new PrinterConnectionException(e, "Socket error on printer %s:%d", ip, port);
            } catch (IOException e) {
                throw new PrinterConnectionException(e, "Error connecting to printer %s:%d", ip, port);
            }
        }
    }

    public void connect() throws PrinterConnectionException {
        connect(5_000);
    }

    /**
     * Disconnect printer socket and free resources.
     */
    private void disconnect() {
        try {
            if (_output != null) {
                try {
                    _output.flush();
                } catch (IOException e) {
                    logger.errorf(e, "Error flushing output stream");
                }
                try {
                    _output.close();
                } catch (IOException e) {
                    logger.errorf(e, "Error closing output stream");
                }
            }

            if (_input != null) {
                try {
                    _input.close();
                } catch (IOException e) {
                    logger.errorf(e, "Error closing input stream");
                }
            }

            if (_socket != null && !_socket.isClosed()) {
                try {
                    _socket.close();
                } catch (IOException e) {
                    logger.errorf(e, "Error closing socket");
                }
            }
        } finally {
            _output = null;
            _input = null;
            _socket = null;
        }
    }

    /**
     * Disconnect the printer socket if already connected, then try to connect again.
     *
     * @throws PrinterConnectionException - if a connection error happens.
     */
    public void reconnect() throws PrinterConnectionException {
        if (isConnected()) {
            disconnect();
        }
        connect();
    }

    @Override
    public void close() throws PrinterConnectionException {
        synchronized (lock) {
            disconnect();
        }
    }

    public String status() {
        if (_socket == null) {
            return "NOT FOUND";
        } else if (_socket.isClosed()) {
            return "CLOSED";
        } else if (_socket.isConnected()) {
            return "CONNECTED";
        } else if (_socket.isBound()) {
            return "BOUND";
        }
        return "DISCONNECTED";
    }

    public String address() {
        return String.format("%s:%d", ip, port);
    }

    /**
     * Send a String to the printer using the configured charset. This writes raw bytes and flushes.
     *
     * @param data payload
     * @throws PrinterConnectionException if IO error or not connected
     */
    public void send(final String data) throws PrinterConnectionException {
        if (data == null) return;
        final byte[] bytes = data.getBytes(charset);
        send(bytes);
    }

    /**
     * Send raw bytes to the printer and flush.
     *
     * @param bytes payload
     * @throws PrinterConnectionException if IO error or not connected
     */
    public void send(final byte[] bytes) throws PrinterConnectionException {
        if (bytes == null) return;
        synchronized (lock) {
            try {
                if (_output == null) {
                    throw new PrinterConnectionException("Printer output stream is not initialized. Are you connected?");
                }
                _output.write(bytes);
                _output.flush();
                logger.tracef("Write: '%s'", new String(bytes, charset));
            } catch (IOException e) {
                throw new PrinterConnectionException(e, "Error writing to printer %s:%d", ip, port);
            }
        }
    }

    /**
     * Read all available bytes from the input stream into a byte array.
     *
     * <p>If a socket read timeout (SO_TIMEOUT) is configured, this method will
     * return the bytes accumulated so far when a {@link SocketTimeoutException}
     * is thrown. That behavior allows callers to rely on the socket timeout as
     * a natural response boundary.</p>
     *
     * @return bytes read from the printer (may be empty)
     * @throws PrinterConnectionException if the input stream is not initialized or other IO errors occur
     */
    public byte[] readAsBytes() throws PrinterConnectionException {
        if (_input == null) {
            throw new PrinterConnectionException("Printer input stream is not initialized. Are you connected?");
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int read;
        try {
            while ((read = _input.read(buffer)) != EOF) {
                if (read > 0) {
                    out.write(buffer, 0, read);
                } else if (read == 0) {
                    Thread.yield();
                }
            }
        } catch (SocketTimeoutException e) {
            throw new PrinterConnectionException(e, "Read timeout from printer %s:%d", ip, port);
        } catch (IOException e) {
            throw new PrinterConnectionException(e, "Error reading from printer %s:%d", ip, port);
        }

        return out.toByteArray();
    }

    /**
     * Read response as a String using the configured charset. Returns an empty
     * string if no bytes were read.
     *
     * @throws PrinterConnectionException on read errors
     */
    public String readAsString() throws PrinterConnectionException {
        final byte[] bytes = readAsBytes();
        if (bytes == null) return null;
        if (bytes.length == 0) return "";
        return new String(bytes, charset);
    }

    /**
     * Read socket input stream into packets (strings).
     * A packet normally starts with [STX] = 0x02 and ends with [ETX] = 0x03.
     * <p>
     * Behavior:
     * - If input bytes arrive without an initial STX, the bytes up to the next ETX or STX (or stream end) are returned as a packet.
     * - If an ETX is missing, the bytes accumulated before the next STX or stream end are returned as a packet.
     * - Consecutive STX will start a new packet; any accumulated bytes are flushed as a packet.
     * <p>
     * Examples:
     * [2,80,81,82,83,3] -> single packet (including STX and ETX)
     * [2,80,81,82,3,2,83,3] -> two packets = [2,80,81,82,3] + [2,83,3]
     * [80,81,82,3,2,83,3] -> two packets (first has no STX) = [80,81,82,3] + [2,83,3]
     * [90,91,92,93] -> one packet (no STX/ETX)
     *
     * @return a list of packet strings using the configured charset. Returns an empty list if no bytes were read.
     * @throws PrinterConnectionException if input stream is not initialized or an IO error occurs (including socket timeouts).
     */
    public List<RawPacket> readAsPackets() throws PrinterConnectionException {
        if (_input == null) {
            throw new PrinterConnectionException("Printer input stream is not initialized. Are you connected?");
        }

        final List<RawPacket> packets = new ArrayList<>();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        try {
            while ((b = _input.read()) != EOF) {
                if (b == STX) {
                    // flush accumulated data as a packet before start a new packet
                    if (out.size() > 0) {
                        final RawPacket packet = flush(out);
                        packets.add(packet);
                    }

                    // start a new packet
                    out.write(b);
                } else if (b == ETX) {
                    // end of a packet, flush it including ETX (even if it had no STX)
                    out.write(b);
                    final RawPacket packet = flush(out);
                    packets.add(packet);
                } else {
                    // append byte to the current packet
                    out.write(b);
                }
            }

            // if EOF is reached and there is remaining data, treat it as a final packet.
            if (out.size() > 0) {
                final RawPacket packet = flush(out);
                packets.add(packet);
            }
        } catch (SocketTimeoutException e) {
            throw new PrinterConnectionException(e, "Read timeout from printer %s:%d", ip, port);
        } catch (IOException e) {
            throw new PrinterConnectionException(e, "Error reading from printer %s:%d", ip, port);
        }

        return packets;
    }

    private RawPacket flush(final ByteArrayOutputStream out) {
        final byte[] bytes = out.toByteArray();
        final RawPacket packet = new RawPacket(bytes, charset);
        out.reset();
        return packet;
    }

}

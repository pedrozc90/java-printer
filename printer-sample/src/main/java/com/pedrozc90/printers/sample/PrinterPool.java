package com.pedrozc90.printers.sample;

import com.pedrozc90.printers.core.Printer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple singleton pool that manages Printer instances keyed by an IP/port pair.
 * <p>
 * The pool stores and returns existing `Printer` objects for the same connection
 * coordinates. Callers should close or drop printers via the pool APIs when
 * they are no longer needed. The `getInstance()` method is thread-safe using
 * the initialization-on-demand holder idiom.
 */
public class PrinterPool implements AutoCloseable {

    private static PrinterPool instance;

    private final Logger logger = Logger.getLogger(PrinterPool.class);
    private final PrinterFactory factory = PrinterFactory.getInstance();
    private final Map<Pair, Printer> _pool = new ConcurrentHashMap<>();

    public static PrinterPool getInstance() {
        if (instance == null) {
            synchronized (PrinterPool.class) {
                if (instance == null) {
                    instance = new PrinterPool();
                }
            }
        }
        return instance;
    }

    /**
     * Returns an unmodifiable view of the internal pool map.
     * <p>
     * The returned map is a read-only snapshot view; attempts to modify it will
     * throw an {@link UnsupportedOperationException}.
     *
     * @return unmodifiable map of `Pair` to `Printer`
     */
    public Map<Pair, Printer> getPool() {
        return Collections.unmodifiableMap(_pool);
    }

    /**
     * Current pool size.
     */
    public int getPoolSize() {
        return _pool.size();
    }

    /**
     * Create or return an existing printer for the given connection coordinates.
     * <p>
     * If a `Printer` is already present in the pool for the provided `ip` and
     * `port`, that instance is returned. Otherwise a new `Printer` is created by
     * the {@link PrinterFactory} and stored in the pool.
     *
     * @param type printer type (for example, "SATO", "ZEBRA" or "AVERY_DENNISON")
     * @param ip   printer IP address
     * @param port printer port
     * @return existing or newly-created `Printer` instance
     */
    public Printer create(final String type, final String ip, final Integer port) {
        final Pair key = new Pair(ip, port);
        return _pool.computeIfAbsent(key, k -> {
            Printer created = factory.create(type, ip, port);
            logger.infof("Created printer of type %s for %s:%s", type, ip, port);
            return created;
        });
    }

    /**
     * Retrieve a printer from the pool by IP and port.
     *
     * @param ip   printer IP address
     * @param port printer port
     * @return the `Printer` instance if present, otherwise `null`
     */
    public Printer get(final String ip, final Integer port) {
        final Pair key = new Pair(ip, port);
        return _pool.get(key);
    }

    // INTERNALS

    /**
     * Check whether a printer for the given IP and port is stored in the pool.
     *
     * @param ip   printer IP address
     * @param port printer port
     * @return `true` if a matching printer is present, otherwise `false`
     */
    public boolean exists(final String ip, final Integer port) {
        final Pair key = new Pair(ip, port);
        return _pool.containsKey(key);
    }

    /**
     * Put a `Printer` into the pool for the specified IP and port.
     * <p>
     * Throws {@link IllegalArgumentException} when a printer is already stored
     * for the same coordinates.
     *
     * @param ip      printer IP address
     * @param port    printer port
     * @param printer printer instance to store
     * @throws IllegalArgumentException if a printer already exists for the key
     */
    public void put(final String ip, final Integer port, final Printer printer) {
        final Pair key = new Pair(ip, port);
        if (_pool.containsKey(key)) {
            throw new IllegalArgumentException("Printer already exists");
        }
        _pool.put(key, printer);
    }

    /**
     * Associates the given `Printer` with the provided `Pair` key.
     *
     * @param key     pair containing ip and port
     * @param printer printer instance to store
     * @return the previous `Printer` associated with the key, or `null` if none
     */
    public Printer put(final Pair key, final Printer printer) {
        return _pool.put(key, printer);
    }

    /**
     * Remove and close the printer associated with the given IP and port.
     * <p>
     * If no printer exists for the coordinates, the method returns silently.
     * The underlying printer's `close()` method may throw an exception which
     * is propagated to the caller.
     *
     * @param ip   printer IP address
     * @param port printer port
     * @throws Exception if closing the underlying `Printer` fails
     */
    public void drop(final String ip, final Integer port) throws Exception {
        final Pair key = new Pair(ip, port);
        final Printer printer = _pool.remove(key);
        if (printer == null) return;
        try {
            printer.close();
            logger.infof("Dropped printer for %s:%s", ip, port);
        } catch (Exception e) {
            logger.errorf(e, "Error closing printer for %s:%s", ip, port);
            throw e;
        }
    }

    /**
     * Close all managed printers and clear the pool.
     * <p>
     * Any exceptions raised while closing individual printers are logged and
     * do not prevent the pool from being cleared.
     */
    public void clear() {
        _pool.forEach((k, v) -> {
            try {
                v.close();
            } catch (Exception e) {
                logger.errorf(e, "Error closing printer: %s", v);
            }
        });
        _pool.clear();
    }

    @Override
    public void close() {
        clear();
    }

    /**
     * Simple immutable key object representing an IP (`key`) and port (`value`).
     * <p>
     * Naming follows the original implementation (fields named `key` and
     * `value`) where `key` holds the IP address and `value` holds the port.
     */
    @Data
    @EqualsAndHashCode
    @ToString
    public static class Pair {

        private final String ip;
        private final Integer port;

    }

}

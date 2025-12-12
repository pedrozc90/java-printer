package com.contare.printers.core;

import com.contare.printers.core.exceptions.PrinterException;
import lombok.Data;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public abstract class BasePrinter implements Printer {

    protected final int MAX_ITERATIONS = 1_000_000; // to avoid infinite loops
    protected final int MAX_RECONNECTIONS = 5;      // maximum number of reconnection attempts
    protected final int READ_TIMEOUT = 15_500;      // maximum time with not response from socket

    private final Set<String> _skus = ConcurrentHashMap.newKeySet();  // store cancelled skus

    protected final PrinterConnection connection;
    protected final Logger logger;

    protected String sku;       // store current printing sku
    protected boolean paused;   // track if the current printing is pause.
    protected boolean printing; // track if the current printing is ongoing.
    protected boolean abort;    // track if print is aborted, meaning it is to skip all next 'content' received by the 'print' method.

    public BasePrinter(final String ip, final Integer port) {
        this.connection = new PrinterConnection(ip, port);
        this.logger = Logger.getLogger(getClass());
    }

    // ACTIONS
    @Override
    public void init() {
        logger.debugf("[%s] Initializing printer...", tag());
        setAbort(false);
        setPaused(false);
        setPrinting(false);
        clearSkus();
    }

    @Override
    public void connect() throws PrinterException {
        try {
            connection.connect();
        } catch (IOException e) {
            throw new PrinterException(e, "Error to connect to the printer");
        }
    }

    @Override
    public void reconnect() throws PrinterException {
        try {
            connection.reconnect();
        } catch (IOException e) {
            throw new PrinterException(e, "Error to reconnect to the printer");
        }
    }

    @Override
    public void send(final String value) throws PrinterException {
        try {
            connection.send(value);
        } catch (IOException e) {
            throw new PrinterException(e, "Error sending data to the printer");
        }
    }

    @Override
    public void close() throws PrinterException {
        try {
            connection.close();
        } catch (IOException e) {
            throw new PrinterException(e, "Error closing printer connection");
        }
    }

    // SKUS

    /**
     * Returns true if the SKU is already marked to be ignored.
     */
    public boolean isSkuIgnored(final String sku) {
        return sku != null && _skus.contains(sku);
    }

    /**
     * Marks a SKU so future print calls for that SKU are skipped.
     * This will register the SKU if it was not present.
     */
    public boolean markSkuToSkip(final String sku) {
        if (sku == null) return false;
        return _skus.add(sku);
    }

    /**
     * Clears all SKU registrations/marks.
     */
    public void clearSkus() {
        _skus.clear();
    }

}

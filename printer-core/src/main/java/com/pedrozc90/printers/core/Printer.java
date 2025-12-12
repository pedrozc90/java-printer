package com.pedrozc90.printers.core;

import com.pedrozc90.printers.core.exceptions.PrinterException;

import java.util.Set;

/**
 * High-level printer abstraction for sending labels and controlling the device.
 * <p>
 * Implementations encapsulate connection handling, driver initialization and
 * device-specific command/response behavior. Methods throw {@link PrinterException}
 * for recoverable or fatal errors that callers should handle (connectivity,
 * protocol, or device errors).
 */
public interface Printer extends AutoCloseable {

    String tag();

    /**
     * Send label content like SBPL or ZPL to the printer.
     * Starts a loop process to request and collect printed EPCs, then return it.
     *
     * @param content - label payload or command stream to be printed (driver-dependent format)
     * @param sku     - identifier for the current SKU being printed (may be used for logging/tracking)
     * @param epcs    - expected number of EPCs encoded/printed by this job (may be null if unknown)
     * @return set of EPC strings that the printer actually printed (may be empty if none)
     * @throws PrinterException on communication, protocol, or device errors while printing
     */
    Set<String> print(final String content, final String sku, final Integer epcs) throws PrinterException;

    /**
     * Hook called when the printer returns a new EPC/TID.
     *
     * @param epc - rfid hexadecimal string.
     * @param tid - tid string.
     */
    void onReceiveEpc(final String epc, final String tid);

    /**
     * Hook called when the printer status changes.
     *
     * @param obj - printer response object.
     */
    void onUpdateStatus(final Object obj);

    /**
     * Initialize the printer driver and any internal resources.
     * <p>
     * Implementations should prepare the printer for subsequent connect/print
     * operations (load templates, validate configuration, etc.).
     */
    void init();

    /**
     * Establish a connection to the physical printer.
     *
     * @throws PrinterException if the printer cannot be reached or initialization of the
     *                          connection fails.
     */
    void connect() throws PrinterException;

    /**
     * Re-establish a connection to the printer. Implementations may close any
     * existing connection and open a fresh one.
     *
     * @throws PrinterException if reconnect fails.
     */
    void reconnect() throws PrinterException;

    /**
     * Send to the printer a string payload.
     *
     * @param value - payload string
     * @throws PrinterException if a connection error happens.
     */
    void send(final String value) throws PrinterException;

    /**
     * Send the command to start or resume printing.
     *
     * @return true if printer ACKed the command, false otherwise.
     * @throws PrinterException if the command cannot be sent or the device reports an error.
     */
    boolean play() throws PrinterException;

    /**
     * Send the command to pause the current print job.
     *
     * @return true if printer ACKed the command, false otherwise.
     * @throws PrinterException if the command cannot be sent or the device reports an error.
     */
    boolean pause() throws PrinterException;

    /**
     * Cancel the current print job on the device.
     *
     * @return true if printer ACKed the command, false otherwise.
     * @throws PrinterException if the cancel command fails or the device reports an error.
     */
    boolean cancel() throws PrinterException;

    /**
     * Cancel printing for the current SKU context only (implementation-defined
     * behavior).
     *
     * @return true if printer ACKed the command, false otherwise.
     * @throws PrinterException if the operation fails.
     */
    boolean cancelSku() throws PrinterException;

}

package com.pedrozc90.printers.sato;

import com.pedrozc90.printers.core.BasePrinter;
import com.pedrozc90.printers.core.exceptions.PrinterException;
import com.pedrozc90.printers.sato.enums.PrinterStatus;
import com.pedrozc90.printers.sato.objects.SatoMessage;
import com.pedrozc90.printers.sato.parser.SatoParser;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SatoPrinter extends BasePrinter {

    private static final long MIN_PS0_RECEIVED = 8;

    private final SatoParser parser = SatoParser.getInstance();

    public SatoPrinter(final String ip, final Integer port) {
        super(ip, port);
    }

    @Override
    public String tag() {
        return "sato@" + connection.address();
    }

    @Override
    public Set<String> print(final String content, final String sku, final Integer epcs) throws PrinterException {
        Objects.requireNonNull(content, "SBPL content must not be null");

        final Set<String> out = new HashSet<>();

        final String tag = tag();

        // normalize label file content
        // replace LFs not already preceded by CR (LF -> CRLF)
        final String normalized = normalize(content);

        // set track sku
        setSku(sku);

        try {
            if (abort) {
                logger.warnf("[%s] Aborting printing of sku '%s'", tag, sku);
                return out;
            }

            // check if sku is already registered
            if (isSkuIgnored(sku)) {
                logger.infof("[%s] Skipping sku '%s'", tag, sku);
                return out;
            }

            // make sure we connected to the printer
            reconnect();

            // stop previous printing, if still ongoing,
            // clear the printer's buffer to avoid PG + PK command to return tags from previous printing.
            final boolean canceled = queryCancel();
            if (!canceled) {
                throw new PrinterException("Failed to cancel previous printing job.");
            }

            printing = true;

            logger.infof("[%s] Sku: '%s'", tag, sku);
            logger.infof("[%s] Number of EPCs: %d", tag, epcs);
            logger.infof("[%s] ------------------------------------------------------------", tag);
            logger.infof("[%s] -- SBPL", tag);
            logger.infof("[%s] ------------------------------------------------------------", tag);
            logger.info(normalized);
            logger.infof("[%s] ------------------------------------------------------------", tag);

            // send SBPL to the printer
            send(normalized);

            // wait a little since se send a big file to the printer
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.errorf(e, "[%s] Error while sleeping", tag);
            }

            SatoMessage.PrinterInfo prev = null;    // store the last printer status message received

            int it = 0;                             // count the number of iterations
            long elapsed = 0;                       // time taken between iterations
            int remaining = Integer.MAX_VALUE;      // remaining number of tags to print (equal to printer status Q parameter)
            long ps0Received = 0;   // count the number of PS0 received

            long start = System.currentTimeMillis();

            final int MAX_COUNTER = 3;
            int stableCount = 0;

            mainLoop:
            while ((elapsed = System.currentTimeMillis() - start) < READ_TIMEOUT && remaining > 0 && printing) {
                logger.infof("[%s] Socket iteration '%d' (%d ms)", tag, it, elapsed);

                // request printer status and EPC/TID
                final List<SatoMessage> messages = queryStatusAndTags();
                logger.debugf("Socket received '%d' messages", messages.size());

                // update the last-received timestamp so timeout is relative to the last activity
                start = System.currentTimeMillis();

                for (SatoMessage m : messages) {
                    // check printer status
                    if (m instanceof SatoMessage.PrinterInfo) {
                        final SatoMessage.PrinterInfo obj = (SatoMessage.PrinterInfo) m;
                        logger.infof("Printer status: %s", obj);

                        if (!Objects.equals(obj, prev)) {
                            remaining = obj.getQ();
                            onUpdateStatus(obj);
                        }

                        if (remaining == 0 && obj.getPs() == PrinterStatus.STANDBY) {
                            stableCount++;
                            logger.debugf("Printer status is STANDBY, count: %d", stableCount);
                            if (stableCount >= (MAX_COUNTER - 1)) {
                                printing = false;
                                break mainLoop;
                            }
                        } else {
                            stableCount = 0;
                        }

                        prev = obj;
                    }
                    // collect epc
                    else if (m instanceof SatoMessage.TagInfo) {
                        final SatoMessage.TagInfo obj = (SatoMessage.TagInfo) m;
                        logger.infof("Tag info: %s", obj);

                        final String epc = obj.getEpc();
                        final String tid = obj.getTid();
                        if (epc != null && out.add(epc)) {
                            onReceiveEpc(epc, tid);
                        }
                    }
                    // command failed
                    else if (m instanceof SatoMessage.Nak) {
                        logger.warn("Failed to query printer status and EPC");
                        // break _loop;
                    }
                    // unknown
                    else {
                        logger.warnf("Unexpected message type received: '%s'", m);
                    }
                }

                it++;
            }
        } finally {
            // print finished
            printing = false;

            try {
                // cancel printing
                // if the loop broke because of an error, the printer will continue anyway, so let's try to force it to stop
                cancel();
            } catch (PrinterException e) {
                logger.error("[%s] Error cancelling printing job", tag, e);
            }

            try {
                // close printer connection
                close();
            } catch (PrinterException e) {
                logger.error("[%s] Error closing printer connection", tag, e);
            }
        }

        return out;
    }

    // CALLBACKS
    @Override
    public void onReceiveEpc(final String epc, final String tid) {
        logger.debugf("[%s] Received EPC: '%s', TID: '%s'", tag(), epc, tid);
    }

    @Override
    public void onUpdateStatus(final Object obj) {
        final SatoMessage.PrinterInfo information = (SatoMessage.PrinterInfo) obj;
        logger.debugf("[%s] Printer status changed: %s", tag(), information);
    }

    // ACTIONS
    @Override
    public boolean play() throws PrinterException {
        final boolean resumed = queryResume();
        if (resumed) {
            logger.infof("[%s] Printing resumed.", tag());
        } else {
            logger.errorf("[%s] Failed to resume printing.", tag());
        }
        return resumed;
    }

    @Override
    public boolean pause() throws PrinterException {
        final boolean paused = queryPause();
        if (paused) {
            logger.infof("[%s] Printing paused.", tag());
        } else {
            logger.errorf("[%s] Failed to pause printing.", tag());
        }
        return paused;
    }

    @Override
    public boolean cancel() throws PrinterException {
        final boolean cancelled = queryCancel();
        if (cancelled) {
            setPrinting(false);
            setAbort(true);
            logger.infof("[%s] Printing cancelled.", tag());
        } else {
            logger.errorf("[%s] Failed to cancel printing.", tag());
        }
        return cancelled;
    }

    @Override
    public boolean cancelSku() throws PrinterException {
        final boolean canceled = queryCancel();
        if (canceled) {
            final String sku = getSku();
            markSkuToSkip(sku);
            logger.infof("[%s] Canceled printing of sku '%s'.", tag(), sku);
        } else {
            logger.errorf("[%s] Failed to cancel printing.", tag());
        }
        return canceled;
    }

    // COMMANDS

    /**
     * Command used to request printer status and tag EPC/TID
     *
     * @throws PrinterException
     */
    protected List<SatoMessage> queryStatusAndTags() throws PrinterException {
        // DC2 + PG = command returns the printer status. (requires PK command to return, pg. 435)
        // DC2 + PK = command returns the status of RFID tag write by <IP0> command and EPC/TID. (pg. 444, 451)
        final String cmd = "\u0002\u0012PG\u0012PK\u0003";
        return sendCommandAndWait(cmd, 1_000);
    }

    /**
     * <DC2 + PG>: Command used to request printer status information.
     * <p>
     * obs.:
     * 1. Return data format <[STX]a...a,b...bc,d...de,...[ETX]> (e.g: [STX]32,PS0,RS0,RE0,PE0,EN00,BT0,Q000000[ETX])
     * 2. Return data <NAK> when a command error occurs
     *
     * @throws PrinterException
     */
    protected SatoMessage.PrinterInfo queryPrinterStatus() throws PrinterException {
        // DC2 + PG = command returns the printer status. (requires PK command to return, pg. 435)
        final String cmd = "\u0002\u0012PG\u0003";
        final List<SatoMessage> messages = this.sendCommandAndWait(cmd, 1_000);
        for (SatoMessage m : messages) {
            if (m instanceof SatoMessage.PrinterInfo) {
                return (SatoMessage.PrinterInfo) m;
            }
        }
        return null;
    }

    /**
     * <DC2 + PK>: Command used to request status of RFID tag write by <IP0> command and EPC/TID
     * <p>
     * obs.:
     * 1. Return data format <[STX]a...a,b,c,d...d[ETX]> (e.g: EP:E0123456789ABCDEF0123456,ID:E200680612345678)
     * 2. Return data <NAK> when a command error occurs
     *
     * @throws PrinterException
     */
    protected SatoMessage.TagInfo queryEPCAndTID() throws PrinterException {
        // DC2 + PK = command returns the status of RFID tag write by <IP0> command and EPC/TID. (pg. 444, 451)
        final String cmd = "\u0002\u0012PK\u0003";
        final List<SatoMessage> messages = sendCommandAndWait(cmd, 1_000);
        for (SatoMessage m : messages) {
            if (m instanceof SatoMessage.TagInfo) {
                return (SatoMessage.TagInfo) m;
            }
        }
        return null;
    }

    /**
     * <DC1 + H>: Command used to resume printing.
     * <p>
     * obs.:
     * 1. Return <ACK> (HEX 06H) - No error in the printer
     * 2. Return <NAK> (HEX 15H) - Error in the printer
     *
     * @throws PrinterException
     */
    protected boolean queryResume() throws PrinterException {
        // DC1 + H = command starts printing. (pg. 495)
        final String cmd = "\u0002\u0011H\u0003";
        return sendControlCommand(cmd, 500);
    }

    /**
     * <DLE + H>: Command used to pause print printing.
     * <p>
     * obs.:
     * 1. Return <ACK> (HEX 06H) - No error in the printer
     * 2. Return <NAK> (HEX 15H) - Error in the printer
     *
     * @throws PrinterException
     */
    protected boolean queryPause() throws PrinterException {
        // DLE + H = command pause printing. (pg. 495)
        final String cmd = "\u0002\u0010H\u0003";
        return sendControlCommand(cmd, 500);
    }

    /**
     * <DC2 + PH>: Cancel used to cancel print jobs and clear printer buffer.
     * <p>
     * obs.:
     * 1. Return <ACK> - Ok
     * 2. Return <NAK> - Error
     *
     * @throws PrinterException -- when socket connection is lost.
     */
    protected boolean queryCancel() throws PrinterException {
        // DC2 + PH = This command cancels print jobs and clears the entire contents of receive buffer. (pg. 438)
        final String cmd = "\u0002\u0012PH\u0003";
        return this.sendControlCommand(cmd, 1_000);
    }

    /**
     * <DC2 + DC>: Command used to restart the printer.
     * <p>
     * obs.:
     * 1. Printer response with <NAK> (only during printing)
     *
     * @return
     * @throws PrinterException
     */
    protected boolean queryReset() throws PrinterException {
        // DC2 + DC = This command resets the printer to its default state. (pg. 400)
        final String cmd = "\u0002\u0012DC\u0003";
        return sendControlCommand(cmd, 500);
    }

    /**
     * <DC2 + DD>: Command used to turn off the printer.
     * <p>
     * obs.:
     * 1. Printer response with <NAK> (only during printing)
     *
     * @return
     * @throws PrinterException
     */
    protected boolean queryPowerOff() throws PrinterException {
        // DC2 + DD = This command powers off the printer. (pg. 400)
        final String cmd = "\u0002\u0012DD\u0003";
        return sendControlCommand(cmd, 500);
    }

    // HELPERS

    /**
     * Send command and block until we receive an ACK/NAK or a framed response, or timeout.
     *
     * @param cmd     - printer command string.
     * @param timeout - response timeout (milliseconds)
     * @return parsed SatoMessage list, it may be empty on timeout.
     * @throws PrinterException
     */
    protected List<SatoMessage> sendCommandAndWait(final String cmd, final long timeout) throws PrinterException {
        return this.sendCommandAndWait(
            cmd,
            timeout,
            (p) -> parser.parse(p),
            (messages) -> messages.stream().anyMatch((m) ->
                m instanceof SatoMessage.Ack
                    || m instanceof SatoMessage.Nak
                    || m instanceof SatoMessage.PrinterInfo
                    || m instanceof SatoMessage.TagInfo
            )
        );
    }

    /**
     * Send control command and return true on ACK, false on NAK / timeout.
     *
     * @param cmd     -- printer command string.
     * @param timeout -- response timeout
     * @return
     * @throws PrinterException
     */
    protected boolean sendControlCommand(final String cmd, final long timeout) throws PrinterException {
        final List<SatoMessage> messages = sendCommandAndWait(cmd, timeout);
        for (SatoMessage m : messages) {
            if (m instanceof SatoMessage.Ack) {
                return true;
            } else if (m instanceof SatoMessage.Nak) {
                return false;
            }
        }
        return false;
    }

    // HELPERS
    private String normalize(final String value) {
        final long start = System.currentTimeMillis();
        final String normalized = value.replaceAll("(?<!\\r)\\n", "\r\n");
        logger.tracef("[%s] Normalize SBPl (%d) ms", tag(), System.currentTimeMillis() - start);
        return normalized;
    }

}

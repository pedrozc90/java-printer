package com.pedrozc90.printers.zebra;

import com.pedrozc90.printers.core.BasePrinter;
import com.pedrozc90.printers.core.exceptions.PrinterException;
import com.pedrozc90.printers.zebra.enums.RFIDOperation;
import com.pedrozc90.printers.zebra.objects.ZebraMessage;
import com.pedrozc90.printers.zebra.parser.ZebraParser;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ZebraPrinter extends BasePrinter {

    private static final long MIN_START_END_RECEIVED = 8;

    private final ZebraParser parser = ZebraParser.getInstance();

    public ZebraPrinter(final String ip, final Integer port) {
        super(ip, port);
    }

    @Override
    public String tag() {
        return "zebra@" + connection.address();
    }

    @Override
    public Set<String> print(final String content, final String sku, final Integer n) throws PrinterException {
        final Set<String> out = new HashSet<>();

        final String tag = tag();

        setSku(sku);

        try {
            // cancelar a impressao de todos os sku de uma impressão
            if (abort) {
                logger.infof("[%s] aborting printing sku '%s'", tag, sku);
                return out;
            }

            // if sku is mark as TRUE, then we should skip it
            if (isSkuIgnored(sku)) {
                logger.infof("[%s] skipping sku '%s'", tag, sku);
                return out;
            }

            // make sure we are connected to the printer
            reconnect();

            printing = true;

            // clear printer buffer
            final boolean canceled = queryCancelAll();
            if (!canceled) {
                throw new PrinterException("Failed to cancel previous printing job.");
            }

            // if the printer is on 'Pause Mode', unpause it
            play();

            logger.infof("[%s] Sku: '%s'", tag, sku);
            logger.infof("[%s] Number of EPCs: %d", tag, n);
            logger.infof("[%s] ------------------------------------------------------------", tag);
            logger.infof("[%s] -- ZPL", tag);
            logger.infof("[%s] ------------------------------------------------------------", tag);
            logger.info(content);
            logger.infof("[%s] ------------------------------------------------------------", tag);

            // send ZPL to printer
            send(content);

            // wait a little since se send a big file to the printer
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.errorf(e, "[%s] Thread sleep interrupted", tag);
            }

            ZebraMessage.RFIDData prev = null;      // store last data received

            int it = 0;
            long elapsed = 0;
            long start = System.currentTimeMillis();
            long startEndReceived = 0;              // count the number of empty payloads, e.g.: "<start><end>"

            mainLoop:
            while ((elapsed = System.currentTimeMillis() - start) < READ_TIMEOUT && printing) {
                logger.infof("[%s] Socket iteration: '%d' (%d ms)", tag, it, elapsed);

                // request RFID data
                final List<ZebraMessage> messages = requestRFIDData();
                logger.debugf("[%s] Socket received '%d' messages", tag, messages.size());

                start = System.currentTimeMillis();

                for (ZebraMessage m : messages) {
                    if (m instanceof ZebraMessage.RFIDData) {
                        final ZebraMessage.RFIDData obj = (ZebraMessage.RFIDData) m;
                        logger.infof("[%s] RFID Data: %s", tag, obj);

                        if (!Objects.equals(obj, prev)) {
                            onUpdateStatus(obj);
                            prev = obj;
                        }

                        if (obj.getOperation() == RFIDOperation.WRITE) {
                            final String epc = obj.getData();
                            if (epc != null && out.add(epc)) {
                                onReceiveEpc(epc, null);
                            }
                        }
                    } else if (m instanceof ZebraMessage.Empty) {
                        startEndReceived++;
                        if (startEndReceived > MIN_START_END_RECEIVED) {
                            break mainLoop;
                        }
                    } else {
                        logger.debugf("[%s] Socket received message: %s", tag, m);
                    }
                }

                it++;

                if (it > MAX_ITERATIONS) {
                    logger.errorf("[%s] Infinite loop", tag);
                    break;
                }
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
        final ZebraMessage.RFIDData data = (ZebraMessage.RFIDData) obj;
        logger.debugf("[%s] Printer status changed: %s", tag(), data);
    }

    // ACTIONS
    @Override
    public boolean play() throws PrinterException {
        final boolean started = queryStart();
        if (started) {
            logger.debugf("[%s] Printing started", tag());
            setPaused(false);
        } else {
            logger.errorf("[%s] Printing not started", tag());
        }
        return started;
    }

    @Override
    public boolean pause() throws PrinterException {
        final boolean paused = queryPause();
        if (paused) {
            logger.debugf("[%s] Printing paused", tag());
            setPaused(true);
        } else {
            logger.errorf("[%s] Printing not paused", tag());
        }
        return paused;
    }

    @Override
    public boolean cancel() throws PrinterException {
        final boolean canceled = queryCancelAll();
        if (canceled) {
            logger.debugf("[%s] Printing canceled", tag());
            this.printing = false;
        } else {
            logger.errorf("[%s] Printing not canceled", tag());
        }
        return canceled;
    }

    @Override
    public boolean cancelSku() throws PrinterException {
        final boolean canceled = queryCancelAll();
        if (canceled) {
            final String sku = getSku();
            markSkuToSkip(sku);
            logger.debugf("[%s] Canceling printing of sku '%s'.", tag(), sku);
            setPrinting(false);
            setAbort(true);
        } else {
            logger.errorf("[%s] Printing not canceled", tag());
        }
        return canceled;
    }

    // COMMANDS
    private void sendHL() throws PrinterException {
        final String cmd = "~HL";
        try {
            connection.reconnect();
            logger.infof("Socket send: '%s'", cmd);
            connection.send(cmd);
        } catch (Exception e) {
            throw new PrinterException(e, "Error sending %s", cmd);
        }
    }

    /**
     * Request that the RFID data lob be returned to the host.
     *
     * @return
     * @throws PrinterException
     */
    protected List<ZebraMessage> requestRFIDData() throws PrinterException {
        // final String cmd = "~HL";    // the command is processed immediately. do not automatically clear data log.
        final String cmd = "^XA^HL^XZ"; // ^HL command clears the current data log and restarts data recording.
        final List<ZebraMessage> messages = sendCommandAndWait(cmd, 500);
//        for (ZebraMessage m : messages) {
//            if (m instanceof ZebraMessage.RFIDData) {
//                return (ZebraMessage.RFIDData) m;
//            }
//        }
        return messages;
    }

    /**
     * The command causes a printer in 'Pause Mode' to resume printing. (pg. 310)
     *
     * @return true if command was successful, otherwise return false.
     * @throws PrinterException
     */
    protected boolean queryStart() throws PrinterException {
        final String cmd = "~PS";
        return sendControlCommand(cmd, 1_000);
    }

    /**
     * The command stops printing after the current label is complete and places the printer in 'Pause Mode'. (pg. 305)
     *
     * @return true if command was successful, otherwise return false.
     * @throws PrinterException
     */
    protected boolean queryPause() throws PrinterException {
        final String cmd = "~PP";
        return sendControlCommand(cmd, 1_000);
    }

    /**
     * Command cancels all format commands in the buffer. It also cancels any batches that are printing. (pg. 229)
     *
     * @throws PrinterException
     */
    protected boolean queryCancelAll() throws PrinterException {
        final String cmd = "~JA";
        return sendControlCommand(cmd, 1_000);
    }

    // HELPER
    protected List<ZebraMessage> sendCommandAndWait(final String cmd, final long timeout) throws PrinterException {
        return sendCommandAndWait(
            cmd,
            timeout,
            (p) -> parser.parse(p),
            null
        );
    }

    protected boolean sendControlCommand(final String cmd, final long timeout) throws PrinterException {
        final List<ZebraMessage> messages = sendCommandAndWait(cmd, timeout);
        for (ZebraMessage message : messages) {
            logger.debugf("Checking message: %s", message);
        }
        return false;
    }

}

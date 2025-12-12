package com.contare.printers.zebra;

import com.contare.printers.core.BasePrinter;
import com.contare.printers.core.exceptions.PrinterException;
import com.contare.printers.zebra.enums.RFIDOperation;
import com.contare.printers.zebra.objects.ZebraPrinterInformation;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ZebraPrinter extends BasePrinter {

    private static final long MIN_START_END_RECEIVED = 8;

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

            // cancel all printer jobs and clear the printer's buffer
            queryCancel();

            // if the printer is on 'Pause Mode', unpause it
            play();

            // request RFIDData and clear buffer (old and new firmware)
            requestRFIDDataAndClearBuffer();

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

            ZebraPrinterInformation prev = null;    // last data received

            int it = 0;
            long elapsed = 0;
            long startEndReceived = 0;              // count the number of empty payloads, e.g.: "<start><end>"

            // request RFIDData
            requestRFIDData();

            long start = System.currentTimeMillis();

            mainLoop:
            while ((elapsed = System.currentTimeMillis() - start) < READ_TIMEOUT && printing) {
                logger.infof("[%s] Socket iteration: '%d' (%d ms)", tag, it, elapsed);

                try {
                    final String read = connection.readAsString();
                    logger.infof("[%s] Socket read: '%s'", tag, read);

                    final ZebraPrinterInformation information = ZebraPrinterInformation.parse(read);
                    logger.debugf("[%s] %s", tag, information);

                    if (information.getOperationStatusList().contains(RFIDOperation.WRITE)) {
                        final List<String> epcs = information.getEpcs();
                        if (epcs != null && !epcs.isEmpty() && out.addAll(epcs)) {
                            for (String epc : epcs) {
                                onReceiveEpc(epc);
                            }
                            epcs.clear();
                        }
                    }

                    // if we already received all the expected EPCs, finish printing
                    if (n != 0 && out.size() == n) {
                        printing = false;
                        logger.infof("[%s] Printing completed", tag);
                    }

                    if (!paused && information.isStartEndReceived()) {
                        if (startEndReceived >= MIN_START_END_RECEIVED) {
                            printing = false;
                            logger.infof("[%s] Printing completed", tag);
                        }
                        logger.infof("[%s] Socket Start End received: %d", tag, startEndReceived);
                        startEndReceived++;
                    } else {
                        startEndReceived = 0;
                    }

                    if (prev == null || !Objects.equals(information.getData().getStatus(), prev.getData().getStatus())) {
                        onUpdateStatus(information);
                    }

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        logger.errorf(e, "[%s] Thread sleep interrupted", tag);
                    }

                    requestRFIDData();

                    start = System.currentTimeMillis();

                    if (it > MAX_ITERATIONS) {
                        logger.errorf("[%s] Infinite loop", tag);
                        break;
                    }

                    prev = information;
                } catch (IOException e) {
                    logger.errorf(e, "[%s] Socket error", tag);
                } catch (PrinterException e) {
                    logger.errorf(e, "[%s] Printer error", tag);

                    final Throwable cause = e.getCause();
                    if (cause instanceof SocketException) {
                        for (int retry = 0; retry < MAX_RECONNECTIONS; retry++) {
                            logger.infof("[%s] Printer socket reconnecting retry %d/%d", tag, (retry + 1), MAX_RECONNECTIONS);
                            reconnect();
                            if (connection.isConnected()) {
                                continue mainLoop;
                            }
                        }
                    }

                    throw e;
                } finally {
                    it++;
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
    public void onReceiveEpc(final String epc) {
        logger.debugf("[%s] Received EPC: '%s'", tag(), epc);
    }

    @Override
    public void onUpdateStatus(final Object obj) {
        final ZebraPrinterInformation information = (ZebraPrinterInformation) obj;
        logger.debugf("[%s] Printer status changed: %s", tag(), information);
    }

    // ACTIONS
    @Override
    public void play() throws PrinterException {
        queryPrintStart();
        paused = false;
    }

    @Override
    public void pause() throws PrinterException {
        queryPrintPause();
        paused = true;
    }

    @Override
    public void cancel() throws PrinterException {
        cancel(false, false);
    }

    @Override
    public void cancelAll() throws PrinterException {
        cancel(true, false);
    }

    @Override
    public void cancelSku() throws PrinterException {
        cancel(false, true);
    }

    private void cancel(final boolean abort, final boolean skip) throws PrinterException {
        reconnect();

        printing = false;
        this.abort = abort;
        if (skip) {
            markSkuToSkip(sku);
        }

        logger.infof("[%s] Socket send - cancel printing: ~JA", tag());

        queryCancel();

        close();
    }

    // COMMANDS
    private void queryPrintStart() throws PrinterException {
        final String cmd = "~PS";
        send(cmd);
    }

    private void queryPrintPause() throws PrinterException {
        final String cmd = "~PP";
        send(cmd);
    }

    private void queryCancel() throws PrinterException {
        final String cmd = "~JA";
        reconnect();
        logger.infof("[%s] Socket send: %s", tag(), cmd);
        send(cmd);
    }

    private void requestRFIDData() throws PrinterException {
        final String cmd = "~HL";
        reconnect();
        logger.infof("[%s] Socket send: %s", tag(), cmd);
        send(cmd);
    }

    private void requestRFIDDataAndClearBuffer() throws PrinterException {
        final String cmd = "^XA^HL^XZ";
        reconnect();
        logger.infof("[%s] Socket send: %s", tag(), cmd);
        send(cmd);
    }

}

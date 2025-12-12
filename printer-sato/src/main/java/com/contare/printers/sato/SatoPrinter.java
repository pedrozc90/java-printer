package com.contare.printers.sato;

import com.contare.printers.core.BasePrinter;
import com.contare.printers.core.exceptions.PrinterException;
import com.contare.printers.sato.enums.PrinterStatus;
import com.contare.printers.sato.objects.SatoPrinterInformation;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SatoPrinter extends BasePrinter {

    private static final long MIN_PS0_RECEIVED = 8;

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
            cancel();

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

            requestPrinterStatusAndEPC();

            // wait a little since se send a big file to the printer
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.errorf(e, "[%s] Error while sleeping", tag);
            }

            SatoPrinterInformation prev = null;
            int it = 0;
            long elapsed = 0;
            long ps0Received = 0;   // count the number of PS0 received

            long start = System.currentTimeMillis();

            mainLoop:
            while ((elapsed = System.currentTimeMillis() - start) < READ_TIMEOUT && printing) {
                logger.infof("[%s] Socket iteration '%d' (%d ms)", tag, it, elapsed);

                try {
                    final String read = connection.readAsString();
                    logger.infof("[%s] Socket read: '%s'", tag, read);

                    final SatoPrinterInformation information = SatoPrinterInformation.parse(read);
                    if (information != null) {
                        logger.infof("[%s] '%s'", tag, information);

                        final String epc = information.getEpc();
                        if (epc != null && out.add(epc)) {
                            onReceiveEpc(epc);
                        }

                        if (!Objects.equals(information, prev)) {
                            onUpdateStatus(information);
                        }

                        requestPrinterStatusAndEPC();

                        // verifica se a impressao está finalizada, pois veio um ,PS0, no status da impressora
                        if (information.getPrinterStatus() == PrinterStatus.STANDBY) {
                            // força o monitoramento a receber 10 status PS0 antes de encerrar o loop, pois as vezes a impressora demora um pouco para mudar o status
                            // nesse caso impressão de 1 ou 2 etiquetas podem ser sobrescritas pelo envio da próxima se o loop terminar rápido demaisø
                            if (ps0Received >= MIN_PS0_RECEIVED) {
                                printing = false;
                                logger.infof("[%s] Printing completed", tag);
                            }
                            logger.infof("[%s] Socket PS0 received: '%d'", tag, ps0Received);
                            ps0Received++;
                        }

                        start = System.currentTimeMillis();
                    } else {
                        // if we read nothing from the socket, send another PG + PK command until we reach max processing time
                        requestPrinterStatusAndEPC();
                    }

                    // small delay so we do not spam the printer
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        logger.errorf(e, "[%s] Error while sleeping", tag);
                    }

                    if (it > MAX_ITERATIONS) {
                        logger.errorf("[%s] Infinite loop", tag);
                        break;
                    }

                    prev = information;
                } catch (IOException e) {
                    logger.errorf(e, "[%s] Error reading from socket", tag);
                } catch (PrinterException e) {
                    logger.errorf(e, "[%s] Printer error", tag);

                    final Throwable cause = e.getCause();
                    if (cause instanceof SocketException) {
                        logger.errorf("[%s] Printer socket disconnected", tag);

                        for (int retry = 0; retry < MAX_RECONNECTIONS; retry++) {
                            logger.infof("[%s] Printer socket reconnecting retry %d/%d", tag, (retry + 1), MAX_RECONNECTIONS);
                            try {
                                connection.connect(3_000);
                                // volta para o loop principal (while)
                                continue mainLoop;
                            } catch (IOException e2) {
                                try {
                                    Thread.sleep(3_000);
                                } catch (InterruptedException ex) {
                                    logger.errorf(ex, "[%s] Error while sleeping", tag);
                                }
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
        final SatoPrinterInformation information = (SatoPrinterInformation) obj;
        logger.debugf("[%s] Printer status changed: %s", tag(), information);
    }

    // ACTIONS
    @Override
    public void play() throws PrinterException {
        reconnect();

        paused = false;

        for (int i = 0; i < 3; i++) {
            queryPlay();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.errorf(e, "[%s] Error while sleeping", tag());
            }
        }

        if (!printing) {
            close();
        }
    }

    @Override
    public void pause() throws PrinterException {
        reconnect();

        paused = true;

        for (int i = 0; i < 3; i++) {
            queryPause();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.errorf(e, "[%s] Error while sleeping", tag());
            }
        }

        if (!printing) {
            close();
        }
    }

    @Override
    public void cancel() throws PrinterException {
        cancel(false, false);
    }

    @Override
    public void cancelSku() throws PrinterException {
        cancel(false, true);
    }

    @Override
    public void cancelAll() throws PrinterException {
        cancel(true, false);
    }

    protected void cancel(boolean abort, boolean skip) throws PrinterException {
        // make sure we are connected to the printer
        reconnect();

        this.printing = false;
        this.abort = abort;
        if (skip) {
            markSkuToSkip(sku);
        }

        for (int i = 0; i < 5; i++) {
            queryCancel();
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                logger.errorf(e, "[%s] Error while sleeping", tag());
            }
        }

        close();
    }

    // COMMANDS
    protected void requestPrinterStatusAndEPC() throws PrinterException {
        // PG command returns the printer status. (requires PK command to return, pg. 435)
        // PK command returns the status of RFID tag write by <IP0> command and EPC/TID. (pg. 444, 451)
        final String cmd = "\u0002\u0012PG\u0012PK\u0003";
        reconnect();
        logger.infof("[%s] Socket send: u0002, u0012PG u0012PK u0003", tag());
        send(cmd);
    }

    protected void queryPlay() throws PrinterException {
        final String cmd = "\u0002\u0011H\u0003";
        logger.infof("[%s] Socket send: u0002, u0011H, u0003 (play printing)", tag());
        send(cmd);
    }

    protected void queryPause() throws PrinterException {
        final String cmd = "\u0002\u0010H\u0003";
        logger.infof("[%s] Socket send: u0002, u0010H, u0003 (pause printing)", tag());
        send(cmd);
    }

    protected void queryCancel() throws PrinterException {
        // This command cancels print jobs and clears the entire contents of receive buffer.
        final String cmd = "\u0002\u0012PH\u0003";
        logger.infof("[%s] Socket send: u0002, u0012PH, u0003 (cancel printing)", tag());
        send(cmd);
    }

    // HELPERS
    private String normalize(final String value) {
        final long start = System.currentTimeMillis();
        final String normalized = value.replaceAll("(?<!\\r)\\n", "\r\n");
        logger.tracef("[%s] Normalize SBPl (%d) ms", tag(), System.currentTimeMillis() - start);
        return normalized;
    }

}

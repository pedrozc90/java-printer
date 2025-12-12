package com.pedrozc90.printers.averydennison;

import com.pedrozc90.printers.core.BasePrinter;
import com.pedrozc90.printers.core.exceptions.PrinterException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AveryDennisonPrinter extends BasePrinter {

    public AveryDennisonPrinter(final String ip, final Integer port) {
        super(ip, port);
    }

    @Override
    public String tag() {
        return "avery-dennison@" + connection.address();
    }

    @Override
    public Set<String> print(final String content, final String sku, final Integer epcs) throws PrinterException {
        final Set<String> out = new HashSet<>();

        final String tag = tag();

        setSku(sku);

        try {
            // make sure we are connected to the printer
            reconnect();

            logger.infof("[%s] Sku: '%s'", tag, sku);
            logger.infof("[%s] Number of EPCs: %d", tag, epcs);
            logger.infof("[%s] ------------------------------------------------------------", tag);
            logger.infof("[%s] -- ZPL", tag);
            logger.infof("[%s] ------------------------------------------------------------", tag);
            logger.info(content);
            logger.infof("[%s] ------------------------------------------------------------", tag);

            // send payload to the printer
            send(content);

            int it = 0;
            long elapsed = 0;

            long start = System.currentTimeMillis();

            while ((elapsed = System.currentTimeMillis() - start) < READ_TIMEOUT & printing) {
                logger.infof("[%s] Socket iteration: '%d' (%d ms)", tag, it, elapsed);

                try {
                    final String read = connection.readAsString();
                    logger.infof("[%s] Socket read: '%s'", tag, read);

                    if (StringUtils.isNotBlank(read)) {
                        if (out.add(read)) {
                            onReceiveEpc(read, null);
                        }

                        start = System.currentTimeMillis();
                    }
                } catch (IOException e) {
                    logger.error("[%s] Error reading from socket", tag, e);
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
    public void onReceiveEpc(final String epc, final String tid) {
        logger.infof("[%s] Received EPC: '%s', TID: '%s'", tag(), epc, tid);
    }

    @Override
    public void onUpdateStatus(final Object obj) {
        throw new UnsupportedOperationException("Method Not Implemented.");
    }

    // ACTIONS
    @Override
    public boolean play() throws PrinterException {
        throw new UnsupportedOperationException("Method Not Implemented.");
    }

    @Override
    public boolean pause() throws PrinterException {
        throw new UnsupportedOperationException("Method Not Implemented.");
    }

    @Override
    public boolean cancel() throws PrinterException {
        throw new UnsupportedOperationException("Method Not Implemented.");
    }

    @Override
    public boolean cancelSku() throws PrinterException {
        throw new UnsupportedOperationException("Method Not Implemented.");
    }

}

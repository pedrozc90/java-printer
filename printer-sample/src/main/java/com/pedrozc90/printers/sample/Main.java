package com.pedrozc90.printers.sample;

import com.pedrozc90.printers.core.Printer;
import com.pedrozc90.printers.sample.mocks.SatoMock;
import com.pedrozc90.printers.tests.utils.ResourceUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    private static final PrinterPool pool = PrinterPool.getInstance();

    private static final ResourceUtils resources = ResourceUtils.getInstance();

    public static void main(final String[] args) {
        final SatoMock mock = new SatoMock();

        final String ip = mock.getHost();
        final int port = mock.getPort();


        try (final Printer printer = pool.create("SATO", ip, port)) {
            logger.infof("Printer: %s", printer);
            printer.init();

            try {
                final String sku = "812345";
                final int qtd = 1;
                final String content = resources.getAsString("files/SBPL.txt", StandardCharsets.UTF_8);

                final Set<String> results = printer.print(content, sku, qtd);
                logger.infof("Printed: %s", results);
            } catch (IOException e) {
                logger.error("Error while reading file", e);
            }
        } catch (Exception e) {
            logger.error("Error while printing", e);
        } finally {
            try {
                mock.close();
            } catch (IOException e) {
                logger.error("Error while closing mock", e);
            }
        }
    }

}

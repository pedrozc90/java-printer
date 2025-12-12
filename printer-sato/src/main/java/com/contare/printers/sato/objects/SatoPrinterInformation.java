package com.contare.printers.sato.objects;

import com.contare.printers.sato.enums.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@EqualsAndHashCode
@ToString
public class SatoPrinterInformation {

    private static final Logger logger = Logger.getLogger(SatoPrinterInformation.class);

    private static final Pattern EPC_PATTERN = Pattern.compile(".*EP:([0-9A-Za-z]+)");
    private static final Pattern TID_PATTERN = Pattern.compile(".*ID:([0-9A-Za-z]+)");
    private static final Pattern REMAIN_BUFFER_PATTERN = Pattern.compile("Q(\\d{6})");

    private final PrinterStatus printerStatus;
    private final ReceiveBufferStatus receiveBuffer;
    private final RibbonStatus ribbonStatus;
    private final MediaStatus mediaStatus;
    private final ErrorNumber errorNumber;
    private final String epc;
    private final String tid;
    private final Integer remainBuffer;

    public static SatoPrinterInformation parse(final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        final PrinterStatus ps = PrinterStatus.parse(value);
        final ReceiveBufferStatus rb = ReceiveBufferStatus.parse(value);
        final RibbonStatus rs = RibbonStatus.parse(value);
        final MediaStatus ms = MediaStatus.parse(value);
        final ErrorNumber en = ErrorNumber.parse(value);
        final String epc = getEPC(value);
        final String tid = getTID(value);
        final Integer q = getRemainingBufferNumber(value);

        return new SatoPrinterInformation(ps, rb, rs, ms, en, epc, tid, q);
    }

    private static String getEPC(final String value) {
        try {
            final Matcher m = EPC_PATTERN.matcher(value);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.errorf(e, "Error EPC parser: %s", e.getMessage());
        }
        return null;
    }

    private static String getTID(final String value) {
        try {
            final Matcher m = TID_PATTERN.matcher(value);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.errorf(e, "Error TID parser: %s", e.getMessage());
        }
        return null;
    }

    private static Integer getRemainingBufferNumber(final String value) {
        // Printer status information name: Q
        // 000000 to 999999: 6-digit remaining number of print
        try {
            final Matcher m = REMAIN_BUFFER_PATTERN.matcher(value);
            if (m.find()) {
                return Integer.valueOf(m.group(1));
            }
        } catch (Exception e) {
            logger.errorf(e, "Error Remain Buffer parser: %s", e.getMessage());
        }
        return null;
    }

}

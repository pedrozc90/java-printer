package com.contare.printers.zebra.objects;

import com.contare.printers.zebra.enums.RFIDOperation;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class ZebraPrinterInformation {

    private static final String START_BLOCK = "<start>";
    private static final String END_BLOCK = "<end>";

    private RFIDData data = new RFIDData();
    private List<RFIDOperation> operationStatusList = new ArrayList<>();
    private List<String> epcs = new ArrayList<>();
    private boolean emptyReceived;
    private boolean startEndReceived;

    public static ZebraPrinterInformation parse(final String value) {
        final ZebraPrinterInformation obj = new ZebraPrinterInformation();
        obj.setEmptyReceived(false);
        obj.setStartEndReceived(false);

        if (StringUtils.isBlank(value)) {
            obj.setStartEndReceived(true);
            return obj;
        }

        // break 'value' into rows by '\r\n'
        final List<String> lines = Arrays.stream(value.split("\\r?\\n"))
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // if we receive only '<start><end>', it is required to send '~HL' again
        if (lines.size() <= 2) {
            if (StringUtils.equals(lines.get(0), START_BLOCK) && StringUtils.equals(lines.get(1), END_BLOCK)) {
                obj.setStartEndReceived(true);
                return obj;
            }
        }

        int startIndex = lines.indexOf(START_BLOCK);
        int endIndex = lines.indexOf(END_BLOCK);

        if (endIndex - startIndex > 0) {
            // if end with ",3400|", means it was printed as 'void'
            final List<String> linesRfidStatusWrite = lines.stream()
                .filter((s) -> isOperationInLine(s, RFIDOperation.WRITE))
                .filter((s) -> !s.contains(",3400|"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!linesRfidStatusWrite.isEmpty()) {
                final List<RFIDData> dataWrite = linesRfidStatusWrite.stream()
                    .map(obj.data::parse)
                    .collect(Collectors.toList());

                if (!dataWrite.isEmpty()) {
                    obj.epcs.addAll(dataWrite.stream().map(RFIDData::getData).filter(Objects::nonNull).collect(Collectors.toList()));
                    obj.operationStatusList.addAll(dataWrite.stream().map(RFIDData::getOperation).collect(Collectors.toList()));
                }
            }

            final List<String> linesRfidStatusLock = lines.stream()
                .filter((s) -> isOperationInLine(s, RFIDOperation.LOCK_UNLOCK_MEMORY_BANK))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!linesRfidStatusLock.isEmpty()) {
                final List<RFIDData> dataLock = linesRfidStatusLock.stream()
                    .map(obj.data::parse)
                    .collect(Collectors.toList());
                if (!dataLock.isEmpty()) {
                    obj.operationStatusList.addAll(dataLock.stream().map(RFIDData::getOperation).collect(Collectors.toList()));
                }
            }

            if (!obj.operationStatusList.contains(RFIDOperation.WRITE)) { // || !information.operationStatusList.contains(OperationStatus.LOCK_UNLOCK_MEMORY_BANK)
                return obj;
            }

            // read operation
            final List<String> linesRfidStatusRead = lines.stream()
                .filter((s) -> isOperationInLine(s, RFIDOperation.READ))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!linesRfidStatusRead.isEmpty()) {
                List<RFIDData> dataRead = linesRfidStatusRead.stream().map(obj.data::parse)
                    .collect(Collectors.toList());
                if (!dataRead.isEmpty()) {
                    obj.operationStatusList.addAll(dataRead.stream().map(RFIDData::getOperation).collect(Collectors.toList()));
                }
            }

        }

        return obj;
    }

    public static boolean isOperationInLine(final String value, final RFIDOperation expected) {
        final String[] args = (value != null) ? value.split(",") : new String[0];
        if (args.length >= 1) {
            return StringUtils.equals(args[0], expected.getCode());
        }
        return false;
    }

}

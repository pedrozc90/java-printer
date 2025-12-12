package com.contare.printers.zebra.objects;

import com.contare.printers.zebra.enums.RFIDOperation;
import com.contare.printers.zebra.enums.RFIDStatus;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * RFID Data Log
 * <p>
 * ZPL (pg. 363)
 * <p>
 * format: [date&time][RFID operation],[program position],[antenna element],[read or write power],[RFID status],[data]
 * where: 'date&time' = log entry timestamp, older version does not display it
 * 'operation' = (B = Permalock, E = Log file reset, L = lock, M = lock/unlock, R = read, S = settings, W = write
 * 'program position' = "F1"
 * 'antenna element' = "D3"
 * 'read or write power' = 27
 * 'status' = "00000000"
 * 'data' = "DATA"
 * <p>
 * ZPL II (pg. 288)
 * <p>
 * format: "C,EEEE,DDDDDDDDDDDDDDDDDDDDDDDD"
 * where: C = the RFID operation (R = read, W = write, L = lock)
 * EEEE = the RFID error code
 * DDDDDDDDDDDDDDDDDDDDDDDD = data read or written
 */
@Data
public class RFIDData {

    private LocalDateTime timestamp;
    private RFIDOperation operation;
    private String programPosition;
    private String antennaElement;
    private String power;
    private RFIDStatus status;
    private String data; // store EPC hexadecimal string

    public RFIDData parse(final String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }

        final String[] args = line.split(",");

        if (args.length >= 3) {
            if (!StringUtils.equals(args[0], RFIDOperation.RFID_SETTINGS.getCode())) {
                // 110ix4
                if (args.length == 3 || args.length == 4) {
                    final RFIDOperation operation = RFIDOperation.get(args[0]);
                    setOperation(operation);

                    final RFIDStatus status = RFIDStatus.get(args[1]);
                    setStatus(status);

                    if (operation == RFIDOperation.WRITE) {
                        setData(args[2]);
                    }

                }
                // zt410
                else {
                    final RFIDOperation operation = RFIDOperation.get(args[0]);
                    setOperation(operation);

                    final String programPosition = args[1];
                    setProgramPosition(programPosition);

                    final String antennaElement = args[2];
                    setAntennaElement(antennaElement);

                    final String power = args[3];
                    setPower(power);

                    final RFIDStatus status = RFIDStatus.get((args.length >= 5) ? args[4] : null);
                    setStatus(status);

                    // NO_TAG_FOUND = the printer was unable to read the label tag and may have been printed as void
                    // WRITE_FAILED = write failed, do not return epc
                    // RFID_OK = printing was successful
                    //if (operation == RFIDOperation.WRITE && status == RFIDStatus.RFID_OK && args.length >= 6) {
                    if (args.length >= 6) {
                        setData(args[5]);
                    }
                }
            }
        }

        return this;
    }

}

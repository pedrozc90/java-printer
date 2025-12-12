package com.contare.printers.zebra.objects;

import com.contare.printers.tests.utils.ResourceUtils;
import com.contare.printers.zebra.enums.RFIDOperation;
import com.contare.printers.zebra.enums.RFIDStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class ZebraPrinterInformationTest {

    private final ResourceUtils resources = ResourceUtils.getInstance();

    @Test
    public void parseReturn() throws IOException {
        final String value = resources.getAsString("sample.txt", StandardCharsets.UTF_8);

        final ZebraPrinterInformation result = ZebraPrinterInformation.parse(value);
        assertNotNull(result);
        assertFalse(result.isEmptyReceived());
        assertFalse(result.isStartEndReceived());

        final List<String> epcs = result.getEpcs();
        assertFalse(epcs.isEmpty());
        assertFalse(epcs.contains("3be1000020a9dcf7773bc3e3"));
        assertTrue(epcs.contains("3be1000020a9dcf7773bc260"));

        final RFIDData data = result.getData();
        assertNotNull(data);
        assertEquals("3be1000020a9dcf7773bc260", data.getData());
    }

    @Test
    public void parseReturnWith6Arguments() throws IOException {
        final String value = "<start>\r\nR,F1,D3,27,00000000,DATA\r\n<end>";

        final ZebraPrinterInformation result = ZebraPrinterInformation.parse(value);
        System.out.println("RESULT: " + result);
        assertNotNull(result);
        assertTrue(result.getEpcs().isEmpty());
        assertTrue(result.getOperationStatusList().isEmpty());

        final RFIDData data = result.getData();
        assertNotNull(data);
        assertEquals(RFIDOperation.READ, data.getOperation());
        assertEquals("F1", data.getProgramPosition());
        assertEquals("D3", data.getAntennaElement());
        assertEquals("27", data.getPower());
        assertEquals(RFIDStatus.RFID_OK, data.getStatus());
        assertEquals("DATA", data.getData());
    }

}

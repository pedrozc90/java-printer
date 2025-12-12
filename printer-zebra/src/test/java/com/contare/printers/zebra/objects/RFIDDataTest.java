package com.contare.printers.zebra.objects;

import com.contare.printers.zebra.enums.RFIDOperation;
import com.contare.printers.zebra.enums.RFIDStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class RFIDDataTest {

    @ParameterizedTest
    @CsvSource(value = {
        "W,0000,3be1000020a9dcf7773bc3e3",
        "W,0408,3be1000020a9dcf7773bc3e3"
    })
    @DisplayName("Parse return with 3 arguments")
    public void parseWithThreeArguments(final String value) {
        final RFIDData parsed = new RFIDData().parse(value);
        assertEquals(RFIDOperation.WRITE, parsed.getOperation());
        assertEquals(RFIDStatus.INVALID_WRITE_DATA, parsed.getStatus());
        assertEquals("3be1000020a9dcf7773bc3e3", parsed.getData());
        assertNull(parsed.getProgramPosition());
        assertNull(parsed.getAntennaElement());
        assertNull(parsed.getPower());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "W,0000,3be1000020a9dcf7773bc3e3,0",
        "W,0408,3be1000020a9dcf7773bc3e3,-1"
    })
    @DisplayName("Parse return with 4 arguments")
    public void parseWithFourArguments(final String value) {
        final RFIDData parsed = new RFIDData().parse(value);
        assertEquals(RFIDOperation.WRITE, parsed.getOperation());
        assertEquals(RFIDStatus.INVALID_WRITE_DATA, parsed.getStatus());
        assertEquals("3be1000020a9dcf7773bc3e3", parsed.getData());
        assertNull(parsed.getProgramPosition());
        assertNull(parsed.getAntennaElement());
        assertNull(parsed.getPower());
    }

    @Test
    @DisplayName("Parse settings return")
    public void parseSettingsReturn() {
        final String value = "S,RPWR=29,WPWR=29,ANT=A2,PPOS=F0";
        final RFIDData parsed = new RFIDData().parse(value);
        assertNull(parsed.getOperation()); // S
        assertNull(parsed.getStatus());
        assertNull(parsed.getData());
        assertNull(parsed.getProgramPosition()); // F0
        assertNull(parsed.getAntennaElement()); // A2
        assertNull(parsed.getPower()); // 29
    }

    @Test
    @DisplayName("Parse return with 6 arguments")
    public void parseWithSixArguments() {
        final String value = "R,F1,D3,27,00000000,DATA";
        final RFIDData parsed = new RFIDData().parse(value);
        assertAll(
            () -> assertEquals(RFIDOperation.READ, parsed.getOperation()),
            () -> assertEquals("F1", parsed.getProgramPosition()),
            () -> assertEquals("D3", parsed.getAntennaElement()),
            () -> assertEquals("27", parsed.getPower()),
            () -> assertEquals(RFIDStatus.RFID_OK, parsed.getStatus()),
            () -> assertEquals("DATA", parsed.getData())
        );
    }

}

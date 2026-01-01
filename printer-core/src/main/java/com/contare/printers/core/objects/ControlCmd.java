package com.contare.printers.core.objects;

public class ControlCmd {

    public static final byte EOF = -1;   // End of File (decimal = -1)
    public static final byte STX = 0x02; // Start of Text (decimal = 2, unicode = '\\u0002')
    public static final byte ETX = 0x03; // End of Text (decimal = 3, unicode = '\\u0003')
    public static final byte ACK = 0x06; // Acknowledge (decimal = 6, unicode = '\\u0006')
    public static final byte DLE = 0x10; // ??? (decimal = 10, unicode = '\\u0010')
    public static final byte DC1 = 0x11; // ??? (decimal = 11, unicode = '\\u0011')
    public static final byte DC2 = 0x12; // ??? (decimal = 12, unicode = '\\u0012')
    public static final byte NAK = 0x15; // Not Acknowledge (decimal = 15, unicode = '\\u0015')
    public static final byte CR = 0x0D;  // Carriage Return (decimal = 13, unicode = '\\u000D')
    public static final byte LF = 0x0A;  // Line Feed ( decimal = 10, unicode = '\\u000A')
    public static final byte ESC = 0x1B;
}

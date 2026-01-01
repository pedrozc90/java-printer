package com.contare.printers.core;

import com.contare.printers.core.exceptions.PrinterParserException;
import com.contare.printers.core.objects.PrinterMessage;
import com.contare.printers.core.objects.RawPacket;

import java.util.List;

public interface Parser<T extends PrinterMessage> {

    List<T> parse(final RawPacket packet) throws PrinterParserException;

}

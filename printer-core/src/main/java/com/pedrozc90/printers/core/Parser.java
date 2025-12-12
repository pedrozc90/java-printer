package com.pedrozc90.printers.core;

import com.pedrozc90.printers.core.exceptions.PrinterParserException;
import com.pedrozc90.printers.core.objects.PrinterMessage;
import com.pedrozc90.printers.core.objects.RawPacket;

import java.util.List;

public interface Parser<T extends PrinterMessage> {

    List<T> parse(final RawPacket packet) throws PrinterParserException;

}

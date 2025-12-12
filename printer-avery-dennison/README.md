# Avery Dennison Module

## Description

Implementation of the **Avery Dennison** printer connection via TCP/IP Socket.
Avery Dennison Printer Module

## Overview

This module implements the driver for Avery Dennison label printers.
The primary entry point is `com.pedrozc90.printers.averydennison.AveryDennisonPrinter`,
which extends the shared `BasePrinter` behavior from `printer-core`.

## Highlights

- TCP/IP socket based communication
- Collects printed EPCs and exposes callbacks for status and EPC events

## Build & test

```bash
mvn -am -pl printer-avery-dennison clean package
```

```bash
mvn -pl printer-avery-dennison test
```

## Usage

```java
import com.pedrozc90.printers.averydennison.AveryDennisonPrinter;

public class Main {
    public static main(final String[] args) {
        try (final AveryDennisonPrinter p = new AveryDennisonPrinter("localhost", 4000)) {
            p.init();
            p.connect();
            final Set<String> epcs = p.print(data, "0101010", 10);
            System.out.println("Printed EPCs: " + epcs);
        } catch (Exception e) {
            System.err.println("Printing failed. Reason: " + e.getMessage());
        }
    }
}
```

## Notes

Some methods are intentionally left as UnsupportedOperationException
placeholders (pause/play/cancel) and should be implemented as needed.

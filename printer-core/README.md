# Printer Core

## Overview

`printer-core` contains the core abstractions and networking helpers used
across the printer modules: the `Printer` interface, `BasePrinter` common
behavior and `PrinterConnection` (TCP socket helper). This module provides
exceptions and utilities used by specific printer drivers (Zebra, SATO,
and Avery Dennison).

## Key Classes

- `com.pedrozc90.printers.core.Printer` — high-level printer abstraction
- `com.pedrozc90.printers.core.BasePrinter` — shared implementation helpers
- `com.pedrozc90.printers.core.PrinterConnection` — lightweight TCP helper

## Build & Test

From the repository root run:

```bash
# build module
mvn -am -pl printer-core clean package
```

```bash
# run module tests
mvn -am -pl printer-core test
```

## Usage

```java
import com.pedrozc90.printers.core.PrinterConnection;

public class Main {
    public static main(final String[] args) {
        final PrinterConnection conn = new PrinterConnection("localhost", 0);
        try {
            conn.connect();

            // send payload like SBPL or ZPL
            conn.send(payload);

            final String response = conn.readAsString();
            System.out.println("Printer response: " + response);

            conn.close();
        } catch (Exception e) {
            System.err.println("Connection error. Reason: " + e.getMessage());
        }
    }
}
```

## Contributing

Follow the repository contribution guidelines. Keep changes small and
add unit tests for behavioral changes.

## License

Please, read [LICENSE](./LICENSE) file.

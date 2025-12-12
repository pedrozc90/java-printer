# SATO Printer Module

## Overview

Driver implementation for SATO label printers. See
`com.pedrozc90.printers.sato.SatoPrinter` for the main implementation.

## Build & Test

```bash
# build module
mvn -am -pl printer-sato clean package
```

```bash
# run module tests
mvn -am -pl printer-sato test
```

## Usage

```java
import com.pedrozc90.printers.sato.SatoPrinter;

public class Main {
    public static main(final String[] args) {
        try (SatoPrinter p = new SatoPrinter("localhost", 4000)) {
            p.init();
            p.connect();
            final Set<String> epcs = p.print(sbpl, "0101010", 10);
            System.out.println("Printed EPCs: " + epcs);
        } catch (Exception e) {
            System.err.println("Printing failed. Reason: " + e.getMessage());
        }
    }
}
```

> See `printer-core` for shared helpers and `PrinterConnection`.

## Printer Responses

### Command `DC2 + PG`

```txt
[STX]32,PS0,RS0,RE0,PE0,EN00,BT0,Q000000[ETX]
```

### Command `DC2 + PK`

```txt
[STX]53,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678[CR][LF][ETX]
[STX]25,1,N,ID:E200680612345678[CR][LF][ETX]
[STX]9,1,T,ID:[CR][LF][ETX]
[STX]9,0,E,ID:[CR][LF][ETX]
```

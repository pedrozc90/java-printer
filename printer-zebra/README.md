# Zebra Printer Module

## Overview

Driver implementation for Zebra label printers. The main class is
`com.pedrozc90.printers.zebra.ZebraPrinter` which extends `BasePrinter` and
provides Zebra-specific print and status handling.

## Build & Test

```bash
# build module
./mvnw -am -pl printer-zebra clean package
```

```bash
# run module tests
./mvnw -am -pl printer-zebra test
```

## Usage

```java
import com.pedrozc90.printers.zebra.ZebraPrinter;

public class Main {
    public static main(final String[] args) {
        try (ZebraPrinter p = new ZebraPrinter("localhost", 4000)) {
            p.init();
            p.connect();
            final Set<String> epcs = p.print(zpl, "0101010", 10);
            System.out.println("Printed EPCs: " + epcs);
        } catch (Exception e) {
            System.err.println("Printing failed. Reason: " + e.getMessage());
        }
    }
}
```

> See `printer-core` for shared helpers and `PrinterConnection`.

## Printer Responses

### Command `~HL` or `^HL`

#### Model 110xi4

-   Format: `A`,`BBBB`,`CCCCCCCCCCCCCCCCCCCCCCCC`
-   Where:
    - `A` = operation
    - `BBBB` = status
    - `CCCCCCCCCCCCCCCCCCCCCCCC` = data

```txt
<start>
W,0000,3be1000020a9dcf7773bc3e3
<end>
```

```txt
<start>
W,0000,3be1000020a9dcf7773bc3e3
W,0408,3be1000020a9dcf7773bc3e4
<end>
```

#### Model ???

-   Format: `A`,`BBBB`,`CCCCCCCCCCCCCCCCCCCCCCCC`,`D`
-   Where:
    - `A` = operation
    - `BBBB` = status
    - `CCCCCCCCCCCCCCCCCCCCCCCC` = data
    - `D` = result

```txt
<start>
W,0000,3be1000020a9dcf7773bc260,0
W,0408,3be1000020a9dcf7773bc261,-1
W,0408,3be1000020a9dcf7773bc262,-1
W,0408,3be1000020a9dcf7773bc263,-1
W,0408,3be1000020a9dcf7773bc264,-1
W,0408,3be1000020a9dcf7773bc265,-1
W,0408,3be1000020a9dcf7773bc266,-1
W,0408,3be1000020a9dcf7773bc267,-1
S,RPWR=29,WPWR=29,ANT=A2,PPOS=F0
W,0408,3be1000020a9dcf7773bc268,-1
W,0408,3be1000020a9dcf7773bc269,-1
<end>
```

#### Model ZT410

-   Format: `DATETIME``A`,`B`,`C`,`DDDDDDDD`,`DATA`
-   Where:
    - `DATETIME` = date & time (optional - some older version do not display it)
    - `A` = operation
    - `B` = program position
    - `C` = antenna element
    - `DDDDDDDD` = status
    - `DATA` = data
    - 
```txt
<start>
R,F1,D3,27,00000000,DATA
<end>
```

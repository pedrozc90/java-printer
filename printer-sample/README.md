# Printer Sample Module

## Overview

`printer-sample` provides a minimal runtime demonstrating how to create
and use `Printer` implementations. It contains `PrinterFactory` and a small
`Main` useful for manual testing and examples.

## Build & run

```bash
# build module
./mvnw -pl printer-sample -am clean package
```

```bash
# run module tests
./mvnw -am -pl printer-sample test
```

```bash
# run Main
java -cp printer-sample/target/classes com.pedrozc90.printers.sample.Main
```

## Usage

Use `PrinterFactory` to create implementations by type: `SATO`, `ZEBRA`,
`AVERY_DENNISON`.

```java
import com.pedrozc90.printers.core.Printer;
import com.pedrozc90.printers.sample.PrinterPool;

public class Main {
    private static final PrinterPool pool = PrinterPool.getInstance();

    public static main(final String[] args) {
        try (Printer p = pool.create("SATO", "localhost", 0)) {
            p.init();
            p.connect();
            final Set<String> epcs = p.print(payload, "0101010", 10);
            System.out.println("Printed EPCs: " + epcs);
        } catch (Exception e) {
            System.err.println("Printing failed. Reason: " + e.getMessage());
        }
    }
}
```

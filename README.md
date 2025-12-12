# java-printer

Java library for label printer communication over TCP/IP, supporting **Zebra**, **SATO**, and **Avery Dennison** drivers.
Organized as a Maven multi-module project.

## Modules

| Module                   | Description                                                   |
|:-------------------------|:--------------------------------------------------------------|
| `printer-core`           | Shared abstractions and the `PrinterConnection` TCP/IP helper |
| `printer-zebra`          | Zebra driver implementation                                   |
| `printer-sato`           | SATO driver implementation                                    |
| `printer-avery-dennison` | Avery Dennison driver implementation                          |
| `printer-sample`         | `PrinterFactory` and runnable examples                        |
| `printer-tests`          | test utilities and fixtures                                   |

## Requirements

- Java 11+
- Maven 3.6+

## Quick Start

Build the whole project from the repository root:

```bash
mvn clean package
```

Run the sample application:

```bash
mvn -pl printer-sample -am package
java -cp printer-sample/target/classes com.pedrozc90.printers.sample.Main
```

## Usage

Use `PrinterFactory` to instantiate a driver. Supported types: `ZEBRA`, `SATO`, `AVERY_DENNISON`.

```java
public class Main {
    
    private static final String ip = "192.168.1.100";
    private static final int port = 9100;
    
    public static void main(final String[] args) {
        final Printer printer = new SatoPrinter(ip, port);
        // final ZebraPrinter zebra = new ZebraPrinter(ip, port);
        // final AveryDennisonPrinter averydennison = new AveryDennisonPrinter(ip, port);
        final Set<String> results = printer.print(sbplabel, sku, qtd);
        System.out.println("EPCs: " + results);
    }
}
```

## Development

Build and test a single module and its dependencies:

```bash
mvn -pl printer-sato -am test
```

Each driver module depends on `printer-core`. The `printer-sample` module can be used as a
manual integration harness when you don't have a physical printer available.

## Contributing

Pull requests and issues are welcome. Please:

- Add unit tests for any behavior changes
- Keep public APIs backward compatible
- Follow the existing code style

## License

See [LICENSE](./LICENSE) file.

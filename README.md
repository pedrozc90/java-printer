# Printer Java

## Overview

This repository contains a set of Java modules that implement networking
and driver logic for label printers (Zebra, SATO and Avery Dennison).
It's organized as a Maven multi-module project with a small sample and a
tests module.

## Repository layout

- `printer-core` — shared abstractions and the `PrinterConnection` TCP helper
- `printer-zebra` — Zebra driver implementation
- `printer-sato` — SATO driver implementation
- `printer-avery-dennison` — Avery Dennison driver implementation
- `printer-sample` — minimal runtime and `PrinterFactory` for examples
- `printer-tests` — test utilities and fixtures

## Quick Start

Build the whole project from the repository root:

```bash
mvn clean package
```

Run the sample (after building):

```bash
mvn -pl printer-sample -am package
java -cp printer-sample/target/classes com.contare.printers.sample.Main
```

## Module Development

- To work on a single module and its dependencies:

```bash
mvn -pl printer-sato -am test
```

- Use the `PrinterFactory` in `printer-sample` to create driver instances
  for manual testing (types: `SATO`, `ZEBRA`, `AVERY_DENNISON`).

## Contributing

Please open issues and PRs. Add unit tests for behavior changes and keep
the public APIs stable across releases.

## License

Please, read [LICENSE](./LICENSE) file.

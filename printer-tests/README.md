# Printer Tests

## Overview

`printer-tests` contains utilities and test harnesses used by the repository
for integration and unit testing. This module is configured to skip creating
an artifact (see its `pom.xml`) and is intended for development and CI runs.

## Running tests

```bash
# run module tests
mvn -pl printer-tests test
```

## Utilities

Look at `com.pedrozc90.printers.tests.utils.ResourceUtils` for helpers that
load test fixtures used across the test suite.

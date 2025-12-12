# Installation

## From GH releases

Get the pre-built JAR from the [releases page](https://github.com/marcemv90/soap-cli/releases) and copy it to a location of your choice. 

That's it.

## Build from source

To build `soap-cli` from source, you need:

- Java Development Kit (JDK) 17 or later
- [Apache Maven](https://maven.apache.org/) 3.8+ on your `PATH`

### 1. Clone the repository

```bash
git clone https://github.com/marcemv90/soap-cli.git
cd soap-cli
```

### 2. Build the executable JAR

```bash
mvn clean package
```

This produces a fat JAR at:

```text
target/soap-cli.jar
```

### 3. Run `soap-cli`

```bash
java -jar target/soap-cli.jar --help
```

You should see the CLI help output.

Once built, you can copy `target/soap-cli.jar` anywhere you like and invoke it with `java -jar`.

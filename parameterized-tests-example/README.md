# Parameterized tests examples

## Description
Examples on creating parameterized unit tests using Java and the JUnit 5 framework

## Execution

Build project and execute all unit tests
```shell
mvn clean install
```

Execute a single unit test:
```shell
mvn test -Dtest=EnumSourceExampleParameterizedTest#testSendMessage
```
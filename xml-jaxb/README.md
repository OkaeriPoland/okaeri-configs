# Okaeri Configs | XML (JAXB)

Based on JAXB.

## Installation

### Maven

Add repository to the `repositories` section:

```xml
<repository>
    <id>okaeri-repo</id>
    <url>https://storehouse.okaeri.eu/repository/maven-public/</url>
</repository>
```

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-xml-jaxb</artifactId>
  <version>4.0.0-beta7</version>
</dependency>
```

### Gradle

Add repository to the `repositories` section:

```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-xml-jaxb:4.0.0-beta7'
```

## Limitations

...

## Usage

Please use XmlJaxbConfigurer as your configurer:

```java
new XmlJaxbConfigurer()
```

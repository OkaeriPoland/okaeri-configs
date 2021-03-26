# Okaeri Configs | XML (dom4j)

Based on [dom4j/dom4j](https://github.com/dom4j/dom4j).

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
  <artifactId>okaeri-configs-xml-dom4j</artifactId>
  <version>1.6.4</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-xml-dom4j:1.6.4'
```

## Usage

Please use XmlDomConfigurer as your configurer:
```java
// default
new XmlDomConfigurer()
```

# Okaeri Configs | HOCON (lightbend)

Based on [lightbend/config](https://github.com/lightbend/config), a part of [Play Framework](https://www.playframework.com/) ecosystem. Not recommended for configurations with multiple levels due to limitations.

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
  <artifactId>okaeri-configs-hocon-lightbend</artifactId>
  <version>2.6.7</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-configs-hocon-lightbend:2.6.7'
```

## Limitations
Lightbend's config at the time of implementing has poor support for dynamically generated configs and is missing features in general:
- Only top level comments and header are available (through standard ConfigPostprocessor hack), see in lightbend/config:
  - [Allow comment on root element (or header comment) #481 from Jul 20, 2017](https://github.com/lightbend/config/issues/481)
  - [multi-line comments #152 from Mar 18, 2014](https://github.com/lightbend/config/issues/152)
- Only top root is guaranteed to be ordered (current Lightbend's HOCON stores configs as unordered maps), see in lightbend/config:
  - [Keep config item order in ConfigObject #365 from Dec 28, 2015](https://github.com/lightbend/config/issues/365)
- HOCON features like includes and substitutions were not tested and are not expected to be working

## Usage

Please use HoconLightbendConfigurer as your configurer:
```java
// default ('# ', '\n')
new HoconLightbendConfigurer()
// add empty spaces between sections
new HoconLightbendConfigurer(SectionSeparator.NEW_LINE)
// change comment character and section separator (no space after # in comments, empty spaces between sections)
new HoconLightbendConfigurer("#", SectionSeparator.NEW_LINE)
```

# Okaeri Configs | Bucket4j

Serializers/Deserializers for [vladimir-bukhtoyarov/bucket4j](https://github.com/vladimir-bukhtoyarov/bucket4j), a java rate limiting library based on token/leaky-bucket algorithm.

```java
new SerdesBucket4j()
```

## Notes

Requires [serdes-commons](https://github.com/OkaeriPoland/okaeri-configs/tree/master/serdes-commons) or other `java.time.Duration` transformer provider.

## Installation

### Maven

Add dependency to the `dependencies` section:

```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-configs-serdes-bucket4j</artifactId>
  <version>4.0.0-beta20</version>
</dependency>
```

### Gradle

Add dependency to the `maven` section:

```groovy
implementation 'eu.okaeri:okaeri-configs-serdes-bucket4j:4.0.0-beta20'
```

## Supported types

### Serializers

| Type | Properties |
|-|-|
| io.github.bucket4j.local.LocalBucket | `bandwidths` |
| io.github.bucket4j.Bandwidth | `capacity`, `refill-period`, `refill-tokens` |
| eu.okaeri.configs.serdes.bucket4j.wrapper.SingleBandwidthBucket | `capacity`, `refill-period`, `refill-tokens` |

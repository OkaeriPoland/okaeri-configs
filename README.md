# Okaeri Configs 

![License](https://img.shields.io/github/license/OkaeriPoland/okaeri-configs)
![Total lines](https://img.shields.io/tokei/lines/github/OkaeriPoland/okaeri-configs)
![Repo size](https://img.shields.io/github/repo-size/OkaeriPoland/okaeri-configs)
![Contributors](https://img.shields.io/github/contributors/OkaeriPoland/okaeri-configs)
[![Discord](https://img.shields.io/discord/589089838200913930)](https://discord.gg/hASN5eX)


## Supported platforms
### YAML
- 🌟 [Minecraft (Bukkit) YamlConfiguration](https://github.com/OkaeriPoland/okaeri-configs/tree/master/yaml-bukkit): no need for additional dependencies when writing Minecraft plugins
### HJSON
- 🌟 [hjson-java](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hjson): Human JSON is the best choice for your JSON configuration
### JSON
- 🌟 [Google GSON](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-gson): ideal for GSON lovers, best suited for in-app storage or advanced user configurations
- [json-simple](https://github.com/OkaeriPoland/okaeri-configs/tree/master/json-simple): fairly limited but still working, no pretty print, probably best suited for simple in-app storage
### HOCON
- [Lightbend (HOCON) Config](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hocon-lightbend): use Human-Optimized Config Object Notation for your configs, but beware of limitations

## Genesis

Okaeri's configuration library is an easy way to use java classes as config adapters:

- Supports different environments with minimal hassle and relatively small footprint
- Allows for even complex types to be serialized/deserialized
- Enhances your configs with durable comments and strongly-typed fields
- Provides ability to access typed fields with the classic getters and setters
- Core library is just ~60kB in size, most of the adapters require only ~100 lines of code

## Example

```java
@Header("################################################################")
@Header("#                                                              #")
@Header("#    okaeri-configs test                                       #")
@Header("#                                                              #")
@Header("#    Nie wiesz jak uzywac? Zerknij do dokumentacji!            #")
@Header("#    https://github.com/OkaeriPoland/okaeri-configs            #")
@Header("#                                                              #")
@Header("#    Trouble using? Check out the documentation!               #")
@Header("#    https://github.com/OkaeriPoland/okaeri-configs            #")
@Header("#                                                              #")
@Header("################################################################")
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class TestConfig extends OkaeriConfig {

    @Variable("APP_TOKEN") // use jvm property or environment variable if available
    @Comment({"Klucz prywatny API", "API secret"})
    private String token = "";

    @CustomKey("example-list")
    @Comment({"Example list", "providing @CustomKey demonstration"})
    private List<String> exampleList = Collections.singletonList("127.0.0.1");

    @Comment({"Simple maps", "ready to go"})
    private Map<String, String> messages = Collections.singletonMap("test", "testing");

    @Comment({"Test complex map 1", "looks like complex maps are working too"})
    private Map<String, Map<String, Integer>> complexMap = Collections.singletonMap("aa", Collections.singletonMap("bb", 222));

    @Comment("Test complex map 2")
    private Map<String, Map<Integer, String>> complexMap2 = Collections.singletonMap("bb", Collections.singletonMap(232, "aadda"));

    @Comment("Custom objects can be serialized")
    private Location spawn = new Location(null, 1, 2, 3, 4, 5);

    @Comment("Non-string map keys")
    private Map<Integer, String> levels = Collections.singletonMap(1, "aaaaaa");

    @Comment("okaeri-configs likes classes more than primitives")
    private Integer version = 2;

    @Comment({"Test enum", "very nice", "right?"})
    private TestEnum testEnum = TestEnum.ONE_THO_THREE;

    @Comment("Test enum list")
    private List<TestEnum> testEnumList = Arrays.asList(TestEnum.ONE, TestEnum.ONE_THO_THREE);

    @Comment("Test enum set")
    private Set<TestEnum> testEnumSet = new HashSet<>(Arrays.asList(TestEnum.ONE, TestEnum.ONE_THO_THREE));

    @Comment("Test custom object list")
    @Comment(".. and repeating comments")
    private List<Location> testLocationList = Arrays.asList(
            new Location(null, 1, 2, 3, 4, 5),
            new Location(null, 3, 3, 5, 6, 9)
    );

    @Comment("Uber-complex-map test")
    private Map<TestEnum, Location> enumToLocationMap = Collections.singletonMap(TestEnum.THREE, new Location(null, 1, 2, 3, 4, 5));

    @CustomKey("list-to-uber-complex-map")
    @Comment("List-to-Uber-complex-map test")
    private List<Map<TestEnum, Location>> listMapEnumToLocationMap = Arrays.asList(
            Collections.singletonMap(TestEnum.THREE, new Location(null, 1, 2, 3, 4, 5)),
            Collections.singletonMap(TestEnum.ONE_THO_THREE, new Location(null, 3, 2, 3, 4, 5))
    );

    @Comment("Math test")
    private BigInteger bigInteger = new BigInteger("999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999876543210");

    @Exclude
    private Instant start = Instant.now();

    /* ... */
}
```

## Usage

### With create(clazz, initializer)
```java
TestConfig config = ConfigManager.create(TestConfig.class, (it) -> {
    it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit()); // specify configurer implementation, optionally additional serdes packages
    it.withBindFile("config.yml"); // specify File or pathname
    it.saveDefaults(); // save file if does not exists
    it.load(true); // load and save to update comments/new fields 
});
```

### With create(clazz)
```java
TestConfig config = (TestConfig) ConfigManager.create(TestConfig.class)
    .withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit()) // specify configurer implementation, optionally additional serdes packages
    .withBindFile("config.yml") // specify File or pathname
    .saveDefaults() // save file if does not exists
    .load(true); // load and save to update comments/new fields
````

## Supported types

- Subconfigs: Any config can be used in the fields of another (`OkaeriConfig`)
- Basic Java types: Boolean, Byte, Character, Double, Float, Integer, Long, Short, String
- Primitives: boolean, byte, char, double, float, int, long, short
- Math types: `java.math.BigInteger`, `java.math.BigDecimal`
- Complex types:
    - `Map<K, V>`: results in LinkedHashMap
    - `Set<T>`: results in HashSet
    - `List<T>`: results in ArrayList
    - Any type assignable from Map or Collection if non-interface type is used and default constructor is available
- Enum types: any enum is automatically transformed using `valueOf()` and `name()`
- Custom types using `ObjectSerializer`/`ObjectTransformer` (see in supported platforms)

## Validation

Currently available validators:
- [core-validator](https://github.com/OkaeriPoland/okaeri-configs/core-validator): hibernate-validator based with full Jakarta Bean Validation 3.0 support

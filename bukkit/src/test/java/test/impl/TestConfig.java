package test.impl;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Location;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Header("################################################################")
@Header("#                                                              #")
@Header("#    OK! No.Proxy Minecraft                                    #")
@Header("#                                                              #")
@Header("#    Nie wiesz jak skonfigurować? Zerknij do dokumentacji!     #")
@Header("#    https://wiki.okaeri.eu/pl/uslugi/noproxy/minecraft        #")
@Header("#                                                              #")
@Header("#    Trouble configuring? Check out the documentation!         #")
@Header("#    https://wiki.okaeri.eu/en/services/noproxy/minecraft      #")
@Header("#                                                              #")
@Header("################################################################")
@Names(strategy = NameStrategy.SNAKE_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class TestConfig extends OkaeriConfig {

    @Variable("APP_TOKEN")
    @Comment({"Klucz prywatny API", "API secret"})
    private String token = "";

    @CustomKey("white-list")
    @Comment({"Biala lista (wpisane nicki lub ip nie beda blokowane)", "Whitelist (nicknames or ips)"})
    private List<String> whitelist = Collections.singletonList("127.0.0.1");

    @Comment({"Wiadomosci", "Messages"})
    private Map<String, String> messages = Collections.singletonMap("test", "testing");

    @Comment("Test complex map 1")
    private Map<String, Map<String, Integer>> complexMap = Collections.singletonMap("aa", Collections.singletonMap("bb", 222));

    @Comment("Test complex map 2")
    private Map<String, Map<Integer, String>> complexMap2 = Collections.singletonMap("bb", Collections.singletonMap(232, "aadda"));

    @Comment("Spawn")
    private Location spawnLOC = new Location(null, 1, 2, 3, 4, 5);

    @Comment({"Poziomy", "levels"})
    private Map<Integer, String> levels = Collections.singletonMap(1, "aaaaaa");

    @Comment({"Nie edytuj tej wartosci", "Do not edit"})
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

    @Comment("Uber-complex-map tree map test")
    private TreeMap<TestEnum, Location> enumToLocationTreeMap = new TreeMap<>(Collections.singletonMap(TestEnum.THREE, new Location(null, 1, 2, 3, 4, 5)));

    @Comment("Uber-complex-map test non-generic")
    private HashMap<TestEnum, Location> enumToLocationMapNonGeneric = new HashMap<>(Collections.singletonMap(TestEnum.THREE, new Location(null, 1, 2, 3, 4, 5)));

    @CustomKey("list-to-uber-complex-map")
    @Comment("List-to-Uber-complex-map test")
    private List<Map<TestEnum, Location>> listMapEnumToLocationMap = Arrays.asList(
            Collections.singletonMap(TestEnum.THREE, new Location(null, 331, 2, 3, 4, 5)),
            Collections.singletonMap(TestEnum.ONE_THO_THREE, new Location(null, 3, 2, 3, 4, 5))
    );

    @Comment("Math test")
    private BigInteger bigInteger = new BigInteger("999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999876543210");

    @Comment("Serializable test")
    private SerializableSubconfig elo = ConfigManager.initialize(new SerializableSubconfig("testString0", true, Arrays.asList("abchehe", "dfghe")));

    @Comment("Test serializable list")
    private List<SerializableSubconfig> serializableList = Arrays.asList(
            ConfigManager.initialize(new SerializableSubconfig("testString1", true, Arrays.asList("abc", "dfg"))),
            ConfigManager.initialize(new SerializableSubconfig("testString2", true, Arrays.asList("axxbc", "dfddg")))
    );

    @Comment("Test primitive int")
    private int testPrimitiveInt = 22;

    @Comment("Test primitive double")
    private double testPrimitiveDouble = 23123.223323334678;

    @Comment("Test boolean")
    private boolean testBoolean = true;

    @Exclude
    private Instant start = Instant.now();
}

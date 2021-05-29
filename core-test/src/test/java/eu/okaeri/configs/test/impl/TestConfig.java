package eu.okaeri.configs.test.impl;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import eu.okaeri.configs.test.obj.CraftLocation;
import eu.okaeri.configs.test.obj.CraftWorld;
import eu.okaeri.configs.test.obj.Location;
import eu.okaeri.validator.annotation.Size;
import lombok.*;

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

    @Size(min = 1, max = 3)
    @Variable("APP_TOKEN")
    @Comment({"Klucz prywatny API", "API secret"})
    private String token = "a";

    @CustomKey("white-list")
    @Comment({"", "Biala lista (wpisane nicki lub ip nie beda blokowane)", "Whitelist (nicknames or ips)"})
    private List<String> whitelist = Collections.singletonList("127.0.0.1");

    @Comment({"", "Wiadomosci", "Messages"})
    private Map<String, String> messages = Collections.singletonMap("test", "testing");

    @Comment({" ", "Test complex map 1"})
    private Map<String, Map<String, Integer>> complexMap = Collections.singletonMap("aa", Collections.singletonMap("bb", 222));

    @Comment("Test complex map 2")
    private Map<String, Map<Integer, String>> complexMap2 = Collections.singletonMap("bb", Collections.singletonMap(232, "aadda"));

    @Comment("Spawn")
    private Location spawnLOC = new CraftLocation(new CraftWorld("world_nether"), 1, 2, 3, 4, 5);

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
    private HashMap<TestEnum, Location> enumToLocationMapNonGeneric = new HashMap<>(Collections.singletonMap(TestEnum.THREE, new Location(new CraftWorld("world_the_end"), 1, 2, 3, 4, 5)));

    @CustomKey("list-to-uber-complex-map")
    @Comment("List-to-Uber-complex-map test")
    private List<Map<TestEnum, Location>> listMapEnumToLocationMap = Arrays.asList(
            Collections.singletonMap(TestEnum.THREE, new Location(null, 331, 2, 3, 4, 5)),
            Collections.singletonMap(TestEnum.ONE_THO_THREE, new Location(null, 3, 2, 3, 4, 5))
    );

    @Comment("Math test")
    private BigInteger bigInteger = new BigInteger("999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999876543210");

    @Comment("Serializable test")
    private SerializableSubconfig elo = ConfigManager.initialize(new SerializableSubconfig("testString0", true, Arrays.asList("abchehe", "dfghe"), TestEnum.ONE));

    @Comment("Test serializable list")
    private List<SerializableSubconfig> serializableList = Arrays.asList(
            ConfigManager.initialize(new SerializableSubconfig("testString1", true, Arrays.asList("abc", "dfg"), TestEnum.TWO)),
            ConfigManager.initialize(new SerializableSubconfig("testString2", true, Arrays.asList("axxbc", "dfddg"), TestEnum.THREE))
    );

    @Comment("Test primitive int")
    private int testPrimitiveInt = 22;

    @Comment("Test primitive double")
    private double testPrimitiveDouble = 23123.223323334678;

    @Comment("Test boolean")
    private boolean testBoolean = true;

    @Comment("Test boolean wrapper")
    private Boolean testBooleanWrapper = true;

    @Comment("Test long")
    private long testLong = 100000000L;

    @Comment("Test long wrapper")
    private Long testLongWrapper = 100000000L;

    @Comment("Test float")
    private float testFloat = 1.2323f;

    @Comment("Test float wrapper")
    private Float testFloatWrapper = 1.2323f;

    @Comment("Test char")
    private char testChar = 'a';

    @Comment("Test char wrapper")
    private Character testCharWrapper = 'a';

    @Comment("Test int")
    private int testInt = 123;

    @Comment("Test int wrapper")
    private int testIntWrapper = 123;

    @Comment("Test short")
    private short testShort = 2;

    @Comment("Test short wrapper")
    private Short testShortWrapper = 2;

    @Comment("Test byte")
    private byte testByte = 2;

    @Comment("Test byte wrapper")
    private Byte testByteWrapper = 2;

    @Comment("Storage settings")
    private StorageConfig testInner = new StorageConfig();

    @Getter
    @Setter
    @ToString
    public class StorageConfig extends OkaeriConfig {

        @Variable("OPE_STORAGE_PREFIX")
        @Comment("Prefix for the storage: allows to have multiple instances using same database")
        @Comment("FLAT  : no effect due to local nature")
        @Comment("REDIS : {storagePrefix}:{collection} -> OkaeriPlatformBukkitExample:player")
        @Comment("MYSQL : {storagePrefix}:{collection} -> ope_storage_player (recommended shortened [here 'ope:storage'] due to database limitations)")
        private String prefix = "OkaeriPlatformBukkitExample:storage";

        @Variable("OPE_STORAGE_URI")
        @Comment("FLAT  : not applicable, plugin controlled")
        @Comment("REDIS : redis://localhost")
        @Comment("MYSQL : jdbc:mysql://localhost:3306/db")
        private String uri = "redis://localhost";

        @Variable("OPE_STORAGE_USERNAME")
        @Comment("FLAT  : N/A")
        @Comment("REDIS : N/A")
        @Comment("MYSQL : used")
        private String username = "root";

        @Variable("OPE_STORAGE_PASSWORD")
        @Comment("FLAT  : N/A")
        @Comment("REDIS : optional")
        @Comment("MYSQL : optional")
        private String password = "1234";

        @Comment("example @Names inheritance")
        private String someStringHere = "huh";
    }

    @Comment("Test multiline string")
    private String multiline = "WELCOME\nMULTIPLIE\n\nLINES\n!!!!!!!!!!!11111";

    @Exclude
    private Instant start = Instant.now();
}

package eu.okaeri.example;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

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
public class ExampleConfig extends OkaeriConfig {

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
    private Location spawn = new Location(Bukkit.getWorlds().get(0), 1, 2, 3, 4, 5);

    @Comment("Non-string map keys")
    private Map<Integer, String> levels = Collections.singletonMap(1, "aaaaaa");

    @Comment("okaeri-configs likes classes more than primitives")
    private Integer version = 2;

    @Comment({"Test enum", "very nice", "right?"})
    private ExampleEnum testEnum = ExampleEnum.ONE_THO_THREE;

    @Comment("Test enum list")
    private List<ExampleEnum> ExampleEnumList = Arrays.asList(ExampleEnum.ONE, ExampleEnum.ONE_THO_THREE);

    @Comment("Test enum set")
    private Set<ExampleEnum> testEnumSet = new HashSet<>(Arrays.asList(ExampleEnum.ONE, ExampleEnum.ONE_THO_THREE));

    @Comment("Test custom object list")
    @Comment(".. and repeating comments")
    private List<Location> testLocationList = Arrays.asList(
            new Location(null, 1, 2, 3, 4, 5),
            new Location(null, 3, 3, 5, 6, 9)
    );

    @Comment("Uber-complex-map test")
    private Map<ExampleEnum, Location> enumToLocationMap = Collections.singletonMap(ExampleEnum.THREE, new Location(Bukkit.getWorlds().get(0), 1, 2, 3, 4, 5));

    @CustomKey("list-to-uber-complex-map")
    @Comment("List-to-Uber-complex-map test")
    private List<Map<ExampleEnum, Location>> listMapEnumToLocationMap = Arrays.asList(
            Collections.singletonMap(ExampleEnum.THREE, new Location(Bukkit.getWorlds().get(0), 1, 2, 3, 4, 5)),
            Collections.singletonMap(ExampleEnum.ONE_THO_THREE, new Location(Bukkit.getWorlds().get(0), 3, 2, 3, 4, 5))
    );

    @Comment("Math test")
    private BigInteger bigInteger = new BigInteger("999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999876543210");

    @Exclude
    private Instant start = Instant.now();

    // GETTERS/SETTERS - LOMBOK CLASS (@Getter @Setter @ToString) RECOMMENDED FOR THE CLEANER CODE //
    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getExampleList() {
        return this.exampleList;
    }

    public void setExampleList(List<String> exampleList) {
        this.exampleList = exampleList;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public Map<String, Map<String, Integer>> getComplexMap() {
        return this.complexMap;
    }

    public void setComplexMap(Map<String, Map<String, Integer>> complexMap) {
        this.complexMap = complexMap;
    }

    public Map<String, Map<Integer, String>> getComplexMap2() {
        return this.complexMap2;
    }

    public void setComplexMap2(Map<String, Map<Integer, String>> complexMap2) {
        this.complexMap2 = complexMap2;
    }

    public Location getSpawn() {
        return this.spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public Map<Integer, String> getLevels() {
        return this.levels;
    }

    public void setLevels(Map<Integer, String> levels) {
        this.levels = levels;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public ExampleEnum getTestEnum() {
        return this.testEnum;
    }

    public void setTestEnum(ExampleEnum testEnum) {
        this.testEnum = testEnum;
    }

    public List<ExampleEnum> getExampleEnumList() {
        return this.ExampleEnumList;
    }

    public void setExampleEnumList(List<ExampleEnum> exampleEnumList) {
        this.ExampleEnumList = exampleEnumList;
    }

    public Set<ExampleEnum> getTestEnumSet() {
        return this.testEnumSet;
    }

    public void setTestEnumSet(Set<ExampleEnum> testEnumSet) {
        this.testEnumSet = testEnumSet;
    }

    public List<Location> getTestLocationList() {
        return this.testLocationList;
    }

    public void setTestLocationList(List<Location> testLocationList) {
        this.testLocationList = testLocationList;
    }

    public Map<ExampleEnum, Location> getEnumToLocationMap() {
        return this.enumToLocationMap;
    }

    public void setEnumToLocationMap(Map<ExampleEnum, Location> enumToLocationMap) {
        this.enumToLocationMap = enumToLocationMap;
    }

    public List<Map<ExampleEnum, Location>> getListMapEnumToLocationMap() {
        return this.listMapEnumToLocationMap;
    }

    public void setListMapEnumToLocationMap(List<Map<ExampleEnum, Location>> listMapEnumToLocationMap) {
        this.listMapEnumToLocationMap = listMapEnumToLocationMap;
    }

    public BigInteger getBigInteger() {
        return this.bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public Instant getStart() {
        return this.start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }
}
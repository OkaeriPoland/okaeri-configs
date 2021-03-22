package test.impl;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Exclude;
import eu.okaeri.configs.annotation.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Location;

import java.time.Instant;
import java.util.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Header({"################################################################\n" +
        "#                                                              #\n" +
        "#    OK! No.Proxy Minecraft                                    #\n" +
        "#                                                              #\n" +
        "#    Nie wiesz jak skonfigurować? Zerknij do dokumentacji!     #\n" +
        "#    https://wiki.okaeri.eu/pl/uslugi/noproxy/minecraft        #\n" +
        "#                                                              #\n" +
        "#    Trouble configuring? Check out the documentation!         #\n" +
        "#    https://wiki.okaeri.eu/en/services/noproxy/minecraft      #\n" +
        "#                                                              #\n" +
        "################################################################"})
public class TestConfig extends OkaeriConfig {

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
    private Location spawn = new Location(null, 1, 2, 3, 4, 5);

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
    private List<Location> testLocationList = Arrays.asList(
            new Location(null, 1, 2, 3, 4, 5),
            new Location(null, 3, 3, 5, 6,9)
    );

    @Comment("Uber-complex-map test")
    private Map<TestEnum, Location> enumToLocationMap = Collections.singletonMap(TestEnum.THREE, new Location(null, 1, 2, 3, 4, 5));

    @CustomKey("complex-map")
    @Comment("List-to-Uber-complex-map test")
    private List<Map<TestEnum, Location>> listMapEnumToLocationMap = Arrays.asList(
            Collections.singletonMap(TestEnum.THREE, new Location(null, 1, 2, 3, 4, 5)),
            Collections.singletonMap(TestEnum.ONE_THO_THREE, new Location(null, 3, 2, 3, 4, 5))
    );

    @Exclude
    private Instant start = Instant.now();
}

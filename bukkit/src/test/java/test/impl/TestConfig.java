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
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Exclude
    private Instant start = Instant.now();
}

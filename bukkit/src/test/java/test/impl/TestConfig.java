package test.impl;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @Comment({"Nie edytuj tej wartosci", "Do not edit"})
    private Integer version = 2;
}

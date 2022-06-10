package eu.okaeri.configs.test;

import eu.okaeri.commons.indexedset.IndexedSet;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.serdes.okaeri.indexedset.IndexedSetSpec;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class TestIndexedSetSerializer {

    @Test
    public void test_serializer() {

        TestConfig config = new TestConfig();
        config.withConfigurer(new YamlBukkitConfigurer(),
//                new SerdesOkaeri(),
                registry -> registry.register(new CustomPlayerSerializer())
        );

        String configString = config.saveToString();
        System.out.println(config);
        System.out.println(config.getPlayers().getClass());
        System.out.println(configString);

        config.load(configString);
        System.out.println(config);
        System.out.println(config.getPlayers().getClass());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static class Player extends OkaeriConfig {

        private UUID id;

        @CustomKey("test-name")
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CustomPlayer {
        private int id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static class TestConfig extends OkaeriConfig {

        @IndexedSetSpec(key = "test-name")
        private IndexedSet<Player, String> players = IndexedSet.builder(Player.class, String.class)
                .keyFunction(Player::getName)
                .add(new Player(UUID.randomUUID(), "Player1"))
                .add(new Player(UUID.randomUUID(), "Player2"))
                .build();

        @IndexedSetSpec(key = "id")
        private IndexedSet<CustomPlayer, Integer> customPlayers = IndexedSet.of(CustomPlayer::getId,
                new CustomPlayer(1, "CPlayer1"),
                new CustomPlayer(2, "CPlayer2")
        );
    }

    static class CustomPlayerSerializer implements ObjectSerializer<CustomPlayer> {

        @Override
        public boolean supports(@NonNull Class<? super CustomPlayer> type) {
            return CustomPlayer.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(@NonNull CustomPlayer object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.add("id", object.getId());
            data.add("name", object.getName());
        }

        @Override
        public CustomPlayer deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            int id = data.get("id", int.class);
            String name = data.get("name", String.class);
            return new CustomPlayer(id, name);
        }
    }
}

package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ItemStackTransformer extends BidirectionalTransformer<ItemStack, String> {

    @Override
    public GenericsPair<ItemStack, String> getPair() {
        return this.genericsPair(ItemStack.class, String.class);
    }

    @Override
    @SneakyThrows
    public String leftToRight(@NonNull ItemStack data, @NonNull SerdesContext serdesContext) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        dataOutput.writeObject(data);
        dataOutput.close();

        return Base64.getEncoder()
            .encodeToString(outputStream.toByteArray())
            .trim();
    }

    @Override
    @SneakyThrows
    public ItemStack rightToLeft(@NonNull String data, @NonNull SerdesContext serdesContext) {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();

        return item;
    }
}

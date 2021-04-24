package eu.okaeri.configs.binary.obdf;

import eu.okaeri.bin.Bin;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObdfConfigurer extends Configurer {

    private Bin bin = new Bin();

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, false);
        this.bin.putUnsafe(key, simplified);
    }

    @Override
    public Object getValue(String key) {
        return this.bin.get(key);
    }

    @Override
    public boolean keyExists(String key) {
        return this.bin.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.bin.getData().keySet()));
    }

    @Override
    public void load(InputStream inputStream, ConfigDeclaration declaration) throws Exception {
        this.bin.load(ConfigPostprocessor.of(inputStream).getContext());
    }

    @Override
    public void write(OutputStream outputStream, ConfigDeclaration declaration) throws Exception {
        outputStream.write(this.bin.write().getBytes(StandardCharsets.UTF_8));
    }
}

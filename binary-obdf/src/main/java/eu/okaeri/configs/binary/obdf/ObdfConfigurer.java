package eu.okaeri.configs.binary.obdf;

import eu.okaeri.bin.Bin;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.io.File;

public class ObdfConfigurer extends Configurer {

    private Bin bin = new Bin();

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type);
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
    public void loadFromFile(File file, ConfigDeclaration declaration) throws Exception {
        if (!file.exists()) {
            return;
        }
        this.bin.load(ConfigPostprocessor.of(file).read().getContext());
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws Exception {
        ConfigPostprocessor.of(file, this.bin.write()).write();
    }
}

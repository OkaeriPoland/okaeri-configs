package eu.okaeri.configs.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public class GsonConfigurer extends Configurer {

    private Map<String, Object> map;
    private Gson gson;

    public GsonConfigurer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.map = new LinkedHashMap<>();
    }

    public GsonConfigurer(Gson gson) {
        this(gson, new LinkedHashMap<>());
    }

    public GsonConfigurer(Gson gson, Map<String, Object> map) {
        this.gson = gson;
        this.map = map;
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type);
        this.map.put(key, simplified);
    }

    @Override
    public Object getValue(String key) {
        return this.map.get(key);
    }

    @Override
    public boolean keyExists(String key) {
        return this.map.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadFromFile(File file, ConfigDeclaration declaration) throws Exception {

        if (!file.exists()) {
            return;
        }

        String data = ConfigPostprocessor.of(file).read().getContext();
        this.map = this.gson.fromJson(data, Map.class);

        if (this.map != null) {
            return;
        }

        this.map = new LinkedHashMap<>();
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws Exception {
        try (Writer writer = new FileWriter(file)) {
            this.gson.toJson(this.map, writer);
        }
    }
}

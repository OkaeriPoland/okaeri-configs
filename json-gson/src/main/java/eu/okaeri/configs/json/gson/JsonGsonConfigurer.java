package eu.okaeri.configs.json.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JsonGsonConfigurer extends Configurer {

    private Map<String, Object> map;
    private Gson gson;

    public JsonGsonConfigurer() {
        this.gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
        this.map = new LinkedHashMap<>();
    }

    public JsonGsonConfigurer(@NonNull Gson gson) {
        this(gson, new LinkedHashMap<>());
    }

    public JsonGsonConfigurer(@NonNull Gson gson, @NonNull Map<String, Object> map) {
        this.gson = gson;
        this.map = map;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("json");
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        String data = ConfigPostprocessor.of(inputStream).getContext();
        this.map = this.gson.fromJson(data, Map.class);

        if (this.map != null) {
            return;
        }

        this.map = new LinkedHashMap<>();
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        this.gson.toJson(this.map, writer);
        writer.flush();
    }
}

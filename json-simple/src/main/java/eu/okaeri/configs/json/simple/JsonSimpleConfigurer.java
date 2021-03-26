package eu.okaeri.configs.json.simple;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonSimpleConfigurer extends Configurer {

    private Map<String, Object> map;
    private JSONParser parser;

    public JsonSimpleConfigurer() {
        this.parser = new JSONParser();
        this.map = new LinkedHashMap<>();
    }

    public JsonSimpleConfigurer(JSONParser parser) {
        this(parser, new LinkedHashMap<>());
    }

    public JsonSimpleConfigurer(JSONParser parser, Map<String, Object> map) {
        this.parser = parser;
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
        this.map = (Map<String, Object>) this.parser.parse(data);

        if (this.map != null) {
            return;
        }

        this.map = new LinkedHashMap<>();
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws Exception {
        try (Writer writer = new FileWriter(file)) {
            JSONObject object = new JSONObject(this.map);
            ConfigPostprocessor.of(file, object.toJSONString()).write();
        }
    }
}

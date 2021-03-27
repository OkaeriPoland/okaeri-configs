package eu.okaeri.configs.test.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Location {
    private World world;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;
}

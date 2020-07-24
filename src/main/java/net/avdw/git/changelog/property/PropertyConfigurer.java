package net.avdw.git.changelog.property;

import com.google.inject.Inject;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class PropertyConfigurer {
    private final Properties defaultProperties;
    private final Properties localProperties;
    private final Properties globalProperties;

    @Inject
    public PropertyConfigurer(@Default final Properties defaultProperties,
                              @Local final Properties localProperties,
                              @Global final Properties globalProperties) {
        this.defaultProperties = defaultProperties;
        this.localProperties = localProperties;
        this.globalProperties = globalProperties;
    }

    public Properties configure() {
        Properties prioritizedProperties = new Properties();
        prioritizedProperties.putAll(defaultProperties);
        prioritizedProperties.putAll(localProperties);
        prioritizedProperties.putAll(globalProperties);

        List<String> propertyList = new ArrayList<>();
        prioritizedProperties.forEach((key, value)->propertyList.add(String.format("%s=%s", key, value)));
        Logger.debug("Properties:\n{}", String.join("\n", propertyList));
        return prioritizedProperties;
    }
}

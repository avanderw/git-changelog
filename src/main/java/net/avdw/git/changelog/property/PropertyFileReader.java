package net.avdw.git.changelog.property;

import org.tinylog.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertyFileReader {
    public Properties read(final Path propertyPath) {
        Properties properties = new Properties();
        if (Files.exists(propertyPath)) {
            try (FileReader fileReader = new FileReader(propertyPath.toFile(), StandardCharsets.UTF_8)) {
                properties.load(fileReader);
                Logger.debug("File loaded: {}", propertyPath);
            } catch (IOException e) {
                Logger.error(e.getMessage());
                Logger.debug(e);
            }
        } else {
            Logger.debug("File does not exist: {}", propertyPath);
        }
        return properties;
    }
}

package net.avdw.git.changelog.temp;

import com.google.inject.AbstractModule;
import lombok.SneakyThrows;
import org.codehaus.plexus.util.FileUtils;
import org.tinylog.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TempModule extends AbstractModule {
    @Override
    protected void configure() {
        Path tmpDir = Paths.get("tmp");
        tmpDir.toFile().mkdirs();
        bind(Path.class).annotatedWith(Temp.class).toInstance(tmpDir);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            @SneakyThrows
            public void run() {
                if (Files.exists(tmpDir)) {
                    Logger.debug("Removing directory {}", tmpDir.toAbsolutePath());
                    FileUtils.deleteDirectory(tmpDir.toFile());
                }
            }
        });
    }
}

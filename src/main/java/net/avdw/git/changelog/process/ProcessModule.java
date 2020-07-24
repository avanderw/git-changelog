package net.avdw.git.changelog.process;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.avdw.git.changelog.PropertyKey;
import org.tinylog.Logger;

import javax.inject.Named;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ProcessModule extends AbstractModule {
    @Provides
    @Bash
    @Singleton
    Path bashPath(@Named(PropertyKey.BASH_PATH) final String bashPath) {
        Path path = Paths.get(findExecutableOnPath("bash.exe").orElse(bashPath));
        Logger.debug("Using path: {}", path);
        return path;
    }

    public Optional<String> findExecutableOnPath(String name) {
        for (String dirname : System.getenv("PATH").split(File.pathSeparator)) {
            File file = new File(dirname, name);
            if (file.isFile() && file.canExecute()) {
                Logger.debug("Found bash executable: {}", file.getAbsolutePath());
                return Optional.of(file.getAbsolutePath());
            }
        }

        Logger.debug("Could not find executable ({}) on system path", name);
        return Optional.empty();
    }
}

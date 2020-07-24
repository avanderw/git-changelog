package net.avdw.git.changelog;

import lombok.SneakyThrows;
import org.tinylog.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarExtractor {
    @SneakyThrows
    public void extract(final Path toDirectory, final String pattern) {
        String jarFileName = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        extract(jarFileName, toDirectory, pattern);
    }

    @SneakyThrows
    public void extract(final String jarFileName, final Path toDirectory, final String pattern) {
        if (!jarFileName.endsWith(".jar")) {
            Logger.warn("{} is not a jar", jarFileName);
            return;
        }

        try (JarFile jar = new JarFile(jarFileName)) {
            Pattern searchPattern = Pattern.compile(pattern);
            Logger.debug("Extracting jar");
            Enumeration<JarEntry> enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry entry = enumEntries.nextElement();
                File file = toDirectory.resolve(entry.getName()).toFile();

                Matcher matcher = searchPattern.matcher(entry.getName());
                if (!matcher.find()) {
                    continue;
                }

                try (InputStream inputStream = jar.getInputStream(entry)) {
                    if (!Files.exists(file.toPath())) {
                        Logger.debug("Creating file {}", file);
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}

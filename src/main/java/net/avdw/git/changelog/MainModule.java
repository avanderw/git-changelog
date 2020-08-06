package net.avdw.git.changelog;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import lombok.SneakyThrows;
import net.avdw.git.changelog.process.ProcessModule;
import net.avdw.git.changelog.property.AbstractPropertyModule;
import net.avdw.git.changelog.temp.Temp;
import net.avdw.git.changelog.temp.TempModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class MainModule extends AbstractPropertyModule {
    @Override
    protected void configure() {
        Properties properties = configureProperties();
        Names.bindProperties(binder(), properties);
        bind(List.class).to(LinkedList.class);

        install(new TempModule());
        install(new ProcessModule());
    }

    @Override
    protected Properties defaultProperties() {
        Properties properties = new Properties();
        properties.put(PropertyKey.BASH_PATH, "C:\\Program Files\\Git\\usr\\bin\\bash.exe");
        return properties;
    }

    @Provides
    @GitCheckout
    @Singleton
    @SneakyThrows
    Path gitCheckoutScript(@Temp final Path tmpDir, final JarExtractor jarExtractor) {
        Path script = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("scripts/git-checkout.sh");
        if (!Files.exists(script)) {
            jarExtractor.extract(tmpDir, ".*\\.sh");
        }
        return script;
    }

    @Provides
    @GitCurrentBranch
    @Singleton
    @SneakyThrows
    Path gitCurrentBranchScript(@Temp final Path tmpDir, final JarExtractor jarExtractor) {
        Path script = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("scripts/git-current-branch.sh");
        if (!Files.exists(script)) {
            jarExtractor.extract(tmpDir, ".*\\.sh");
        }
        return script;
    }

    @Provides
    @GitFirstCommit
    @Singleton
    @SneakyThrows
    Path gitFirstCommitScript(@Temp final Path tmpDir, final JarExtractor jarExtractor) {
        Path script = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("scripts/git-first-commit.sh");
        if (!Files.exists(script)) {
            jarExtractor.extract(tmpDir, ".*\\.sh");
        }
        return script;
    }

    @Provides
    @GitLatestTag
    @Singleton
    @SneakyThrows
    Path gitLatestTagScript(@Temp final Path tmpDir, final JarExtractor jarExtractor) {
        Path script = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("scripts/git-latest-tag.sh");
        if (!Files.exists(script)) {
            jarExtractor.extract(tmpDir, ".*\\.sh");
        }
        return script;
    }

    @Provides
    @GitLs
    @Singleton
    @SneakyThrows
    Path gitLsScript(@Temp final Path tmpDir, final JarExtractor jarExtractor) {
        Path script = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("scripts/git-ls.sh");
        if (!Files.exists(script)) {
            jarExtractor.extract(tmpDir, ".*\\.sh");
        }
        return script;
    }

    @Provides
    @Singleton
    ResourceBundle resourceBundle() {
        return ResourceBundle.getBundle("changelog", Locale.ENGLISH);
    }

    @Provides
    @Singleton
    Templator templator(final ResourceBundle resourceBundle) {
        return new Templator(resourceBundle);
    }
}

package net.avdw.git.changelog;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import lombok.SneakyThrows;
import net.avdw.git.changelog.process.ProcessModule;
import net.avdw.git.changelog.property.AbstractPropertyModule;
import net.avdw.git.changelog.temp.Temp;
import net.avdw.git.changelog.temp.TempModule;

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
    Path gitCheckoutScript(@Script Path scriptPath) {
        return scriptPath.resolve("scripts/git-checkout.sh");
    }

    @Provides
    @GitCurrentBranch
    @Singleton
    @SneakyThrows
    Path gitCurrentBranchScript(@Script Path scriptPath) {
        return scriptPath.resolve("scripts/git-current-branch.sh");
    }

    @Provides
    @GitFirstCommit
    @Singleton
    @SneakyThrows
    Path gitFirstCommitScript(@Script Path scriptPath) {
        return scriptPath.resolve("scripts/git-first-commit.sh");
    }

    @Provides
    @GitLatestTag
    @Singleton
    @SneakyThrows
    Path gitLatestTagScript(@Script Path scriptPath) {
        return scriptPath.resolve("scripts/git-latest-tag.sh");
    }

    @Provides
    @GitLs
    @Singleton
    @SneakyThrows
    Path gitLsScript(@Script Path scriptPath) {
        return scriptPath.resolve("scripts/git-ls.sh");
    }

    @Provides
    @Singleton
    ResourceBundle resourceBundle() {
        return ResourceBundle.getBundle("changelog", Locale.ENGLISH);
    }

    @Provides
    @Singleton
    @Script
    @SneakyThrows
    Path scriptPath(@Temp final Path tmpDir, final JarExtractor jarExtractor) {
        if (Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toString().endsWith(".jar")) {
            jarExtractor.extract(tmpDir, ".*\\.sh");
            return tmpDir;
        } else {
            return Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        }
    }

    @Provides
    @Singleton
    Templator templator(final ResourceBundle resourceBundle) {
        return new Templator(resourceBundle);
    }
}

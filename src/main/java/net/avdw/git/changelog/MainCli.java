package net.avdw.git.changelog;

import com.google.inject.Inject;
import lombok.SneakyThrows;
import net.avdw.git.changelog.process.ProcessRunner;
import net.avdw.git.changelog.temp.Temp;
import org.tinylog.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;

@Command(name = "git-changelog", description = "Git changelog transformer",
        versionProvider = MainVersion.class, mixinStandardHelpOptions = true,
        subcommands = {})
public class MainCli implements Runnable {
    @Parameters(arity = "0..1", index = "1")
    private String from = "";
    @Inject
    @GitLs
    private Path gitLsPath;
    @Inject
    private ProcessRunner processRunner;
    @Option(names = {"-r", "--repository"})
    private Path repository;
    @Spec
    private CommandSpec spec;
    @Inject
    @Temp
    private Path tmpDir;
    @Parameters(arity = "0..1", index = "0")
    private String to = "master";

    public MainCli() {
    }

    /**
     * Entry point for picocli.
     */
    @Override
    @SneakyThrows
    public void run() {
        Logger.debug("MainCli.java entry point");
        processRunner.execute(gitLsPath, repository, String.format("-t=%s", to), String.format("-f=%s", from), String.format("-o=%s/git-ls", tmpDir.toAbsolutePath()));
        spec.commandLine().usage(spec.commandLine().getOut());

    }
}

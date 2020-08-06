package net.avdw.git.changelog.process;

import com.google.inject.Inject;
import lombok.SneakyThrows;
import org.tinylog.Logger;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class ProcessRunner {
    private static final int BASH_PROCESS_ARGS = 3;

    private final Path bashExecutable;

    @Inject
    ProcessRunner(@Bash final Path bashPath) {
        this.bashExecutable = bashPath;
    }

    @SneakyThrows
    public void execute(final Path script, final Path baseDir, final PrintStream out, final String... scriptArgs) {
        Path baseDirCopy = (baseDir == null) ? Paths.get("").toAbsolutePath() : baseDir;
        Logger.debug("Executing script: {} {}", script.getFileName(), Arrays.toString(scriptArgs));
        Logger.debug("Base directory: {}", baseDirCopy);
        if (script.toString().endsWith(".sh")) {
            String[] processArgs = new String[BASH_PROCESS_ARGS + scriptArgs.length];
            processArgs[0] = bashExecutable.toString();
            processArgs[1] = "--login";
            processArgs[2] = script.toString();
            System.arraycopy(scriptArgs, 0, processArgs, BASH_PROCESS_ARGS, scriptArgs.length);
            Process process = new ProcessBuilder(processArgs)
                    .directory(baseDirCopy.toFile())
                    .start();

            routeStream(process.getInputStream(), out);
            routeStream(process.getErrorStream(), null);
            process.waitFor();
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported script type: %s", script));
        }
    }

    private static void routeStream(final InputStream src, final PrintStream dest) {
        new Thread(() -> {
            Scanner scanner = new Scanner(src);
            while(scanner.hasNextLine()) {
                if (dest == null) {
                    Logger.debug("Script: {}", scanner.nextLine());
                } else {
                    dest.println(scanner.nextLine());
                }
            }
        }).start();
    }
}

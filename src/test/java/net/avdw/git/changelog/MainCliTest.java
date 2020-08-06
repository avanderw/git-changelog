package net.avdw.git.changelog;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.avdw.git.changelog.process.ProcessRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class MainCliTest {

    private static final Path checkoutScript = Paths.get("src/test/resources/checkout.sh").toAbsolutePath();
    private static final ByteArrayOutputStream scriptOutput = new ByteArrayOutputStream();
    private static final PrintStream printStream = new PrintStream(scriptOutput);
    private static final Path testRepository = Paths.get("src/test/repository").toAbsolutePath();
    private static ProcessRunner processRunner;
    private CommandLine commandLine;
    private StringWriter errWriter;
    private StringWriter outWriter;

    @BeforeClass
    public static void beforeClass() {
        Injector injector = Guice.createInjector(new TestModule());
        processRunner = injector.getInstance(ProcessRunner.class);
    }

    private void assertSuccess(final int exitCode) {
        if (!outWriter.toString().isEmpty()) {
            Logger.debug("Standard output:\n{}", outWriter.toString());
        }
        if (!errWriter.toString().isEmpty()) {
            Logger.error("Error output:\n{}", errWriter.toString());
        }
        assertEquals("MUST NOT HAVE error output", "", errWriter.toString());
        assertNotEquals("MUST HAVE standard output", "", outWriter.toString());
        assertEquals(0, exitCode);
    }

    @Before
    public void beforeTest() {
        commandLine = new CommandLine(MainCli.class, GuiceFactory.getInstance());
        errWriter = new StringWriter();
        outWriter = new StringWriter();
        commandLine.setOut(new PrintWriter(outWriter));
        commandLine.setErr(new PrintWriter(errWriter));
    }

    @Test
    public void testBasic() {
        assertSuccess(commandLine.execute("v1.0.0", "v1.1.0", "-r=" + testRepository));
        assertFalse("SHOULD NOT output usage help", outWriter.toString().contains("Usage"));
    }

    @Test
    public void testEmpty() {
        processRunner.execute(checkoutScript, testRepository, printStream, "feature");
        assertSuccess(commandLine.execute());
        assertFalse("SHOULD NOT output usage help", outWriter.toString().contains("Usage"));
    }

    @Test
    public void testVoidRepository() {
        assertSuccess(commandLine.execute("-r=" + System.getProperty("user.home")));
    }

    @Test
    public void testEmptyRepository() {
        processRunner.execute(checkoutScript, testRepository, printStream, "hotfix");
        assertSuccess(commandLine.execute("-r=" + testRepository));
    }

    @Test
    public void testVersion() {
        assertSuccess(commandLine.execute("--version"));
        assertNotEquals(2, outWriter.toString().length());
    }
}
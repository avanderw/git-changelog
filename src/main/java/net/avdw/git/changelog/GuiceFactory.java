package net.avdw.git.changelog;

import com.google.inject.*;
import picocli.CommandLine;

final class GuiceFactory implements CommandLine.IFactory {
    private static final GuiceFactory INSTANCE = new GuiceFactory();
    private static final Injector INJECTOR = Guice.createInjector(new MainModule());

    public static CommandLine.IFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public <K> K create(final Class<K> aClass) {
        return INJECTOR.getInstance(aClass);
    }

}

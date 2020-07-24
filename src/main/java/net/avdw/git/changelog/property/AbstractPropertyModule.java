package net.avdw.git.changelog.property;

import com.google.inject.AbstractModule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public abstract class AbstractPropertyModule extends AbstractModule {
    protected Properties configureProperties() {
        PropertyFileReader propertyFileReader = new PropertyFileReader();
        Properties localProperties = propertyFileReader.read(localPropertyPath());
        Properties globalProperties = propertyFileReader.read(globalPropertyPath());
        Properties defaultProperties = defaultProperties();
        PropertyConfigurer propertyConfigurer = new PropertyConfigurer(defaultProperties, localProperties, globalProperties);
        return propertyConfigurer.configure();
    }

    protected abstract Properties defaultProperties();

    public Path globalPropertyPath() {
        return Paths.get(System.getProperty("user.home"))
                .resolve(Paths.get(String.format("net.avdw/%s/%s", namespaceDirectory(), propertyFileName())));
    }

    public Path localPropertyPath() {
        return Paths.get(String.format("%s", propertyFileName()));
    }

    protected String namespaceDirectory() {
        String packageDirectory = this.getClass().getPackage().getName();
        packageDirectory = packageDirectory.replace("net.avdw.", "");
        if (packageDirectory.contains(".")) {
            packageDirectory = packageDirectory.substring(0, packageDirectory.lastIndexOf("."));
            packageDirectory = packageDirectory.replace('.', '/');
        }
        return packageDirectory;
    }

    protected String propertyFileName() {
        String propertyFileName = this.getClass().getPackage().getName();
        propertyFileName = propertyFileName.substring(propertyFileName.lastIndexOf(".") + 1);
        propertyFileName = String.format("%s.properties", propertyFileName);
        return propertyFileName;
    }
}

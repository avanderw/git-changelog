package net.avdw.git.changelog;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.inject.Inject;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ResourceBundle;

public class Templator {
    private final ResourceBundle resourceBundle;

    @Inject
    public Templator(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public String populate(final String templateKey, final Object data) {
        Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(resourceBundle.getString(templateKey)), templateKey);
        StringWriter stringWriter = new StringWriter();
        return mustache.execute(stringWriter, data).toString();
    }

    public String populate(final String templateKey) {
        return resourceBundle.getString(templateKey);
    }
}

package net.avdw.git.changelog;

import com.google.gson.Gson;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import net.avdw.git.changelog.process.ProcessRunner;
import net.avdw.git.changelog.temp.Temp;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

@Command(name = "git-changelog", description = "Git changelog transformer",
        versionProvider = MainVersion.class, mixinStandardHelpOptions = true,
        subcommands = {})
public class MainCli implements Runnable {
    @Parameters(arity = "0..1", index = "1")
    private String from = "";
    @Inject
    @GitLs
    private Path gitLogScript;
    private Gson gson = new Gson();
    @Inject
    private ProcessRunner processRunner;
    @Option(names = {"-r", "--repository"})
    private Path repository;
    @Spec
    private CommandSpec spec;
    @Inject
    private Templator templator;
    @Inject
    @Temp
    private Path tmpDir;
    @Parameters(arity = "0..1", index = "0")
    private String to = "master";

    private boolean calculateRecommend(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        return categorisedGitChangeMap.containsKey(ChangeType.SECURITY);
    }

    private String calculateType(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        if (isMajor(categorisedGitChangeMap)) {
            return "Major";
        } else if (isMinor(categorisedGitChangeMap)) {
            return "Feature";
        } else if (isPatch(categorisedGitChangeMap)) {
            return "Bugfix";
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private boolean isMinor(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        return categorisedGitChangeMap.containsKey(ChangeType.ADDED) || categorisedGitChangeMap.containsKey(ChangeType.DEPRECATED);
    }

    private boolean isPatch(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        return categorisedGitChangeMap.containsKey(ChangeType.FIXED)
                    || categorisedGitChangeMap.containsKey(ChangeType.SECURITY)
                    || categorisedGitChangeMap.containsKey(ChangeType.UNCLASSIFIED)
                    || categorisedGitChangeMap.containsKey(ChangeType.IGNORED);
    }

    private String calculateVersion(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        Optional<String> latestVersion = findLatestVersion();

        int majorNum = 0;
        int minorNum = 0;
        int patchNum = 0;

        if (latestVersion.isPresent()) {
            String version = latestVersion.get();
            majorNum = Integer.parseInt(version.substring(0, version.indexOf(".")));
            minorNum = Integer.parseInt(version.substring(version.indexOf(".") + 1, version.lastIndexOf(".")));
            patchNum = Integer.parseInt(version.substring(version.lastIndexOf(".") + 1));
        }
        majorNum += isMajor(categorisedGitChangeMap) ? 1 : 0;
        minorNum += isMinor(categorisedGitChangeMap) ? 1 : 0;
        patchNum += isPatch(categorisedGitChangeMap) ? 1 : 0;

        return String.format("%s.%s.%s", majorNum, minorNum, patchNum);
    }

    private boolean isMajor(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        return categorisedGitChangeMap.containsKey(ChangeType.CHANGED) || categorisedGitChangeMap.containsKey(ChangeType.REMOVED);
    }

    private Map<ChangeType, List<GitChange>> categoriseGitChangeList(final List<GitChange> gitChangeList) {
        Map<ChangeType, List<GitChange>> categorisedGitChangeMap = new HashMap<>();
        gitChangeList.forEach(gitChange -> {
            ChangeType key = identifyGitChangeType(gitChange);
            categorisedGitChangeMap.putIfAbsent(key, new ArrayList<>());
            categorisedGitChangeMap.get(key).add(gitChange);
        });
        return categorisedGitChangeMap;
    }

    private Optional<String> findLatestVersion() {
        return Optional.empty();
    }

    private ChangeType identifyGitChangeType(final GitChange gitChange) {
        if (isIgnored(gitChange)) {
            return ChangeType.IGNORED;
        }

        if (isAdded(gitChange)) {
            return ChangeType.ADDED;
        } else if (isChanged(gitChange)) {
            return ChangeType.CHANGED;
        } else if (isDeprecated(gitChange)) {
            return ChangeType.DEPRECATED;
        } else if (isRemoved(gitChange)) {
            return ChangeType.REMOVED;
        } else if (isFixed(gitChange)) {
            return ChangeType.FIXED;
        } else if (isSecurity(gitChange)) {
            return ChangeType.SECURITY;
        } else {
            return ChangeType.UNCLASSIFIED;
        }
    }

    private boolean isAdded(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("add");
    }

    private boolean isChanged(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("change");
    }

    private boolean isDeprecated(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("deprecate");
    }

    private boolean isFixed(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("fix");
    }

    private boolean isIgnored(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("maintain");
    }

    private boolean isRemoved(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("remove");
    }

    private boolean isSecurity(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("secure");
    }

    private List<GitChange> parseGitChangeList(final String jsonChangelog) {
        List<GitChange> gitChangeList = new ArrayList<>();
        Scanner lineScanner = new Scanner(jsonChangelog);
        while (lineScanner.hasNextLine()) {
            String line = lineScanner.nextLine();
            gitChangeList.add(gson.fromJson(line, GitChange.class));
        }
        return gitChangeList;
    }

    private void printChangelog(final List<GitChange> gitChanges) {
        gitChanges.forEach(gitChange -> {
            spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.LINE_ITEM, gitChange));
        });

    }

    /**
     * Entry point for picocli.
     */
    @Override
    @SneakyThrows
    public void run() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream scriptOutput = new PrintStream(baos, true, StandardCharsets.UTF_8);
        processRunner.execute(gitLogScript, repository, scriptOutput, String.format("-t=%s", to), String.format("-f=%s", from));
        String jsonChangelog = new String(baos.toByteArray());
        if (jsonChangelog.length() == 0) {
            spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.NO_CHANGE));
        } else {
            spec.commandLine().getOut().println(jsonChangelog);
            try {
                List<GitChange> gitChangeList = parseGitChangeList(jsonChangelog);
                Map<ChangeType, List<GitChange>> categorisedGitChangeMap = categoriseGitChangeList(gitChangeList);

                if (categorisedGitChangeMap.get(ChangeType.IGNORED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.IGNORED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.IGNORED));
                    spec.commandLine().getOut().println();
                }
                if (categorisedGitChangeMap.get(ChangeType.UNCLASSIFIED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.UNCLASSIFIED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.UNCLASSIFIED));
                    spec.commandLine().getOut().println();
                }

                if (!categorisedGitChangeMap.isEmpty()) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.RELEASE_TITLE,
                            gson.fromJson(String.format("{version:'%s',type:'%s',recommend:%s,date:'%s'}",
                                    calculateVersion(categorisedGitChangeMap),
                                    calculateType(categorisedGitChangeMap),
                                    calculateRecommend(categorisedGitChangeMap),
                                    new SimpleDateFormat("yyyy-MM-dd").format(new Date())),
                                    Map.class)));
                }

                if (categorisedGitChangeMap.get(ChangeType.ADDED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.ADDED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.ADDED));
                }

                if (categorisedGitChangeMap.get(ChangeType.CHANGED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.CHANGED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.CHANGED));
                }

                if (categorisedGitChangeMap.get(ChangeType.DEPRECATED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.DEPRECATED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.DEPRECATED));
                }

                if (categorisedGitChangeMap.get(ChangeType.REMOVED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.REMOVED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.REMOVED));
                }

                if (categorisedGitChangeMap.get(ChangeType.FIXED) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.FIXED_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.FIXED));
                }

                if (categorisedGitChangeMap.get(ChangeType.SECURITY) != null) {
                    spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.SECURITY_TITLE));
                    printChangelog(categorisedGitChangeMap.get(ChangeType.SECURITY));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new UnsupportedOperationException();
            }
        }
    }
}

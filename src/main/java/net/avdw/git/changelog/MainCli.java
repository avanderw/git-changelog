package net.avdw.git.changelog;

import com.google.gson.Gson;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import net.avdw.git.changelog.process.ProcessRunner;
import org.tinylog.Logger;
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
        versionProvider = MainVersion.class, mixinStandardHelpOptions = true)
public class MainCli implements Runnable {
    private String currentBranch;
    private String firstCommit;
    @Parameters(arity = "0..1", index = "1")
    private String from = "";
    @Inject
    @GitCheckout
    private Path gitCheckoutScript;
    @Inject
    @GitCurrentBranch
    private Path gitCurrentBranchScript;
    @Inject
    @GitFirstCommit
    private Path gitFirstCommitScript;
    @Inject
    @GitLatestTag
    private Path gitLatestTagScript;
    @Inject
    @GitLs
    private Path gitLogScript;
    private Gson gson = new Gson();
    private String latestTag;
    @Inject
    private ProcessRunner processRunner;
    @Option(names = {"-r", "--repository"})
    private Path repository;
    @Spec
    private CommandSpec spec;
    @Inject
    private Templator templator;
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
            return "Maintenance";
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private String calculateVersion(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        Optional<String> latestVersion = findLatestVersion();

        int majorNum = 0;
        int minorNum = 0;
        int patchNum = 0;

        if (latestVersion.isPresent()) {
            String version = latestVersion.get().trim();
            majorNum = Integer.parseInt(version.substring(0, version.indexOf(".")));
            minorNum = Integer.parseInt(version.substring(version.indexOf(".") + 1, version.lastIndexOf(".")));
            patchNum = Integer.parseInt(version.substring(version.lastIndexOf(".") + 1));
        }

        if (isMajor(categorisedGitChangeMap)) {
            majorNum += 1;
            minorNum = 0;
            patchNum = 0;
        } else if (isMinor(categorisedGitChangeMap)) {
            minorNum += 1;
            patchNum = 0;
        } else if (isPatch(categorisedGitChangeMap)) {
            patchNum += 1;
        }

        return String.format("%s.%s.%s", majorNum, minorNum, patchNum);
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

    private String currentBranch() {
        if (currentBranch == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            processRunner.execute(gitCurrentBranchScript, repository, new PrintStream(baos, true, StandardCharsets.UTF_8));
            currentBranch = new String(baos.toByteArray()).trim();
            Logger.debug("Current branch: {}", currentBranch);
        }
        return currentBranch;
    }

    private Optional<String> findLatestVersion() {
        String version = latestTag();

        if (version.isEmpty()) {
            Logger.debug("No latest version found");
            return Optional.empty();
        } else {
            if (version.startsWith("v")) {
                version = version.substring(1);
            }
            Logger.debug("Latest version: {}", version);
            return Optional.of(version);
        }
    }

    private String firstCommit() {
        if (firstCommit == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            processRunner.execute(gitFirstCommitScript, repository, new PrintStream(baos, true, StandardCharsets.UTF_8));
            firstCommit = new String(baos.toByteArray()).trim();
            Logger.debug("First commit: {}", firstCommit);
        }
        return firstCommit;
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

    private boolean isMajor(final Map<ChangeType, List<GitChange>> categorisedGitChangeMap) {
        return categorisedGitChangeMap.containsKey(ChangeType.CHANGED) || categorisedGitChangeMap.containsKey(ChangeType.REMOVED);
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

    private boolean isRemoved(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("remove");
    }

    private boolean isSecurity(final GitChange gitChange) {
        return gitChange.subject.toLowerCase().startsWith("secure");
    }

    private String latestTag() {
        if (latestTag == null) {
            if (!"master".equals(currentBranch())) {
                processRunner.execute(gitCheckoutScript, repository, null, "master");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            processRunner.execute(gitLatestTagScript, repository, new PrintStream(baos, true, StandardCharsets.UTF_8));
            latestTag = new String(baos.toByteArray()).trim();

            if (!"master".equals(currentBranch())) {
                processRunner.execute(gitCheckoutScript, repository, null, currentBranch());
            }

            Logger.debug("Latest tag: {}", latestTag);
        }
        return latestTag;
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
        gitChanges.forEach(gitChange -> spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.LINE_ITEM, gitChange)));

    }

    /**
     * Entry point for picocli.
     */
    @Override
    @SneakyThrows
    public void run() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (currentBranch().equals(to) && from.isEmpty()) {
            Logger.debug("Cannot determine changelog as current ({}) is the same as to ({}) and from is not specified", currentBranch(), to);
            if (!latestTag().isEmpty()) {
                Logger.debug("Using latest tag ({}) as base", latestTag());
                processRunner.execute(gitLogScript, repository, new PrintStream(baos, true, StandardCharsets.UTF_8), String.format("-t=%s", latestTag), String.format("-f=%s", "master"));
            } else {
                Logger.debug("Using first commit ({}) as base", firstCommit());
                processRunner.execute(gitLogScript, repository, new PrintStream(baos, true, StandardCharsets.UTF_8), String.format("-t=%s", firstCommit()), String.format("-f=%s", "master"));
            }
        } else {
            processRunner.execute(gitLogScript, repository, new PrintStream(baos, true, StandardCharsets.UTF_8), String.format("-t=%s", to), String.format("-f=%s", from));
        }

        String jsonChangelog = new String(baos.toByteArray());
        if (jsonChangelog.length() == 0) {
            spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.NO_CHANGE));
        } else {
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
                spec.commandLine().getOut().println();
            }

            if (categorisedGitChangeMap.get(ChangeType.ADDED) != null) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.ADDED_TITLE));
                printChangelog(categorisedGitChangeMap.get(ChangeType.ADDED));
                spec.commandLine().getOut().println();
            }

            if (categorisedGitChangeMap.get(ChangeType.CHANGED) != null) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.CHANGED_TITLE));
                printChangelog(categorisedGitChangeMap.get(ChangeType.CHANGED));
                spec.commandLine().getOut().println();
            }

            if (categorisedGitChangeMap.get(ChangeType.DEPRECATED) != null) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.DEPRECATED_TITLE));
                printChangelog(categorisedGitChangeMap.get(ChangeType.DEPRECATED));
                spec.commandLine().getOut().println();
            }

            if (categorisedGitChangeMap.get(ChangeType.REMOVED) != null) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.REMOVED_TITLE));
                printChangelog(categorisedGitChangeMap.get(ChangeType.REMOVED));
                spec.commandLine().getOut().println();
            }

            if (categorisedGitChangeMap.get(ChangeType.FIXED) != null) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.FIXED_TITLE));
                printChangelog(categorisedGitChangeMap.get(ChangeType.FIXED));
                spec.commandLine().getOut().println();
            }

            if (categorisedGitChangeMap.get(ChangeType.SECURITY) != null) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.SECURITY_TITLE));
                printChangelog(categorisedGitChangeMap.get(ChangeType.SECURITY));
                spec.commandLine().getOut().println();
            }

            if (!categorisedGitChangeMap.containsKey(ChangeType.ADDED) &&
                    !categorisedGitChangeMap.containsKey(ChangeType.CHANGED) &&
                    !categorisedGitChangeMap.containsKey(ChangeType.DEPRECATED) &&
                    !categorisedGitChangeMap.containsKey(ChangeType.REMOVED) &&
                    !categorisedGitChangeMap.containsKey(ChangeType.FIXED) &&
                    !categorisedGitChangeMap.containsKey(ChangeType.SECURITY)) {
                spec.commandLine().getOut().println(templator.populate(ResourceBundleKey.NONE_STANDARD_CHANGELOG));
            }
        }
    }
}

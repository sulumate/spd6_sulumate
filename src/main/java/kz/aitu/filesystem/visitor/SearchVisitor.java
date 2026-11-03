package kz.aitu.filesystem.visitor;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class SearchVisitor extends AbstractVisitor<List<String>> {
    private final Predicate<String> filenameCriterion;
    private final long minimumSizeBytes;
    private final List<String> matches = new ArrayList<>();

    public SearchVisitor(Predicate<String> filenameCriterion, long minimumSizeBytes) {
        this.filenameCriterion = Objects.requireNonNull(filenameCriterion, "Filename criterion must not be null");
        if (minimumSizeBytes < 0) {
            throw new IllegalArgumentException("Minimum size must not be negative");
        }
        this.minimumSizeBytes = minimumSizeBytes;
    }

    @Override
    protected void resetResult() {
        matches.clear();
    }

    @Override
    public void visit(FileNode file) {
        if (filenameCriterion.test(file.name()) && file.sizeBytes() >= minimumSizeBytes) {
            matches.add(relativePath().isEmpty() ? file.name() : relativePath());
        }
    }

    @Override
    public void visit(DirectoryNode directory) {
    }

    @Override
    public List<String> result() {
        return matches.stream().sorted().toList();
    }
}

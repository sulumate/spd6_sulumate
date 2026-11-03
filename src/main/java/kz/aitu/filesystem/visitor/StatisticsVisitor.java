package kz.aitu.filesystem.visitor;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;

public final class StatisticsVisitor extends AbstractVisitor<Statistics> {
    private long fileCount;
    private long directoryCount;
    private int maximumDepth;

    @Override
    protected void resetResult() {
        fileCount = 0;
        directoryCount = 0;
        maximumDepth = 0;
    }

    @Override
    public void visit(FileNode file) {
        fileCount = Math.incrementExact(fileCount);
        recordDepth();
    }

    @Override
    public void visit(DirectoryNode directory) {
        directoryCount = Math.incrementExact(directoryCount);
        recordDepth();
    }

    @Override
    public Statistics result() {
        return new Statistics(fileCount, directoryCount, maximumDepth);
    }

    private void recordDepth() {
        maximumDepth = Math.max(maximumDepth, depth());
    }
}

package kz.aitu.filesystem.visitor;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;

public final class TotalSizeVisitor extends AbstractVisitor<Long> {
    private long totalBytes;

    @Override
    protected void resetResult() {
        totalBytes = 0;
    }

    @Override
    public void visit(FileNode file) {
        totalBytes = Math.addExact(totalBytes, file.sizeBytes());
    }

    @Override
    public void visit(DirectoryNode directory) {
    }

    @Override
    public Long result() {
        return totalBytes;
    }
}

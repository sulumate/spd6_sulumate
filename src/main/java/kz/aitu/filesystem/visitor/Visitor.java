package kz.aitu.filesystem.visitor;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;

public interface Visitor<R> {
    void reset();

    void at(String relativePath, int depth);

    void visit(FileNode file);

    void visit(DirectoryNode directory);

    R result();
}

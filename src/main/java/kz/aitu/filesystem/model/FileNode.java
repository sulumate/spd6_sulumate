package kz.aitu.filesystem.model;

import kz.aitu.filesystem.visitor.Visitor;

import java.util.Objects;

public final class FileNode extends Node {
    private final long sizeBytes;

    public FileNode(String name, long sizeBytes) {
        super(name);
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("File size must not be negative");
        }
        this.sizeBytes = sizeBytes;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    @Override
    public void accept(Visitor<?> visitor) {
        Objects.requireNonNull(visitor, "Visitor must not be null").visit(this);
    }
}

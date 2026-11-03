package kz.aitu.filesystem.visitor;

import java.util.Objects;

public abstract class AbstractVisitor<R> implements Visitor<R> {
    private String relativePath = "";
    private int depth;

    @Override
    public final void reset() {
        relativePath = "";
        depth = 0;
        resetResult();
    }

    @Override
    public final void at(String relativePath, int depth) {
        Objects.requireNonNull(relativePath, "Relative path must not be null");
        if (depth < 0) {
            throw new IllegalArgumentException("Depth must not be negative");
        }
        this.relativePath = relativePath;
        this.depth = depth;
    }

    protected final String relativePath() {
        return relativePath;
    }

    protected final int depth() {
        return depth;
    }

    protected abstract void resetResult();
}

package kz.aitu.filesystem.model;

import java.util.List;
import java.util.Objects;

public abstract sealed class Node implements Element permits FileNode, DirectoryNode {
    private final String name;
    private DirectoryNode parent;

    protected Node(String name) {
        this.name = validName(name);
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public List<? extends Element> children() {
        return List.of();
    }

    final DirectoryNode parent() {
        return parent;
    }

    final void attachTo(DirectoryNode directory) {
        parent = directory;
    }

    private static String validName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank() || !name.equals(name.strip()) || name.equals(".") || name.equals("..")
                || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Name must be one non-blank path component");
        }
        return name;
    }
}

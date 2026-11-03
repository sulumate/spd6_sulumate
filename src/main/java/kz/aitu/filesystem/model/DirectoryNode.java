package kz.aitu.filesystem.model;

import kz.aitu.filesystem.visitor.Visitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class DirectoryNode extends Node {
    private final Map<String, Node> children = new LinkedHashMap<>();

    public DirectoryNode(String name) {
        super(name);
    }

    public void add(Node child) {
        Objects.requireNonNull(child, "Child must not be null");
        rejectCycle(child);
        if (child.parent() != null) {
            throw new IllegalStateException("Child already belongs to a directory");
        }
        if (children.containsKey(child.name())) {
            throw new IllegalArgumentException("Duplicate sibling name: " + child.name());
        }
        children.put(child.name(), child);
        child.attachTo(this);
    }

    public Node remove(String name) {
        Objects.requireNonNull(name, "Child name must not be null");
        Node removed = children.remove(name);
        if (removed == null) {
            throw new NoSuchElementException("No child named: " + name);
        }
        removed.attachTo(null);
        return removed;
    }

    @Override
    public List<Node> children() {
        return List.copyOf(children.values());
    }

    @Override
    public void accept(Visitor<?> visitor) {
        Objects.requireNonNull(visitor, "Visitor must not be null").visit(this);
    }

    private void rejectCycle(Node child) {
        for (Node ancestor = this; ancestor != null; ancestor = ancestor.parent()) {
            if (ancestor == child) {
                throw new IllegalArgumentException("A directory cannot contain itself or an ancestor");
            }
        }
    }
}

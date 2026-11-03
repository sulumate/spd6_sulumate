package kz.aitu.filesystem.traversal;

import kz.aitu.filesystem.model.Element;
import kz.aitu.filesystem.visitor.Visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class TreeWalker {
    public <R> R walk(Element root, Visitor<R> visitor) {
        Objects.requireNonNull(root, "Root must not be null");
        Objects.requireNonNull(visitor, "Visitor must not be null");
        visitor.reset();
        Deque<Frame> pending = new ArrayDeque<>();
        pending.push(new Frame(root, "", 0));
        while (!pending.isEmpty()) {
            Frame frame = pending.pop();
            visitor.at(frame.relativePath(), frame.depth());
            frame.element().accept(visitor);
            pushChildren(frame, pending);
        }
        return visitor.result();
    }

    private void pushChildren(Frame frame, Deque<Frame> pending) {
        List<? extends Element> children = frame.element().children();
        for (int index = children.size() - 1; index >= 0; index--) {
            Element child = children.get(index);
            String path = frame.relativePath().isEmpty() ? child.name()
                    : frame.relativePath() + "/" + child.name();
            pending.push(new Frame(child, path, Math.incrementExact(frame.depth())));
        }
    }

    private record Frame(Element element, String relativePath, int depth) {
    }
}

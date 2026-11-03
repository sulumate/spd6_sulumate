package kz.aitu.filesystem.model;

import kz.aitu.filesystem.visitor.Visitor;

import java.util.List;

public sealed interface Element permits Node {
    String name();

    List<? extends Element> children();

    void accept(Visitor<?> visitor);
}

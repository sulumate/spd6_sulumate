package kz.aitu.filesystem;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;

final class TestTrees {
    private TestTrees() {
    }

    static DirectoryNode archive() {
        DirectoryNode root = new DirectoryNode("archive");
        root.add(new FileNode("z.txt", 50));
        DirectoryNode docs = new DirectoryNode("docs");
        docs.add(new FileNode("b.md", 200));
        DirectoryNode deep = new DirectoryNode("deep");
        deep.add(new FileNode("c.txt", 300));
        docs.add(deep);
        docs.add(new FileNode("a.txt", 100));
        root.add(docs);
        root.add(new DirectoryNode("empty"));
        return root;
    }
}

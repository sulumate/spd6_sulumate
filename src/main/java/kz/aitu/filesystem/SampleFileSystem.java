package kz.aitu.filesystem;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;

public final class SampleFileSystem {
    private SampleFileSystem() {
    }

    public static DirectoryNode create() {
        DirectoryNode root = new DirectoryNode("workspace");
        DirectoryNode assets = new DirectoryNode("assets");
        assets.add(new FileNode("logo.png", 8192));
        assets.add(new DirectoryNode("empty"));
        DirectoryNode documents = new DirectoryNode("docs");
        documents.add(new FileNode("guide.txt", 1024));
        DirectoryNode drafts = new DirectoryNode("drafts");
        drafts.add(new FileNode("notes.txt", 256));
        drafts.add(new FileNode("todo.md", 128));
        documents.add(drafts);
        root.add(assets);
        root.add(documents);
        root.add(new FileNode("Main.java", 2048));
        root.add(new FileNode("zero.txt", 0));
        root.add(new FileNode("README.md", 512));
        return root;
    }
}

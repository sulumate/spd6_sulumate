package kz.aitu.filesystem;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;
import kz.aitu.filesystem.model.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeIntegrityTest {
    @Test
    void negativeSizesAreRejectedAndZeroIsAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new FileNode("bad", -1));
        assertEquals(0, new FileNode("zero", 0).sizeBytes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", ".", "..", "a/b", "a\\b", " leading", "trailing ", "line\nfeed"})
    void invalidNamesCannotBecomePathComponents(String name) {
        assertThrows(IllegalArgumentException.class, () -> new FileNode(name, 1));
        assertThrows(IllegalArgumentException.class, () -> new DirectoryNode(name));
    }

    @Test
    void unicodeAndInteriorSpacesRemainValid() {
        assertEquals("Мои файлы", new DirectoryNode("Мои файлы").name());
    }

    @Test
    void duplicateFileAndDirectorySiblingNamesAreRejectedAtomically() {
        DirectoryNode parent = new DirectoryNode("parent");
        FileNode original = new FileNode("same", 12);
        DirectoryNode rejected = new DirectoryNode("same");
        parent.add(original);
        assertThrows(IllegalArgumentException.class, () -> parent.add(rejected));
        assertEquals(List.of(original), parent.children());
        DirectoryNode other = new DirectoryNode("other");
        other.add(rejected);
        assertEquals(List.of(rejected), other.children());
    }

    @Test
    void filesAndDirectoriesCannotHaveMultipleParents() {
        for (Node child : List.of(new FileNode("file", 1), new DirectoryNode("directory"))) {
            DirectoryNode first = new DirectoryNode("first");
            DirectoryNode second = new DirectoryNode("second");
            first.add(child);
            assertThrows(IllegalStateException.class, () -> second.add(child));
            assertThrows(IllegalStateException.class, () -> first.add(child));
            assertEquals(List.of(child), first.children());
            assertEquals(List.of(), second.children());
        }
    }

    @Test
    void selfAndAncestorCyclesAreRejectedWithoutChangingTheTree() {
        DirectoryNode root = new DirectoryNode("root");
        DirectoryNode child = new DirectoryNode("child");
        DirectoryNode grandchild = new DirectoryNode("grandchild");
        root.add(child);
        child.add(grandchild);
        assertThrows(IllegalArgumentException.class, () -> root.add(root));
        assertThrows(IllegalArgumentException.class, () -> grandchild.add(root));
        assertThrows(IllegalArgumentException.class, () -> grandchild.add(child));
        assertEquals(List.of(child), root.children());
        assertEquals(List.of(grandchild), child.children());
        assertEquals(List.of(), grandchild.children());
        new DirectoryNode("wrapper").add(root);
    }

    @Test
    void detachAllowsReparentingTheWholeSubtree() {
        DirectoryNode first = new DirectoryNode("first");
        DirectoryNode second = new DirectoryNode("second");
        DirectoryNode subtree = new DirectoryNode("subtree");
        subtree.add(new FileNode("retained", 8));
        first.add(subtree);
        assertSame(subtree, first.remove("subtree"));
        second.add(subtree);
        assertEquals(List.of(), first.children());
        assertEquals(List.of(subtree), second.children());
        assertEquals("retained", subtree.children().get(0).name());
    }

    @Test
    void childCollectionsAreImmutableSnapshots() {
        DirectoryNode root = new DirectoryNode("root");
        FileNode first = new FileNode("first", 1);
        root.add(first);
        List<Node> snapshot = root.children();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new FileNode("injected", 0)));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        root.add(new FileNode("second", 2));
        assertEquals(List.of(first), snapshot);
        assertEquals(2, root.children().size());
    }

    @Test
    void sameFilenameInDifferentDirectoriesIsAllowed() {
        DirectoryNode root = new DirectoryNode("root");
        DirectoryNode first = new DirectoryNode("first");
        DirectoryNode second = new DirectoryNode("second");
        first.add(new FileNode("same.txt", 1));
        second.add(new FileNode("same.txt", 2));
        root.add(first);
        root.add(second);
        assertEquals(2, root.children().size());
    }

    @Test
    void nullAndMissingChildOperationsFailPredictably() {
        DirectoryNode root = new DirectoryNode("root");
        assertThrows(NullPointerException.class, () -> new FileNode(null, 0));
        assertThrows(NullPointerException.class, () -> new DirectoryNode(null));
        assertThrows(NullPointerException.class, () -> root.add(null));
        assertThrows(NullPointerException.class, () -> root.remove(null));
        assertThrows(NoSuchElementException.class, () -> root.remove("missing"));
        root.add(new FileNode("valid", 0));
        assertEquals(1, root.children().size());
    }
}

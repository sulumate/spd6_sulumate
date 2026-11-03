package kz.aitu.filesystem;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.Element;
import kz.aitu.filesystem.model.FileNode;
import kz.aitu.filesystem.traversal.TreeWalker;
import kz.aitu.filesystem.visitor.AbstractVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatchTest {
    @Test
    void acceptSelectsEachMatchingOverloadWithoutTraversingChildren() {
        Element file = new FileNode("note.txt", 9);
        DirectoryNode directory = new DirectoryNode("notes");
        directory.add(new FileNode("child.txt", 12));
        Element folder = directory;
        RecordingVisitor visitor = new RecordingVisitor();
        file.accept(visitor);
        folder.accept(visitor);
        assertEquals(List.of("file:note.txt", "directory:notes"), visitor.result());
    }

    @Test
    void walkerVisitsEachReachableObjectOnceIncludingEmptyDirectories() {
        RecordingVisitor visitor = new RecordingVisitor();
        List<String> visits = new TreeWalker().walk(TestTrees.archive(), visitor);
        assertEquals(8, visits.size());
        assertEquals(8, visitor.identities.size());
        assertEquals("directory:archive", visits.get(0));
        assertTrue(visits.contains("directory:empty"));
    }

    private static final class RecordingVisitor extends AbstractVisitor<List<String>> {
        private final List<String> calls = new ArrayList<>();
        private final Set<Element> identities = Collections.newSetFromMap(new IdentityHashMap<>());

        @Override
        protected void resetResult() {
            calls.clear();
            identities.clear();
        }

        @Override
        public void visit(FileNode file) {
            calls.add("file:" + file.name());
            assertTrue(identities.add(file));
        }

        @Override
        public void visit(DirectoryNode directory) {
            calls.add("directory:" + directory.name());
            assertTrue(identities.add(directory));
        }

        @Override
        public List<String> result() {
            return List.copyOf(calls);
        }
    }
}

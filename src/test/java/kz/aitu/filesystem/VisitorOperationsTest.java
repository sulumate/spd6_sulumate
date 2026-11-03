package kz.aitu.filesystem;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.model.FileNode;
import kz.aitu.filesystem.traversal.TreeWalker;
import kz.aitu.filesystem.visitor.SearchVisitor;
import kz.aitu.filesystem.visitor.Statistics;
import kz.aitu.filesystem.visitor.StatisticsVisitor;
import kz.aitu.filesystem.visitor.TotalSizeVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisitorOperationsTest {
    private final TreeWalker walker = new TreeWalker();

    @Test
    void totalSizeIncludesAllNestedFilesExactlyOnce() {
        assertEquals(650L, walker.walk(TestTrees.archive(), new TotalSizeVisitor()));
    }

    @Test
    void chosenSubtreeExcludesFilesOutsideThatSubtree() {
        DirectoryNode subtree = new DirectoryNode("nested");
        subtree.add(new FileNode("inside.bin", 75));
        DirectoryNode root = new DirectoryNode("root");
        root.add(new FileNode("outside.bin", 900));
        root.add(subtree);
        assertEquals(75L, walker.walk(subtree, new TotalSizeVisitor()));
    }

    @Test
    void searchCombinesFilenameAndInclusiveMinimumSize() {
        SearchVisitor visitor = new SearchVisitor(name -> name.endsWith(".txt"), 100);
        assertEquals(List.of("docs/a.txt", "docs/deep/c.txt"), walker.walk(TestTrees.archive(), visitor));
    }

    @Test
    void searchCriterionCanBeReconfiguredBeyondSuffixMatching() {
        SearchVisitor visitor = new SearchVisitor(name -> name.startsWith("b"), 150);
        assertEquals(List.of("docs/b.md"), walker.walk(TestTrees.archive(), visitor));
    }

    @Test
    void pathsAreSortedRegardlessOfInsertionOrder() {
        SearchVisitor visitor = new SearchVisitor(name -> true, 0);
        assertEquals(List.of("docs/a.txt", "docs/b.md", "docs/deep/c.txt", "z.txt"),
                walker.walk(TestTrees.archive(), visitor));
    }

    @Test
    void pathsAreRelativeToChosenRootEvenWhenItHasAParent() {
        DirectoryNode parent = new DirectoryNode("parent");
        DirectoryNode chosen = new DirectoryNode("chosen");
        DirectoryNode nested = new DirectoryNode("nested");
        nested.add(new FileNode("answer.txt", 42));
        chosen.add(nested);
        parent.add(chosen);
        assertEquals(List.of("nested/answer.txt"),
                walker.walk(chosen, new SearchVisitor(name -> true, 0)));
        assertEquals(new Statistics(1, 2, 2), walker.walk(chosen, new StatisticsVisitor()));
    }

    @Test
    void statisticsIncludeDirectoryRootAndAllElementDepths() {
        assertEquals(new Statistics(4, 4, 3), walker.walk(TestTrees.archive(), new StatisticsVisitor()));
    }

    @Test
    void deepestEmptyDirectoryAlsoDeterminesMaximumDepth() {
        DirectoryNode root = new DirectoryNode("root");
        DirectoryNode first = new DirectoryNode("first");
        first.add(new DirectoryNode("last"));
        root.add(first);
        assertEquals(new Statistics(0, 3, 2), walker.walk(root, new StatisticsVisitor()));
    }

    @Test
    void emptyDirectoryHasZeroBytesNoMatchesAndDepthZero() {
        DirectoryNode root = new DirectoryNode("empty");
        assertEquals(0L, walker.walk(root, new TotalSizeVisitor()));
        assertEquals(List.of(), walker.walk(root, new SearchVisitor(name -> true, 0)));
        assertEquals(new Statistics(0, 1, 0), walker.walk(root, new StatisticsVisitor()));
    }

    @Test
    void nonemptyTreeCanProduceNoSearchMatches() {
        assertEquals(List.of(), walker.walk(TestTrees.archive(), new SearchVisitor(name -> false, 0)));
        assertEquals(List.of(), walker.walk(TestTrees.archive(), new SearchVisitor(name -> true, 301)));
    }

    @Test
    void standaloneFileIsAValidRoot() {
        FileNode root = new FileNode("solo.txt", 20);
        assertEquals(20L, walker.walk(root, new TotalSizeVisitor()));
        assertEquals(List.of("solo.txt"), walker.walk(root, new SearchVisitor(name -> true, 20)));
        assertEquals(new Statistics(1, 0, 0), walker.walk(root, new StatisticsVisitor()));
    }

    @Test
    void zeroByteFileMatchesZeroThreshold() {
        assertEquals(List.of("zero"), walker.walk(new FileNode("zero", 0), new SearchVisitor(name -> true, 0)));
    }

    @Test
    void reusedVisitorsResetAcrossRunsAndReturnIndependentSnapshots() {
        DirectoryNode root = TestTrees.archive();
        TotalSizeVisitor size = new TotalSizeVisitor();
        SearchVisitor search = new SearchVisitor(name -> true, 0);
        StatisticsVisitor statistics = new StatisticsVisitor();
        List<String> oldPaths = walker.walk(root, search);
        Statistics oldStatistics = walker.walk(root, statistics);
        assertEquals(650L, walker.walk(root, size));
        assertEquals(650L, walker.walk(root, size));
        assertEquals(oldPaths, walker.walk(root, search));
        assertEquals(oldStatistics, walker.walk(root, statistics));
        root.add(new FileNode("new.bin", 7));
        assertEquals(657L, walker.walk(root, size));
        assertEquals(5, walker.walk(root, search).size());
        assertEquals(new Statistics(5, 4, 3), walker.walk(root, statistics));
        assertEquals(4, oldPaths.size());
        assertEquals(new Statistics(4, 4, 3), oldStatistics);
        DirectoryNode empty = new DirectoryNode("fresh");
        assertEquals(List.of(), walker.walk(empty, search));
        assertEquals(0L, walker.walk(empty, size));
        assertEquals(new Statistics(0, 1, 0), walker.walk(empty, statistics));
    }

    @Test
    void searchResultsAreImmutable() {
        List<String> result = walker.walk(TestTrees.archive(), new SearchVisitor(name -> true, 0));
        assertThrows(UnsupportedOperationException.class, () -> result.add("injected.txt"));
    }

    @Test
    void failedRunDoesNotContaminateNextExecution() {
        SearchVisitor search = new SearchVisitor(name -> {
            if (name.equals("bad")) {
                throw new IllegalArgumentException("Rejected by test predicate");
            }
            return true;
        }, 0);
        DirectoryNode root = new DirectoryNode("root");
        root.add(new FileNode("good", 1));
        root.add(new FileNode("bad", 1));
        assertThrows(IllegalArgumentException.class, () -> walker.walk(root, search));
        assertEquals(List.of("next"), walker.walk(new FileNode("next", 2), search));
    }

    @Test
    void totalSizeOverflowFailsExplicitlyAndNextRunResets() {
        DirectoryNode root = new DirectoryNode("large");
        root.add(new FileNode("first", Long.MAX_VALUE));
        root.add(new FileNode("second", 1));
        TotalSizeVisitor size = new TotalSizeVisitor();
        assertThrows(ArithmeticException.class, () -> walker.walk(root, size));
        assertEquals(0L, walker.walk(new DirectoryNode("empty"), size));
    }

    @Test
    void deepNestingUsesAnExplicitStack() {
        DirectoryNode root = new DirectoryNode("root");
        DirectoryNode current = root;
        for (int depth = 0; depth < 2000; depth++) {
            DirectoryNode next = new DirectoryNode("d");
            current.add(next);
            current = next;
        }
        current.add(new FileNode("end", 17));
        assertEquals(new Statistics(1, 2001, 2001), walker.walk(root, new StatisticsVisitor()));
        assertEquals(17L, walker.walk(root, new TotalSizeVisitor()));
    }

    @Test
    void invalidOperationArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new SearchVisitor(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new SearchVisitor(name -> true, -1));
        assertThrows(NullPointerException.class, () -> walker.walk(null, new TotalSizeVisitor()));
        assertThrows(NullPointerException.class, () -> walker.walk(new DirectoryNode("root"), null));
        assertThrows(NullPointerException.class, () -> new FileNode("file", 0).accept(null));
        assertThrows(NullPointerException.class, () -> new DirectoryNode("root").accept(null));
    }
}

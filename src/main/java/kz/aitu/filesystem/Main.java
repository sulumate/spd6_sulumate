package kz.aitu.filesystem;

import kz.aitu.filesystem.model.DirectoryNode;
import kz.aitu.filesystem.traversal.TreeWalker;
import kz.aitu.filesystem.visitor.SearchVisitor;
import kz.aitu.filesystem.visitor.Statistics;
import kz.aitu.filesystem.visitor.StatisticsVisitor;
import kz.aitu.filesystem.visitor.TotalSizeVisitor;

import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            throw new IllegalArgumentException("Usage: java -jar visitor-filesystem-1.0.0.jar [suffix] [minimumBytes]");
        }
        String suffix = args.length > 0 ? args[0] : ".txt";
        long minimumBytes = args.length > 1 ? Long.parseLong(args[1]) : 256;
        SearchVisitor search = new SearchVisitor(name -> name.endsWith(suffix), minimumBytes);
        DirectoryNode root = SampleFileSystem.create();
        TreeWalker walker = new TreeWalker();
        long totalBytes = walker.walk(root, new TotalSizeVisitor());
        List<String> matches = walker.walk(root, search);
        Statistics statistics = walker.walk(root, new StatisticsVisitor());
        System.out.println("Root: " + root.name());
        System.out.println("Total size: " + totalBytes + " bytes");
        System.out.println("Search: suffix=" + suffix + ", minimum=" + minimumBytes + " bytes");
        matches.forEach(path -> System.out.println("  " + path));
        System.out.println("Files: " + statistics.fileCount());
        System.out.println("Directories: " + statistics.directoryCount());
        System.out.println("Maximum depth: " + statistics.maximumDepth());
        System.out.println("Repeated search unchanged: " + matches.equals(walker.walk(root, search)));
    }
}

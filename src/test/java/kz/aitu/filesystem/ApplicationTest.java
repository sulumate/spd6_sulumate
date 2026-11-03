package kz.aitu.filesystem;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationTest {
    @Test
    void defaultDemoProducesKnownOutput() {
        assertEquals("""
                Root: workspace
                Total size: 12160 bytes
                Search: suffix=.txt, minimum=256 bytes
                  docs/drafts/notes.txt
                  docs/guide.txt
                Files: 7
                Directories: 5
                Maximum depth: 3
                Repeated search unchanged: true
                """, runApplication());
    }

    @Test
    void changedConfigurationProducesDifferentMatches() {
        assertEquals("""
                Root: workspace
                Total size: 12160 bytes
                Search: suffix=.md, minimum=128 bytes
                  README.md
                  docs/drafts/todo.md
                Files: 7
                Directories: 5
                Maximum depth: 3
                Repeated search unchanged: true
                """, runApplication(".md", "128"));
    }

    @Test
    void malformedArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{".txt", "1", "extra"}));
        assertThrows(NumberFormatException.class, () -> Main.main(new String[]{".txt", "abc"}));
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{".txt", "-1"}));
    }

    private String runApplication(String... arguments) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            Main.main(arguments);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}

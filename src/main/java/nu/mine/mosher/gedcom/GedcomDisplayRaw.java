package nu.mine.mosher.gedcom;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Minimally parse (being liberal in what we allow) an input
 * GEDCOM file (stdin), and (somehow) format it nicely for display (in
 * as raw a form as possible).
 * <p>
 * Input GEDCOM file is assumed UTF-8.
 */
class GedcomDisplayRaw {
    private static final int INDENTATION = 5;

    public static void main(final String... args) throws IOException {
        System.setProperty("jansi.force", Boolean.TRUE.toString());
        AnsiConsole.systemInstall();

        try (final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(FileDescriptor.in), StandardCharsets.UTF_8))) {
            final AtomicInteger plev = new AtomicInteger(-1);
            in.lines().map(line -> {
                try {
                    final ParsedLine p = new ParsedLine(line, plev.get());
                    final int nextLevel = p.getLevel();
                    if (nextLevel >= 0) {
                        plev.set(nextLevel);
                    }
                    return p.toAnsiString(INDENTATION);
                } catch (final Throwable e) {
                    return ansi().bg(RED).a(line).reset().toString();
                }
            }).forEachOrdered(System.out::println);
        }

        System.out.flush();
        System.err.flush();

        AnsiConsole.systemUninstall();
    }
}

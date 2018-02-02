package nu.mine.mosher.gedcom;

import org.fusesource.jansi.Ansi;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

class ParsedLine {
    /*
        Break down the line into all the sections we're interested in.
        The only thing _required_ is at least one digit (which represents the level).
         */
    private static String PATTERN_STRING =
        "(?<space0>\\s*)" +
        "(?<level>\\d+)" +
        "(?<space1>\\s*)" +
        "(?<tag>\\S*)" +
        "(?<space2> ?)" +
        "(?<space3>\\s*)" +
        "(?<value>.*)";

    private static Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    private static final int MAX_INDENTATION = 20;
    private static final String INDENTER = " ";
    private static final int INVALID_LEVEL = -9999;



    private final String unparsed;

    private final boolean blank;
    private final boolean allwhite;
    private final boolean matches;

    private final String space0;
    private final String space1;
    private final String space2;
    private final String space3;
    private final String space4;

    private final int expectedPreviousLevel;

    private final int level;
    private final String tag;
    private final String value;



    public ParsedLine(final String line, final int currentParentLevel) {
        this.expectedPreviousLevel = currentParentLevel;

        final String s = Optional.ofNullable(line).orElse("");
        this.unparsed = s;

        this.blank = s.isEmpty();
        this.allwhite = s.trim().isEmpty();



        final Matcher m = PATTERN.matcher(s);

        this.matches = m.matches();

        if (this.matches) {
            this.space0 = m.group("space0");
            this.level = asInt(m.group("level"));
            this.space1 = m.group("space1");
            this.tag = m.group("tag");
            this.space2 = m.group("space2");
            this.space3 = m.group("space3");
            final String v = m.group("value");
            assert !v.startsWith(" ");

            final int vlen = v.trim().length();
            final int trailing = v.length() - vlen;
            if (trailing <= 0) {
                this.value = v;
                this.space4 = "";
            } else {
                /* split out trailing whitespace */
                /* TODO: why can't the regex match the trailing whitespace? */
                this.value = v.substring(0,vlen);
                this.space4 = v.substring(vlen);
            }
        } else {
            this.space0 = "";
            this.level = INVALID_LEVEL;
            this.space1 = "";
            this.tag = "";
            this.space2 = "";
            this.space3 = "";
            this.value = "";
            this.space4 = "";
        }
    }



    private static int asInt(final String level) {
        try {
            return Integer.parseInt(level);
        } catch (final Throwable ignore) {
            return INVALID_LEVEL;
        }
    }

    public String toAnsiString(final int indentation) {
        Ansi a = ansi();

        a = a.a(safeIndent(indentation));

        if (this.matches && levelIsValid()) {
            a = aSpace0(a);
            a = aLevel(a);
            a = aSpace1(a);
            a = aTag(a);
            a = aSpace2(a);
            a = aSpace3(a);
            a = aValue(a);
            a = aSpace4(a);
        } else {
            a = a.bg(RED).a(this.unparsed);
        }

        return a.reset().toString();
    }

    private Ansi aValue(Ansi a) {
        a = a.a(this.value);
        return a.reset();
    }

    private Ansi aTag(Ansi a) {
        a = a.bold();
        if (this.tag.startsWith("_")) {
            a = a.fg(GREEN);
        } else {
            a = a.fg(MAGENTA);
        }
        a = a.a(this.tag);
        return a.reset();
    }

    private Ansi aLevel(Ansi a) {
        a = a.fg(CYAN);
        a = a.a(this.level);
        return a.reset();
    }

    private Ansi aSpace0(Ansi a) {
        if (!this.space0.isEmpty()) {
            a = a.bg(RED);
        }
        a = a.a(this.space0);
        return a.reset();
    }
    private Ansi aSpace1(Ansi a) {
        if (this.space1.length() != 1) {
            a = a.bg(RED);
        }
        a = a.a(this.space1);
        return a.reset();
    }
    private Ansi aSpace2(Ansi a) {
        if (this.space2.length() != 1) {
            a = a.bg(RED);
        }
        a = a.a(this.space2);
        return a.reset();
    }
    private Ansi aSpace3(Ansi a) {
        if (!this.space3.isEmpty()) {
            a = a.bg(RED);
        }
        a = a.a(this.space3);
        return a.reset();
    }
    private Ansi aSpace4(Ansi a) {
        if (!this.space4.isEmpty()) {
            a = a.bg(RED);
        }
        a = a.a(this.space4);
        return a.reset();
    }

    private String safeIndent(int ind) {
        int lev = this.level;
        if (lev < 0) {
            lev = this.expectedPreviousLevel;
        }
        ind *= lev;
        ind = max(0, min(ind, MAX_INDENTATION));
        return String.join("", Collections.nCopies(ind, INDENTER));
    }

    public boolean levelIsValid() {
        return 0 <= this.level && this.level <= this.expectedPreviousLevel+1;
    }

    public int getLevel() {
        return this.level;
    }
}

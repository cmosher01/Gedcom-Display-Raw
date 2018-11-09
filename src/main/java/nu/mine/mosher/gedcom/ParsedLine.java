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

    private static String PATTERN_STRING2 =
        "(?<space0>\\s*)" +
        "(?<level>\\d+)" +
        "(?<space1>\\s*)" +
        "(?<id>@\\S*?@)" +
        "(?<spaceid>\\s*)" +
        "(?<tag>\\S*)" +
        "(?<space2> ?)" +
        "(?<space3>\\s*)" +
        "(?<value>.*)";

    private static Pattern PATTERN2 = Pattern.compile(PATTERN_STRING2);

    private static final int MAX_INDENTATION = 45;
    private static final String INDENTER = " ";
    private static final int INVALID_LEVEL = -9999;



    private final String unparsed;

    private final boolean blank;
    private final boolean allwhite;
    private final boolean matches;

    private final String space0;
    private final String space1;
    private final String spaceid;
    private final String space2;
    private final String space3;
    private final String space4;

    private final int expectedPreviousLevel;

    private final int level;
    private final String tag;
    private final String value;
    private final String id;
    private final boolean valueIsPointer;
    private final boolean gedcom551tag;



    public ParsedLine(final String line, final int currentParentLevel) {
        this.expectedPreviousLevel = currentParentLevel;

        final String s = Optional.ofNullable(line).orElse("");
        this.unparsed = s;

        this.blank = s.isEmpty();
        this.allwhite = s.trim().isEmpty();



        final Matcher m = PATTERN.matcher(s);
        final Matcher m2 = PATTERN2.matcher(s);

        this.matches = m.matches() | m2.matches();

        if (this.matches) {
            this.space0 = m.group("space0");
            this.level = asInt(m.group("level"));
            this.space1 = m.group("space1");
            final String tagOrId = m.group("tag");
            if (tagOrId.startsWith("@") && tagOrId.endsWith("@") && ! tagOrId.contains("@@")) {
                /* it's an ID */
                /* this should have matched pattern2 */

                this.id = m2.group("id");
                this.spaceid = m2.group("spaceid");

                this.tag = m2.group("tag");
                this.space2 = m2.group("space2");
                this.space3 = m2.group("space3");

                final String v = m2.group("value");
                assert !v.startsWith(" ");

                final int tlen = v.trim().length();
                if (tlen <= v.length()) {
                    /* split out trailing whitespace */
                    this.value = v.substring(0,tlen);
                    this.space4 = v.substring(tlen);
                } else {
                    this.value = v;
                    this.space4 = "";
                }
            } else {
                /* it's a tag */

                this.id = "";
                this.spaceid = "";

                this.tag = m.group("tag");
                this.space2 = m.group("space2");
                this.space3 = m.group("space3");

                final String v = m.group("value");
                assert !v.startsWith(" ");

                final int tlen = v.trim().length();
                if (tlen <= v.length()) {
                    /* split out trailing whitespace */
                    this.value = v.substring(0,tlen);
                    this.space4 = v.substring(tlen);
                } else {
                    this.value = v;
                    this.space4 = "";
                }
            }
        } else {
            this.space0 = "";
            this.level = INVALID_LEVEL;
            this.space1 = "";
            this.id = "";
            this.spaceid = "";
            this.tag = "";
            this.space2 = "";
            this.space3 = "";
            this.value = "";
            this.space4 = "";
        }

        this.valueIsPointer =  this.value.startsWith("@") && this.value.endsWith("@") && !this.value.contains("@@");
        this.gedcom551tag = Gedcom551Tag.okTag(this.tag);
    }



    private static int asInt(final String level) {
        try {
            return Integer.parseInt(level);
        } catch (final Throwable ignore) {
            return INVALID_LEVEL;
        }
    }

    public String toAnsiString(final int indentation) {
        final Ansi a = ansi();

        a.a(safeIndent(indentation));

        if (this.blank) {
            a.bg(RED).a("[BLANK-LINE]");
        } else if (this.matches && levelIsValid()) {
            aSpace0(a);
            aLevel(a);
            aSpace1(a);
            aId(a);
            aSpaceid(a);
            aTag(a);
            aSpace2(a);
            aSpace3(a);
            aValue(a);
            aSpace4(a);
        } else {
            a.bg(RED).a(this.unparsed);
        }

        return a.reset().toString();
    }

    private void aId(Ansi a) {
        a.fg(YELLOW).a(this.id).reset();
    }

    private void aValue(Ansi a) {
        if (this.valueIsPointer) {
            a.fg(YELLOW).a(this.value);
        } else {
            hiliteNonAscii(a, this.value);
        }
        a.reset();
    }

    private static void hiliteNonAscii(Ansi a, final String s) {
        s.codePoints().forEach(c -> {
            if (/*c==' ' ||*/ c=='"' || c=='\'' || c=='-' || c=='@' || c=='\\' || c=='^' || c=='`' || c=='|' || c=='~') {
                a.fg(BLACK).bg(YELLOW).a((char)c).reset();
            } else if (' ' <= c && c <= '~') {
                a.a((char) c);
            } else {
                a.fg(BLACK).bg(GREEN).a("<").a(Character.getName(c)).a(">").reset();
            }
        });
        a.reset();
    }

    private void aTag(Ansi a) {
        a.bold();
        if (this.tag.startsWith("_")) {
            a.fg(GREEN);
        } else if (this.gedcom551tag) {
            a.fg(MAGENTA);
        } else if (this.tag.isEmpty()) {
            a.bg(RED).a("[MISSING TAG]");
        } else {
            a.bg(RED);
        }
        a.a(this.tag).reset();
    }

    private void aLevel(Ansi a) {
        a.fg(CYAN).a(this.level).reset();
    }

    private void aSpace0(Ansi a) {
        if (!this.space0.isEmpty()) {
            a.bg(RED);
        }
        a.a(this.space0).reset();
    }
    private void aSpace1(Ansi a) {
        if (this.space1.length() != 1) {
            a.bg(RED);
        }
        if (this.space1.isEmpty()) {
            a.a("[MISSING-SPACE]");
        }
        a.a(this.space1).reset();
    }
    private void aSpaceid(Ansi a) {
        if (this.spaceid.length() != 1) {
            a.bg(RED);
        }
        if (!this.id.isEmpty() && this.spaceid.isEmpty()) {
            a.a("[MISSING-SPACE]");
        }
        a.a(this.spaceid).reset();
    }
    private void aSpace2(Ansi a) {
        if (this.space2.length() != 1 || this.value.isEmpty()) {
            a.bg(RED);
        }
        a.a(this.space2).reset();
    }
    private void aSpace3(Ansi a) {
        if (!this.space3.isEmpty()) {
            a.bg(RED);
        }
        a.a(this.space3).reset();
    }
    private void aSpace4(Ansi a) {
        if (!this.space4.isEmpty()) {
            a.bg(RED);
        }
        a.a(this.space4).reset();
    }

    public String safeIndent(int ind) {
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


    private enum Gedcom551Tag {
        ABBR,
        ADDR,
        ADR1,
        ADR2,
        ADR3,
        ADOP,
        AFN,
        AGE,
        AGNC,
        ALIA,
        ANCE,
        ANCI,
        ANUL,
        ASSO,
        AUTH,
        BAPL,
        BAPM,
        BARM,
        BASM,
        BIRT,
        BLES,
        BURI,
        CALN,
        CAST,
        CAUS,
        CENS,
        CHAN,
        CHAR,
        CHIL,
        CHR,
        CHRA,
        CITY,
        CONC,
        CONF,
        CONL,
        CONT,
        COPR,
        CORP,
        CREM,
        CTRY,
        DATA,
        DATE,
        DEAT,
        DESC,
        DESI,
        DEST,
        DIV,
        DIVF,
        DSCR,
        EDUC,
        EMAIL,
        EMIG,
        ENDL,
        ENGA,
        EVEN,
        FACT,
        FAM,
        FAMC,
        FAMF,
        FAMS,
        FAX,
        FCOM,
        FILE,
        FONE,
        FORM,
        GEDC,
        GIVN,
        GRAD,
        HEAD,
        HUSB,
        IDNO,
        IMMI,
        INDI,
        LANG,
        LATI,
        LONG,
        MAP,
        MARB,
        MARC,
        MARL,
        MARR,
        MARS,
        MEDI,
        NAME,
        NATI,
        NATU,
        NCHI,
        NICK,
        NMR,
        NOTE,
        NPFX,
        NSFX,
        OBJE,
        OCCU,
        ORDI,
        ORDN,
        PAGE,
        PEDI,
        PHON,
        PLAC,
        POST,
        PROB,
        PROP,
        PUBL,
        QUAY,
        REFN,
        RELA,
        RELI,
        REPO,
        RESI,
        RESN,
        RETI,
        RFN,
        RIN,
        ROLE,
        ROMN,
        SEX,
        SLGC,
        SLGS,
        SOUR,
        SPFX,
        SSN,
        STAE,
        STAT,
        SUBM,
        SUBN,
        SURN,
        TEMP,
        TEXT,
        TIME,
        TITL,
        TRLR,
        TYPE,
        VERS,
        WIFE,
        WILL,
        WWW,;

        public static boolean okTag(final String value) {
            if (value.startsWith("_")) {
                return true;
            }
            try {
                valueOf(value);
                return true;
            } catch (final Throwable invalidTag) {
                return false;
            }
        }
    }
}

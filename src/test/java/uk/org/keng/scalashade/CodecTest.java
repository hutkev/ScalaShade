package uk.org.keng.scalashade;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class CodecTest {

    public static String compareStrings(String a, String b) {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        sb.append("First length: ").append(a.length()).append(nl);
        sb.append("Second length: ").append(b.length()).append(nl);
        for (int i = 0; i < Math.max(a.length(), b.length()); i++) {
            if (i < a.length() && i < b.length()) {
                if (a.charAt(i) != b.charAt(i)) {
                    sb.append("Differs at: ").append(i).append(" ").append((byte) a.charAt(i)).
                            append(" ").append((byte) b.charAt(i)).append(nl);
                }
            } else if (i < a.length()) {
                sb.append("Only in first: ").append(i).append(" ").append((byte) a.charAt(i)).append(nl);
            } else if (i < b.length()) {
                sb.append("Only in second: ").append(i).append(" ").append((byte) b.charAt(i)).append(nl);
            }
        }
        return sb.toString();
    }

    public static String compareBytes(byte[] a, byte[] b) {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        sb.append("First length: ").append(a.length).append(nl);
        sb.append("Second length: ").append(b.length).append(nl);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            if (i < a.length && i < b.length) {
                if (a[i] != b[i]) {
                    sb.append("Differs at: ").append(i).append(" ").append(a[i]).
                            append(" ").append(b[i]).append(nl);
                }
            } else if (i < a.length) {
                sb.append("Only in first: ").append(i).append(" ").append(a[i]).append(nl);
            } else if (i < b.length) {
                sb.append("Only in second: ").append(i).append(" ").append(b[i]).append(nl);
            }
        }
        return sb.toString();
    }

    @Test
    public void EmptyOk() {
        assertTrue(Encoding.isValidEncoding(""));
        assertTrue(Encoding.decode("").length==0);
        assertTrue(Encoding.encode(new byte[]{}).length()==0);
    }

    @Test
    public void ValidChars() {
        assertTrue(Encoding.isValidEncoding("\u0000"));
        assertTrue(Encoding.isValidEncoding("\u0001"));
        assertTrue(Encoding.isValidEncoding("\u007F"));
        assertFalse(Encoding.isValidEncoding("\u0080"));
        assertFalse(Encoding.isValidEncoding("\u00C0"));
        assertTrue(Encoding.isValidEncoding("\u00C0\u0080"));
        assertFalse(Encoding.isValidEncoding("\u00C0\u0081"));
        assertFalse(Encoding.isValidEncoding("\u00C0\u007F"));
        assertFalse(Encoding.isValidEncoding("\u00BF\u0080"));
        assertFalse(Encoding.isValidEncoding("\u00C1\u0080"));
    }

    public void Roundtrip() {
        assertTrue(Encoding.encode(Encoding.decode("\u0000")).equals("\u0000"));
        assertTrue(Encoding.encode(Encoding.decode("\u0001")).equals("\u0001"));
        assertTrue(Encoding.encode(Encoding.decode("\u007F")).equals("\u007F"));
        assertTrue(Encoding.decode("\u0080") == null);
        assertTrue(Encoding.decode("\u00C0") == null);
        assertTrue(Encoding.encode(Encoding.decode("\u00C0\u0080")).equals("\u00C0\u0080"));
        assertTrue(Encoding.decode("\u00C0\u0081") == null);
        assertTrue(Encoding.decode("\u00C0\u007F") == null);
        assertTrue(Encoding.decode("\u00BF\u0080") == null);
        assertTrue(Encoding.decode("\u00C1\u0080") == null);
    }

    @Test
    public void Sample() {

        String input = "\006\001\005\rc\001B\001\003\0015\021!b\025;sS:<G+\0379f\025\t\031A!A\003usB,7O\003\002\006\r\005\0311/\0357\013\005\035A\021!B:qCJ\\'BA\005\013\003\031\t\007/Y2iK*\t1\"A\002pe\036\034\001a\005\002\001\035A\021q\002E\007\002\005%\021\021C\001\002\013\003R|W.[2UsB,\007\"B\n\001\t\023!\022A\002\037j]&$h\bF\001\026!\ty\001!B\003\030\001\001!\001D\001\007J]R,'O\\1m)f\004X\r\005\002\032;5\t!D\003\002\0047)\021ADB\001\007k:\034\030MZ3\n\005yQ\"AC+U\rb\032FO]5oO\"I\001\005\001EC\002\023\005A!I\001\004i\006<W#\001\022\021\007\rJtH\004\002%m9\021Qe\r\b\003MAr!aJ\027\017\005!ZS\"A\025\013\005)b\021A\002\037s_>$h(C\001-\003\025\0318-\0317b\023\tqs&A\004sK\032dWm\031;\013\0031J!!\r\032\002\017I,h\016^5nK*\021afL\005\003iU\nq\001]1dW\006<WM\003\0022e%\021q\007O\001\tk:Lg/\032:tK*\021A'N\005\003um\022q\001V=qKR\013w-\003\002={\tAA+\0379f)\006<7O\003\002?e\005\031\021\r]5\021\005\0013R\"\001\001\t\021\t\003\001\022!Q!\n\t\nA\001^1hA!\022\021\t\022\t\003\013\032k\021aL\005\003\017>\022\021\002\036:b]NLWM\034;\t\021%\003!\031!C\001\t)\013\001b\034:eKJLgnZ\013\002\027B\031AjT \016\0035S!AT\030\002\t5\fG\017[\005\003!6\023\001b\024:eKJLgn\032\005\007%\002\001\013\021B&\002\023=\024H-\032:j]\036\004\003\"\002+\001\t\003*\026a\0033fM\006,H\016^*ju\026,\022A\026\t\003\013^K!\001W\030\003\007%sG\017\003\004[\001\021\005caW\001\013CNtU\017\0347bE2,W#A\013)\005\001i\006C\0010b\033\005y&B\0011\007\003)\tgN\\8uCRLwN\\\005\003E~\023A\002R3wK2|\007/\032:Ba&<Q\001\032\002\t\002\026\f!b\025;sS:<G+\0379f!\tyaMB\003\002\005!\005um\005\003g+!\\\007CA#j\023\tQwFA\004Qe>$Wo\031;\021\005\025c\027BA70\0051\031VM]5bY&T\030M\0317f\021\025\031b\r\"\001p)\005)\007bB9g\003\003%\tE]\001\016aJ|G-^2u!J,g-\033=\026\003M\004\"\001^=\016\003UT!A^<\002\t1\fgn\032\006\002q\006!!.\031<b\023\tQXO\001\004TiJLgn\032\005\by\032\f\t\021\"\001V\0031\001(o\0343vGR\f%/\033;z\021\035qh-!A\005\002}\fa\002\035:pIV\034G/\0227f[\026tG\017\006\003\002\002\005\035\001cA#\002\004%\031\021QA\030\003\007\005s\027\020\003\005\002\nu\f\t\0211\001W\003\rAH%\r\005\n\003\0331\027\021!C!\003\037\tq\002\035:pIV\034G/\023;fe\006$xN]\013\003\003#\001b!a\005\002\032\005\005QBAA\013\025\r\t9bL\001\013G>dG.Z2uS>t\027\002BA\016\003+\021\001\"\023;fe\006$xN\035\005\n\003?1\027\021!C\001\003C\t\001bY1o\013F,\030\r\034\013\005\003G\tI\003E\002F\003KI1!a\n0\005\035\021un\0347fC:D!\"!\003\002\036\005\005\t\031AA\001\021%\tiCZA\001\n\003\ny#\001\005iCND7i\0343f)\0051\006\"CA\032M\006\005I\021IA\033\003!!xn\025;sS:<G#A:\t\023\005eb-!A\005\n\005m\022a\003:fC\022\024Vm]8mm\026$\"!!\020\021\007Q\fy$C\002\002BU\024aa\0242kK\016$\b";
        byte[] decoded = Encoding.decode(input);
        assertTrue(decoded.length == 1047);
        String encoded = Encoding.encode(decoded);
        if (!encoded.equals(input)) {
            System.err.println(compareStrings(input, encoded));
            assertTrue(false);
        }
    }

/*
    @Test
    public void newCodec() {
        byte[] foo = new byte[] {0,0,0,0,0,0,0,0,0,0,0,0};
        for (int pos=0; pos<9; pos++) {
            for (int i = 0; i < 255; i++) {
                byte b = (byte) i;
                Encoding.encodeByte(foo, pos, b);
                //byte[] bar = Encoding.encode8to7(foo);
                if (Encoding.decodeByte(foo, pos) != b)
                    assertTrue(false);
            }
        }
    }
    */
}

package uk.org.keng.scalashade;

import org.junit.Test;

import java.util.Arrays;

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
                    sb.append("Differs at: ").append(i).append(" ").append((int) a.charAt(i)).
                            append(" ").append((int) b.charAt(i)).append(nl);
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

    @SuppressWarnings("ConstantConditions")
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
        assertFalse(Encoding.isValidEncoding("\u00C0\u0080"));
        assertFalse(Encoding.isValidEncoding("\u00C0\u0081"));
        assertFalse(Encoding.isValidEncoding("\u00C0\u007F"));
        assertFalse(Encoding.isValidEncoding("\u00BF\u0080"));
        assertFalse(Encoding.isValidEncoding("\u00C1\u0080"));
    }

    @Test
    public void ValidDecoding() {
        assertTrue(Arrays.equals(Encoding.decode("\u0000\u0001"), new byte[] {0x7F}));
        assertTrue(Arrays.equals(Encoding.decode("\u0001\u0001"), new byte[] {0x0}));
        assertTrue(Arrays.equals(Encoding.decode("\u0002\u0001"), new byte[] {0x1}));
        assertTrue(Arrays.equals(Encoding.decode("\u0010\u0001"), new byte[] {0xF}));
    }

    @Test
    public void ValidEncoding() {
        assertTrue("\u0000\u0001".equals(Encoding.encode(new byte[] {0x7F})));
        assertTrue("\u0001\u0001".equals(Encoding.encode(new byte[] {0x0})));
        assertTrue("\u0002\u0001".equals(Encoding.encode(new byte[] {0x1})));
        assertTrue("\u0010\u0001".equals(Encoding.encode(new byte[] {0xF})));
    }

    @Test
    public void Roundtrip() {
        assertTrue(Encoding.encode(Encoding.decode("\u0000\u0001")).equals("\u0000\u0001"));
        assertTrue(Encoding.encode(Encoding.decode("\u0001\u0001")).equals("\u0001\u0001"));
        assertTrue(Encoding.encode(Encoding.decode("\u007F\u0001")).equals("\u007F\u0001"));
        assertTrue(Encoding.decode("\u0080") == null);
        assertTrue(Encoding.decode("\u00C0") == null);
        assertTrue(Encoding.decode("\u00C0\u0080") == null);
    }

    @Test
    public void Sample1() {
        String input = "\006\001\005\rc\001B\001\003\0015\021!b\025;sS:<G+\0379f\025\t\031A!A\003usB,7O\003\002\006\r\005\0311/\0357\013\005\035A\021!B:qCJ\\'BA\005\013\003\031\t\007/Y2iK*\t1\"A\002pe\036\034\001a\005\002\001\035A\021q\002E\007\002\005%\021\021C\001\002\013\003R|W.[2UsB,\007\"B\n\001\t\023!\022A\002\037j]&$h\bF\001\026!\ty\001!B\003\030\001\001!\001D\001\007J]R,'O\\1m)f\004X\r\005\002\032;5\t!D\003\002\0047)\021ADB\001\007k:\034\030MZ3\n\005yQ\"AC+U\rb\032FO]5oO\"I\001\005\001EC\002\023\005A!I\001\004i\006<W#\001\022\021\007\rJtH\004\002%m9\021Qe\r\b\003MAr!aJ\027\017\005!ZS\"A\025\013\005)b\021A\002\037s_>$h(C\001-\003\025\0318-\0317b\023\tqs&A\004sK\032dWm\031;\013\0031J!!\r\032\002\017I,h\016^5nK*\021afL\005\003iU\nq\001]1dW\006<WM\003\0022e%\021q\007O\001\tk:Lg/\032:tK*\021A'N\005\003um\022q\001V=qKR\013w-\003\002={\tAA+\0379f)\006<7O\003\002?e\005\031\021\r]5\021\005\0013R\"\001\001\t\021\t\003\001\022!Q!\n\t\nA\001^1hA!\022\021\t\022\t\003\013\032k\021aL\005\003\017>\022\021\002\036:b]NLWM\034;\t\021%\003!\031!C\001\t)\013\001b\034:eKJLgnZ\013\002\027B\031AjT \016\0035S!AT\030\002\t5\fG\017[\005\003!6\023\001b\024:eKJLgn\032\005\007%\002\001\013\021B&\002\023=\024H-\032:j]\036\004\003\"\002+\001\t\003*\026a\0033fM\006,H\016^*ju\026,\022A\026\t\003\013^K!\001W\030\003\007%sG\017\003\004[\001\021\005caW\001\013CNtU\017\0347bE2,W#A\013)\005\001i\006C\0010b\033\005y&B\0011\007\003)\tgN\\8uCRLwN\\\005\003E~\023A\002R3wK2|\007/\032:Ba&<Q\001\032\002\t\002\026\f!b\025;sS:<G+\0379f!\tyaMB\003\002\005!\005um\005\003g+!\\\007CA#j\023\tQwFA\004Qe>$Wo\031;\021\005\025c\027BA70\0051\031VM]5bY&T\030M\0317f\021\025\031b\r\"\001p)\005)\007bB9g\003\003%\tE]\001\016aJ|G-^2u!J,g-\033=\026\003M\004\"\001^=\016\003UT!A^<\002\t1\fgn\032\006\002q\006!!.\031<b\023\tQXO\001\004TiJLgn\032\005\by\032\f\t\021\"\001V\0031\001(o\0343vGR\f%/\033;z\021\035qh-!A\005\002}\fa\002\035:pIV\034G/\0227f[\026tG\017\006\003\002\002\005\035\001cA#\002\004%\031\021QA\030\003\007\005s\027\020\003\005\002\nu\f\t\0211\001W\003\rAH%\r\005\n\003\0331\027\021!C!\003\037\tq\002\035:pIV\034G/\023;fe\006$xN]\013\003\003#\001b!a\005\002\032\005\005QBAA\013\025\r\t9bL\001\013G>dG.Z2uS>t\027\002BA\016\003+\021\001\"\023;fe\006$xN\035\005\n\003?1\027\021!C\001\003C\t\001bY1o\013F,\030\r\034\013\005\003G\tI\003E\002F\003KI1!a\n0\005\035\021un\0347fC:D!\"!\003\002\036\005\005\t\031AA\001\021%\tiCZA\001\n\003\ny#\001\005iCND7i\0343f)\0051\006\"CA\032M\006\005I\021IA\033\003!!xn\025;sS:<G#A:\t\023\005eb-!A\005\n\005m\022a\003:fC\022\024Vm]8mm\026$\"!!\020\021\007Q\fy$C\002\002BU\024aa\0242kK\016$\b";
        byte[] decoded = Encoding.decode(input);
        //noinspection ConstantConditions
        assertTrue(decoded.length == 1047);
        String encoded = Encoding.encode(decoded);
        if (!encoded.equals(input)) {
            System.err.println(compareStrings(input, encoded));
            assertTrue(false);
        }
    }

    @Test
    public void Sample2() {
        String input = "\006\001\rEd\001B\001\003\0055\021q\001R3dS6\fGN\003\002\004\t\005)A/\0379fg*\021QAB\001\004gFd'BA\004\t\003\025\031\b/\031:l\025\tI!\"\001\004ba\006\034\007.\032\006\002\027\005\031qN]4\004\001M!\001A\004\013#!\ty!#D\001\021\025\005\t\022!B:dC2\f\027BA\n\021\005\031\te.\037*fMB\031Q#\b\021\017\005YYbBA\f\033\033\005A\"BA\r\r\003\031a$o\\8u}%\t\021#\003\002\035!\0059\001/Y2lC\036,\027B\001\020 \005\035y%\017Z3sK\022T!\001\b\t\021\005\005\002Q\"\001\002\021\005=\031\023B\001\023\021\0051\031VM]5bY&T\030M\0317f\021\0251\003\001\"\001(\003\031a\024N\\5u}Q\t\001\005C\004*\001\001\007I\021\002\026\002\025\021,7-[7bYZ\013G.F\001,!\t)B&\003\002.?\tQ!)[4EK\016LW.\0317\t\017=\002\001\031!C\005a\005qA-Z2j[\006dg+\0317`I\025\fHCA\0315!\ty!'\003\0024!\t!QK\\5u\021\035)d&!AA\002-\n1\001\037\0232\021\0319\004\001)Q\005W\005YA-Z2j[\006dg+\0317!\021\035I\004\0011A\005\ni\nq\001\\8oOZ\013G.F\001<!\tyA(\003\002>!\t!Aj\0348h\021\035y\004\0011A\005\n\001\0131\002\\8oOZ\013Gn\030\023fcR\021\021'\021\005\bky\n\t\0211\001<\021\031\031\005\001)Q\005w\005AAn\0348h-\006d\007\005C\004F\001\001\007I\021\002$\002\025}\003(/Z2jg&|g.F\001H!\ty\001*\003\002J!\t\031\021J\034;\t\017-\003\001\031!C\005\031\006qq\f\035:fG&\034\030n\0348`I\025\fHCA\031N\021\035)$*!AA\002\035Caa\024\001!B\0239\025aC0qe\026\034\027n]5p]\002Bq!\025\001A\002\023%a)\001\004`g\016\fG.\032\005\b'\002\001\r\021\"\003U\003)y6oY1mK~#S-\035\013\003cUCq!\016*\002\002\003\007q\t\003\004X\001\001\006KaR\001\b?N\034\027\r\\3!\021\025I\006\001\"\001G\003%\001(/Z2jg&|g\016C\003\\\001\021\005a)A\003tG\006dW\rC\003^\001\021\005a,A\002tKR$\"\001I0\t\013eb\006\031A\036\t\013u\003A\021A1\025\005\001\022\007\"B2a\001\0049\025AB5oiZ\013G\016C\003^\001\021\005Q\r\006\003!M\"L\007\"B4e\001\004Y\024\001C;og\016\fG.\0323\t\013e#\007\031A$\t\013m#\007\031A$\t\013-\004A\021\0017\002\023M,Go\024:Ok2dG\003\002\021n]>DQa\0326A\002mBQ!\0276A\002\035CQa\0276A\002\035CQ!\030\001\005\002E$B\001\t:uk\")1\017\035a\001W\0059A-Z2j[\006d\007\"B-q\001\0049\005\"B.q\001\0049\005\"B/\001\t\0039HC\001\021y\021\025\031h\0171\001,\021\025i\006\001\"\001{)\t\0013\020C\003ts\002\007\001\005C\003~\001\021\005!&\001\007u_\nKw\rR3dS6\fG\016\003\004\000\001\021\005\021\021A\001\021i>T\025M^1CS\036$UmY5nC2,\"!a\001\021\t\005\025\021qB\007\003\003\017QA!!\003\002\f\005!Q.\031;i\025\t\ti!\001\003kCZ\f\027bA\027\002\b!1\0211\003\001\005\002i\na\002^8V]N\034\027\r\\3e\031>tw\rC\004\002\030\001!\t%!\007\002\021Q|7\013\036:j]\036$\"!a\007\021\t\005u\0211\005\b\004\037\005}\021bAA\021!\0051\001K]3eK\032LA!!\n\002(\t11\013\036:j]\036T1!!\t\021\021\035\tY\003\001C\001\003[\tQ\002^8EK\n,xm\025;sS:<WCAA\016Q\021\tI#!\r\021\t\005M\022\021H\007\003\003kQ1!a\016\007\003)\tgN\\8uCRLwN\\\005\005\003w\t)D\001\007EKZ,Gn\0349fe\006\003\030\016C\004\002@\001!\t!!\021\002\021Q|Gi\\;cY\026,\"!a\021\021\007=\t)%C\002\002HA\021a\001R8vE2,\007bBA&\001\021\005\021QJ\001\bi>4En\\1u+\t\ty\005E\002\020\003#J1!a\025\021\005\0251En\\1u\021\031\t9\006\001C\001u\0051Ao\034'p]\036Da!a\027\001\t\0031\025!\002;p\023:$\bbBA0\001\021\005\021\021M\001\bi>\034\006n\034:u+\t\t\031\007E\002\020\003KJ1!a\032\021\005\025\031\006n\034:u\021\035\tY\007\001C\001\003[\na\001^8CsR,WCAA8!\ry\021\021O\005\004\003g\002\"\001\002\"zi\026Dq!a\036\001\t\003\tI(A\bdQ\006tw-\032)sK\016L7/[8o)\031\tY(!!\002\004B\031q\"! \n\007\005}\004CA\004C_>dW-\0318\t\re\013)\b1\001H\021\031Y\026Q\017a\001\017\"1\021q\021\001\005B\035\nQa\0317p]\026Dq!a#\001\t\003\ni)A\004d_6\004\030M]3\025\007\035\013y\tC\004\002\022\006%\005\031\001\021\002\013=$\b.\032:\t\017\005U\005\001\"\021\002\030\0061Q-];bYN$B!a\037\002\032\"A\021\021SAJ\001\004\tY\nE\002\020\003;K1!a(\021\005\r\te.\037\005\b\003G\003A\021IAS\003!A\027m\0355D_\022,G#A$\t\017\005%\006\001\"\001\002,\0061\021n\035.fe>,\"!a\037\t\017\005=\006\001\"\001\0022\006)A\005\0357vgR\031\001%a-\t\017\005U\026Q\026a\001A\005!A\017[1u\021\035\tI\f\001C\001\003w\013a\001J7j]V\034Hc\001\021\002>\"9\021QWA\\\001\004\001\003bBAa\001\021\005\0211Y\001\007IQLW.Z:\025\007\001\n)\rC\004\0026\006}\006\031\001\021\t\017\005%\007\001\"\001\002L\006!A\005Z5w)\r\001\023Q\032\005\b\003k\0139\r1\001!\021\035\t\t\016\001C\001\003'\f\001\002\n9fe\016,g\016\036\013\004A\005U\007bBA[\003\037\004\r\001\t\005\b\0033\004A\021AAn\003%\021X-\\1j]\022,'\017F\002!\003;Dq!!.\002X\002\007\001\005C\004\002b\002!\t!a9\002\031Ut\027M]=`I5Lg.^:\026\003\001Bq!a:\001\t\003\t\031/A\002bEN<q!a;\003\021\003\ti/A\004EK\016LW.\0317\021\007\005\nyO\002\004\002\005!\005\021\021_\n\005\003_t!\005C\004'\003_$\t!!>\025\005\0055\bBCA}\003_\024\r\021\"\003\002|\006i!kT+O\t&suiX'P\t\026+\"!!@\021\t\005}(q\002\b\005\005\003\0219AD\002\026\005\007I1A!\002 \003)\021\025n\032#fG&l\027\r\\\005\005\005\023\021Y!\001\007S_VtG-\0338h\033>$WM\003\003\003\006\t5!bAA\005!%!!\021\003B\n\005\0251\026\r\\;f\023\r\021)\002\005\002\f\013:,X.\032:bi&|g\016C\005\003\032\005=\b\025!\003\002~\006q!kT+O\t&suiX'P\t\026\003\003\"\003B\017\003_\024\r\021\"\001G\003=i\025\tW0M\037:;u\fR%H\023R\033\006\002\003B\021\003_\004\013\021B$\002!5\013\005l\030'P\035\036{F)S$J)N\003\003B\003B\023\003_\024\r\021\"\003\003(\0051\001kT,`cA*\"A!\013\021\t=\021YcO\005\004\005[\001\"!B!se\006L\b\"\003B\031\003_\004\013\021\002B\025\003\035\001vjV02a\001B!B!\016\002p\n\007I\021\002B\034\0031\021\025jR0E\013\016{&,\022*P+\t\021I\004\005\003\003<\tuRB\001B\007\023\ri#Q\002\005\n\005\003\ny\017)A\005\005s\tQBQ%H?\022+5i\030.F%>\003\003B\003B#\003_\024\r\021\"\003\003H\005aQ*\021+I?\016{e\nV#Y)V\021!\021\n\t\005\003\013\021Y%\003\003\003N\005\035!aC'bi\"\034uN\034;fqRD\021B!\025\002p\002\006IA!\023\002\0335\013E\013S0D\037:#V\t\027+!\021-\021)&a<C\002\023\005A!a9\002\ti+%k\024\005\t\0053\ny\017)A\005A\005)!,\022*PA!Y!QLAx\005\004%\t\001BAr\003\rye*\022\005\t\005C\ny\017)A\005A\005!qJT#!\021!\021)'a<\005\002\t\035\024!B1qa2LHc\001\021\003j!A!1\016B2\001\004\t\031%A\003wC2,X\r\003\005\003f\005=H\021\001B8)\r\001#\021\017\005\b\005W\022i\0071\001<\021!\021)'a<\005\002\tUDc\001\021\003x!9!1\016B:\001\0049\005\002\003B3\003_$\tAa\037\025\007\001\022i\bC\004\003l\te\004\031A\026\t\021\t\025\024q\036C\001\005\003#2\001\tBB\021!\021YGa A\002\005\r\001\002\003B3\003_$\tAa\"\025\017\001\022IIa#\003\016\"9!1\016BC\001\004Y\003BB-\003\006\002\007q\t\003\004\\\005\013\003\ra\022\005\t\005K\ny\017\"\001\003\022R9\001Ea%\003\026\n]\005\002\003B6\005\037\003\r!a\001\t\re\023y\t1\001H\021\031Y&q\022a\001\017\"A!QMAx\t\003\021Y\nF\004!\005;\023yJ!)\t\r\035\024I\n1\001<\021\031I&\021\024a\001\017\"11L!'A\002\035C\001B!\032\002p\022\005!Q\025\013\004A\t\035\006\002\003B6\005G\003\r!a\007\007\027\t-\026q\036I\001\004\003!!Q\026\002\024\t\026\034\027.\\1m\023N\034uN\0344mS\016$X\rZ\n\007\005S\023yKa/\021\t\tE&qW\007\003\005gSAA!.\002\f\005!A.\0318h\023\021\021ILa-\003\r=\023'.Z2u!\021)\"Q\030\021\n\007\t}vDA\004Ok6,'/[2\t\021\t\r'\021\026C\001\005\013\fa\001J5oSR$C#A\031\t\021\t%'\021\026C!\005\027\fA\001\0357vgR)\001E!4\003R\"9!q\032Bd\001\004\001\023!\001=\t\017\tM'q\031a\001A\005\t\021\020\003\005\003X\n%F\021\tBm\003\025!\030.\\3t)\025\001#1\034Bo\021\035\021yM!6A\002\001BqAa5\003V\002\007\001\005\003\005\003b\n%F\021\tBr\003\025i\027N\\;t)\025\001#Q\035Bt\021\035\021yMa8A\002\001BqAa5\003`\002\007\001\005\003\005\003l\n%F\021\tBw\003\031qWmZ1uKR\031\001Ea<\t\017\t='\021\036a\001A!A\021q\bBU\t\003\022\031\020\006\003\002D\tU\bb\002Bh\005c\004\r\001\t\005\t\003\027\022I\013\"\021\003zR!\021q\nB~\021\035\021yMa>A\002\001B\001\"a\027\003*\022\005#q \013\004\017\016\005\001b\002Bh\005{\004\r\001\t\005\t\003/\022I\013\"\021\004\006Q\0311ha\002\t\017\t=71\001a\001A!A11\002BU\t\003\032i!A\004ge>l\027J\034;\025\007\001\032y\001C\004\003P\016%\001\031A$\t\021\005-%\021\026C!\007'!RaRB\013\007/AqAa4\004\022\001\007\001\005C\004\003T\016E\001\031\001\021\b\023\rm\021q\036E\001\t\ru\021a\005#fG&l\027\r\\%t\rJ\f7\r^5p]\006d\007\003BB\020\007Ci!!a<\007\023\r\r\022q\036E\001\t\r\025\"a\005#fG&l\027\r\\%t\rJ\f7\r^5p]\006d7\003CB\021\005_\0339c!\013\021\t\r}!\021\026\t\005+\r-\002%C\002\004.}\021!B\022:bGRLwN\\1m\021\03513\021\005C\001\007c!\"a!\b\t\021\rU2\021\005C!\007o\t1\001Z5w)\025\0013\021HB\036\021\035\021yma\rA\002\001BqAa5\0044\001\007\001\005\003\006\004@\r\005\022\021!C\005\007\003\n1B]3bIJ+7o\0347wKR\021!qV\004\n\007\013\ny\017#\001\005\007\017\n1\003R3dS6\fG.Q:JM&sG/Z4sC2\004Baa\b\004J\031I11JAx\021\003!1Q\n\002\024\t\026\034\027.\\1m\003NLe-\0238uK\036\024\030\r\\\n\t\007\023\022yka\n\004PA!Qc!\025!\023\r\031\031f\b\002\t\023:$Xm\032:bY\"9ae!\023\005\002\r]CCAB$\021!\031Yf!\023\005B\ru\023\001B9v_R$R\001IB0\007CBqAa4\004Z\001\007\001\005C\004\003T\016e\003\031\001\021\t\021\r\0254\021\nC!\007O\n1A]3n)\025\0013\021NB6\021\035\021yma\031A\002\001BqAa5\004d\001\007\001\005\003\006\004@\r%\023\021!C\005\007\003B!ba\020\002p\006\005I\021BB!\001";
        byte[] decoded = Encoding.decode(input);
        //noinspection ConstantConditions
        assertTrue(decoded.length == 3431);
        String encoded = Encoding.encode(decoded);
        if (!encoded.equals(input)) {
            System.err.println(compareStrings(input, encoded));
            assertTrue(false);
        }
    }
}

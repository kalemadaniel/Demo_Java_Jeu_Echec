package echecs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;
import javax.swing.JComponent;
import javax.swing.UIManager;

final class Echiquier extends JComponent {
    private static final Color CLAIRE = new Color(0xE9DBBF), FONCEE = new Color(0x77958A);
    private static final Color CADRE = new Color(0x21433D), DORE = new Color(0xF6C76B);
    private static final String PIECES = " \u265F\u265E\u265D\u265C\u265B\u265A";
    private final IntConsumer action;
    private final Font symboles = policeSymboles();
    private final boolean[] cibles = new boolean[64];
    private Position position = Position.initiale();
    private Position.Coup dernier;
    private int selection = -1, curseur = 52;
    private boolean clavier;

    Echiquier(IntConsumer action) {
        this.action = action;
        setPreferredSize(new Dimension(575, 575));
        setMinimumSize(new Dimension(350, 350));
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                int taille = tailleCase();
                if (taille < 1) return;
                int x = e.getX() - origineX(), y = e.getY() - origineY();
                if (x < 0 || y < 0 || x >= 8 * taille || y >= 8 * taille) return;
                clavier = false;
                curseur = 8 * (y / taille) + x / taille;
                requestFocusInWindow();
                action.accept(curseur);
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: curseur = Math.max(0, curseur / 8 - 1) * 8 + curseur % 8; break;
                    case KeyEvent.VK_DOWN: curseur = Math.min(7, curseur / 8 + 1) * 8 + curseur % 8; break;
                    case KeyEvent.VK_LEFT: if (curseur % 8 > 0) curseur--; break;
                    case KeyEvent.VK_RIGHT: if (curseur % 8 < 7) curseur++; break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE: action.accept(curseur); break;
                    case KeyEvent.VK_ESCAPE: action.accept(-1); break;
                    default: return;
                }
                e.consume();
                clavier = true;
                repaint();
            }
        });
    }

    void afficher(Position nouvelle, Position.Coup coup, int choix, Iterable<Position.Coup> legaux) {
        position = nouvelle;
        dernier = coup;
        selection = choix;
        java.util.Arrays.fill(cibles, false);
        for (Position.Coup possible : legaux) {
            if (possible.origine == choix) cibles[possible.arrivee] = true;
        }
        repaint();
    }

    private int tailleCase() { return Math.max(0, (Math.min(getWidth(), getHeight()) - 42) / 8); }
    private int origineX() { return (getWidth() - 8 * tailleCase()) / 2; }
    private int origineY() { return (getHeight() - 8 * tailleCase()) / 2; }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        lissage(g);
        int taille = tailleCase(), x0 = origineX(), y0 = origineY(), cote = 8 * taille;
        if (taille < 1) {
            g.dispose();
            return;
        }

        g.setColor(CADRE);
        g.fillRoundRect(x0 - 19, y0 - 18, cote + 38, cote + 36, 17, 17);
        g.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11f));
        g.setColor(new Color(0xF0EAD7));
        for (int i = 0; i < 8; i++) {
            String lettre = String.valueOf((char) ('a' + i));
            String chiffre = Integer.toString(8 - i);
            int x = x0 + i * taille + (taille - g.getFontMetrics().stringWidth(lettre)) / 2;
            g.drawString(lettre, x, y0 + cote + 13);
            g.drawString(chiffre, x0 - 13, y0 + i * taille + taille / 2 + 4);
        }

        int roiEnEchec = -1;
        if (position.enEchec(position.trait)) {
            roiEnEchec = position.trait == Position.BLANC ? position.roiBlanc : position.roiNoir;
        }
        for (int i = 0; i < 64; i++) {
            boolean claire = (i / 8 + i % 8) % 2 == 0;
            int x = x0 + i % 8 * taille, y = y0 + i / 8 * taille;
            Color fond = claire ? CLAIRE : FONCEE;
            if (dernier != null && (dernier.origine == i || dernier.arrivee == i)) {
                fond = claire ? new Color(0xECD393) : new Color(0xABB47A);
            }
            if (i == roiEnEchec) fond = new Color(0xD98179);
            g.setColor(fond);
            g.fillRect(x, y, taille, taille);

            int piece = position.cases[i];
            if (piece != 0) dessinerPiece(g, piece, x, y, taille);
            if (cibles[i]) {
                g.setColor(new Color(0xA638534B, true));
                if (piece == 0) {
                    int diametre = Math.max(6, taille / 5);
                    g.fillOval(x + (taille - diametre) / 2, y + (taille - diametre) / 2, diametre, diametre);
                } else {
                    g.setStroke(new BasicStroke(Math.max(2, taille / 12)));
                    g.drawOval(x + 5, y + 5, taille - 10, taille - 10);
                }
            }
            if (i == selection || (i == curseur && clavier && hasFocus())) {
                g.setColor(i == selection ? DORE : CADRE);
                g.setStroke(new BasicStroke(3));
                g.drawRect(x + 2, y + 2, taille - 4, taille - 4);
            }
        }
        g.dispose();
    }

    private void dessinerPiece(Graphics2D g, int piece, int x, int y, int taille) {
        Font police = symboles.deriveFont(taille * 0.83f);
        GlyphVector glyphe = police.createGlyphVector(g.getFontRenderContext(),
                String.valueOf(PIECES.charAt(Math.abs(piece))));
        Shape forme = glyphe.getOutline();
        Rectangle2D b = forme.getBounds2D();
        Shape centre = AffineTransform.getTranslateInstance(x + (taille - b.getWidth()) / 2 - b.getX(),
                y + (taille - b.getHeight()) / 2 - b.getY()).createTransformedShape(forme);
        g.setStroke(new BasicStroke(Math.max(1.4f, taille / 32f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(piece > 0 ? new Color(0x554A3B) : new Color(0xEEE2CB));
        g.draw(centre);
        g.setColor(piece > 0 ? new Color(0xFFF8E8) : new Color(0x263634));
        g.fill(centre);
    }

    private static Font policeSymboles() {
        for (String nom : new String[]{"Segoe UI Symbol", "DejaVu Sans", "Dialog"}) {
            Font police = new Font(nom, Font.PLAIN, 48);
            if (police.canDisplayUpTo(PIECES) < 0) return police;
        }
        return new Font(Font.DIALOG, Font.PLAIN, 48);
    }

    static void lissage(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static BufferedImage tasse(int taille) {
        BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        lissage(g);
        g.scale(taille / 64.0, taille / 64.0);
        g.setColor(CADRE);
        g.fillRoundRect(0, 0, 64, 64, 16, 16);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(DORE);
        g.drawArc(25, 9, 9, 12, -80, 160);
        g.drawArc(34, 7, 9, 14, -80, 160);
        g.setColor(new Color(0xFFF8E8));
        g.drawRoundRect(35, 29, 15, 13, 8, 8);
        g.fillRoundRect(15, 25, 28, 24, 11, 11);
        g.drawLine(12, 53, 49, 53);
        g.dispose();
        return image;
    }
}


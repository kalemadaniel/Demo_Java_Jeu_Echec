package echecs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public final class Application extends JFrame {
    private static final Color FOND = new Color(0xEDF1ED), ENCRE = new Color(0x193830);
    private static final Color GRIS = new Color(0x51665F), CARTE = new Color(0xFCFCF7);
    private static final Color LIGNE = new Color(0xD7E2D8);
    private static final String[] PROMOTIONS = {"Dame", "Tour", "Fou", "Cavalier"};
    private final Echiquier plateau = new Echiquier(this::cliquer);
    private final JLabel statut = texte(" ", 18, Font.BOLD, ENCRE);
    private final JLabel indication = texte(" ", 12, Font.PLAIN, GRIS);
    private final JComboBox<Adversaire.Niveau> niveaux = new JComboBox<>(Adversaire.Niveau.values());
    private final DefaultListModel<String> journal = new DefaultListModel<>();
    private final JList<String> historique = new JList<>(journal);
    private final Map<String, Integer> repetitions = new HashMap<>();
    private Position position;
    private List<Position.Coup> legaux;
    private Position.Coup dernier;
    private int selection, numeroCoup;
    private String resultat;
    private SwingWorker<Position.Coup, Void> calcul;

    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception erreur) {
                throw new IllegalStateException("Le thème Nimbus de Java SE est introuvable.", erreur);
            }
            new Application().setVisible(true);
        });
    }

    private Application() {
        super("Demo_Java_Jeu_Echec");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImages(Arrays.asList(Echiquier.tasse(64), Echiquier.tasse(32), Echiquier.tasse(16)));
        setJMenuBar(menu());
        setContentPane(interfaceJeu());
        setMinimumSize(new Dimension(735, 540));
        pack();
        Rectangle disponible = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(getWidth(), disponible.width), Math.min(getHeight(), disponible.height));
        setLocationRelativeTo(null);
        recommencer();
    }

    private JPanel interfaceJeu() {
        JPanel page = new JPanel(new BorderLayout(16, 12));
        page.setBorder(BorderFactory.createEmptyBorder(12, 19, 14, 19));
        page.setBackground(FOND);

        JPanel titre = new JPanel(new BorderLayout(11, 0));
        titre.setOpaque(false);
        titre.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        titre.add(new JLabel(new ImageIcon(Echiquier.tasse(44))), BorderLayout.WEST);
        JPanel libelles = new JPanel(new GridLayout(2, 1, 0, 1));
        libelles.setOpaque(false);
        libelles.add(texte("Jeu d’échecs", 22, Font.BOLD, ENCRE));
        libelles.add(texte("Vous jouez les blancs, l’ordinateur joue les noirs.", 13, Font.PLAIN, GRIS));
        titre.add(libelles, BorderLayout.CENTER);
        page.add(titre, BorderLayout.NORTH);
        page.add(plateau, BorderLayout.CENTER);

        JPanel lateral = new JPanel(new BorderLayout(0, 13));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(254, 500));

        JPanel commandes = carte();
        commandes.setLayout(new BoxLayout(commandes, BoxLayout.Y_AXIS));
        JButton nouvelle = new JButton("Nouvelle partie");
        nouvelle.addActionListener(e -> demanderNouvelle());
        JLabel difficulte = texte("Niveau de l’ordinateur", 13, Font.BOLD, ENCRE);
        difficulte.setLabelFor(niveaux);
        niveaux.setSelectedItem(Adversaire.Niveau.MOYEN);
        niveaux.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        nouvelle.setMaximumSize(new Dimension(Short.MAX_VALUE, 34));
        commandes.add(gauche(statut));
        commandes.add(Box.createVerticalStrut(3));
        commandes.add(gauche(indication));
        commandes.add(Box.createVerticalStrut(15));
        commandes.add(gauche(difficulte));
        commandes.add(Box.createVerticalStrut(5));
        commandes.add(gauche(niveaux));
        commandes.add(Box.createVerticalStrut(10));
        commandes.add(gauche(nouvelle));
        lateral.add(commandes, BorderLayout.NORTH);

        JPanel coups = carte();
        coups.setLayout(new BorderLayout(0, 8));
        coups.add(texte("Historique des coups", 15, Font.BOLD, ENCRE), BorderLayout.NORTH);
        historique.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        historique.setFixedCellHeight(26);
        historique.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historique.getAccessibleContext().setAccessibleName("Historique des coups");
        JScrollPane defilement = new JScrollPane(historique);
        defilement.setBorder(BorderFactory.createLineBorder(LIGNE));
        coups.add(defilement, BorderLayout.CENTER);
        lateral.add(coups, BorderLayout.CENTER);
        page.add(lateral, BorderLayout.EAST);

        page.add(texte("Sélectionnez une pièce puis une case. Clavier : flèches et Entrée.",
                12, Font.PLAIN, GRIS), BorderLayout.SOUTH);
        return page;
    }

    private JMenuBar menu() {
        JMenuBar barre = new JMenuBar();
        JMenu partie = new JMenu("Partie");
        JMenuItem nouvelle = new JMenuItem("Nouvelle partie");
        nouvelle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        nouvelle.addActionListener(e -> demanderNouvelle());
        JMenuItem quitter = new JMenuItem("Quitter");
        quitter.addActionListener(e -> dispose());
        partie.add(nouvelle);
        partie.addSeparator();
        partie.add(quitter);
        JMenu aide = new JMenu("Aide");
        JMenuItem propos = new JMenuItem("À propos");
        propos.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        propos.addActionListener(e -> aPropos());
        aide.add(propos);
        barre.add(partie);
        barre.add(aide);
        return barre;
    }

    @Override public void dispose() {
        annulerCalcul();
        super.dispose();
    }

    private static JPanel carte() {
        JPanel panel = new JPanel();
        panel.setBackground(CARTE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LIGNE),
                BorderFactory.createEmptyBorder(13, 13, 13, 13)));
        return panel;
    }

    private static JLabel texte(String libelle, float taille, int style, Color couleur) {
        JLabel etiquette = new JLabel(libelle);
        etiquette.setFont(etiquette.getFont().deriveFont(style, taille));
        etiquette.setForeground(couleur);
        return etiquette;
    }

    private static <T extends JComponent> T gauche(T composant) {
        composant.setAlignmentX(Component.LEFT_ALIGNMENT);
        return composant;
    }


    private void demanderNouvelle() {
        if (dernier == null || resultat != null || JOptionPane.showConfirmDialog(this,
                "Abandonner la partie en cours et recommencer ?", "Nouvelle partie",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            recommencer();
        }
    }

    private void recommencer() {
        annulerCalcul();
        position = Position.initiale();
        dernier = null;
        selection = -1;
        numeroCoup = 1;
        journal.clear();
        repetitions.clear();
        actualiserPosition();
    }

    private void annulerCalcul() {
        if (calcul != null) {
            SwingWorker<Position.Coup, Void> ancien = calcul;
            calcul = null;
            ancien.cancel(true);
        }
    }

    private void cliquer(int caseChoisie) {
        if (resultat != null || calcul != null || position.trait != Position.BLANC) return;
        if (caseChoisie < 0 || caseChoisie == selection) {
            selection = -1;
        } else if (position.cases[caseChoisie] > 0) {
            selection = caseChoisie;
        } else if (selection >= 0) {
            List<Position.Coup> choix = new ArrayList<>(4);
            for (Position.Coup coup : legaux) {
                if (coup.origine == selection && coup.arrivee == caseChoisie) choix.add(coup);
            }
            if (!choix.isEmpty()) {
                int i = 0;
                if (choix.size() > 1) {
                    i = JOptionPane.showOptionDialog(this, "En quelle pièce promouvoir le pion ?",
                            "Promotion", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                            null, PROMOTIONS, PROMOTIONS[0]);
                }
                if (i >= 0) jouer(choix.get(i));
                return;
            }
        }
        afficher();
    }

    private void jouer(Position.Coup coup) {
        Position avant = position;
        position = position.apres(coup);
        selection = -1;
        dernier = coup;
        actualiserPosition();
        String note = notation(avant, coup);
        if (avant.trait == Position.BLANC) {
            journal.addElement(String.format("%3d.  %-12s", numeroCoup, note));
        } else {
            int ligne = journal.size() - 1;
            journal.set(ligne, journal.get(ligne) + note);
            numeroCoup++;
        }
        historique.ensureIndexIsVisible(journal.size() - 1);
        if (resultat == null && position.trait == Position.NOIR) lancerCalcul();
    }

    private String notation(Position avant, Position.Coup coup) {
        int type = Math.abs(avant.cases[coup.origine]);
        String note;
        if (type == Position.ROI && Math.abs(coup.arrivee - coup.origine) == 2) {
            note = coup.arrivee > coup.origine ? "0-0" : "0-0-0";
        } else {
            boolean prise = avant.cases[coup.arrivee] != 0
                    || (type == Position.PION && coup.arrivee == avant.passant);
            note = (type == Position.PION ? "" : " PCFTDR".substring(type, type + 1))
                    + Position.nomCase(coup.origine) + (prise ? "×" : "–") + Position.nomCase(coup.arrivee);
            if (coup.promotion != 0) note += "=" + " PCFTDR".charAt(coup.promotion);
        }
        if (position.enEchec(position.trait)) note += legaux.isEmpty() ? "#" : "+";
        return note;
    }

    private void actualiserPosition() {
        legaux = position.legaux();
        int occurrences = repetitions.merge(position.cle(legaux), 1, Integer::sum);
        resultat = null;
        if (legaux.isEmpty()) {
            if (position.enEchec(position.trait)) {
                resultat = position.trait == Position.BLANC ? "Mat · les noirs gagnent." : "Mat · vous gagnez !";
            } else {
                resultat = "Nulle · pat.";
            }
        } else if (position.materielInsuffisant()) {
            resultat = "Nulle · matériel insuffisant.";
        } else if (position.demiCoups >= 100) {
            resultat = "Nulle · règle des 50 coups.";
        } else if (occurrences >= 3) {
            resultat = "Nulle · triple répétition.";
        }
        afficher();
    }

    private void afficher() {
        plateau.afficher(position, dernier, selection, legaux);
        boolean blanc = position.trait == Position.BLANC;
        niveaux.setEnabled(blanc || resultat != null);
        if (resultat != null) {
            statut.setText("Partie terminée");
            indication.setText(resultat);
        } else {
            statut.setText(blanc ? "À vous de jouer" : "L’ordinateur réfléchit…");
            if (position.enEchec(position.trait)) {
                indication.setText(blanc ? "Votre roi est en échec." : "Le roi noir est en échec.");
            } else {
                indication.setText(blanc ? "Tour des blancs" : "Tour des noirs");
            }
        }
    }

    private void lancerCalcul() {
        Position instantane = position;
        Map<String, Integer> coupsPrecedents = new HashMap<>(repetitions);
        Adversaire.Niveau niveau = (Adversaire.Niveau) niveaux.getSelectedItem();
        calcul = new SwingWorker<Position.Coup, Void>() {
            @Override protected Position.Coup doInBackground() {
                return new Adversaire().choisir(instantane, niveau, coupsPrecedents);
            }

            @Override protected void done() {
                if (calcul != this) return;
                calcul = null;
                try {
                    jouer(get());
                } catch (InterruptedException erreur) {
                    Thread.currentThread().interrupt();
                    jouer(legaux.get(0));
                } catch (ExecutionException erreur) {
                    erreur.getCause().printStackTrace();
                    jouer(legaux.get(0));
                }
            }
        };
        calcul.execute();
    }

    private void aPropos() {
        String html = "<html><body style='font-family:sans-serif;font-size:11pt;color:#193830'>"
                + "<h2>Demo_Java_Jeu_Echec</h2>"
                + "<p><b>Auteur</b> Kalema Daniel Jonathan</p>"
                + "<h3>But du jeu</h3>"
                + "<p>Les blancs commencent. Chaque camp joue à son tour. Un roi attaqué est en échec. "
                + "Il est interdit de laisser ou de placer son propre roi en échec. Si aucun coup légal "
                + "ne peut sauver le roi en échec, c’est échec et mat.</p>"
                + "<h3>Déplacement des pièces</h3><ul>"
                + "<li><b>Roi</b> : une case dans toutes les directions.</li>"
                + "<li><b>Dame</b> : en ligne droite ou en diagonale.</li>"
                + "<li><b>Tour</b> : à l’horizontale ou à la verticale.</li>"
                + "<li><b>Fou</b> : en diagonale.</li>"
                + "<li><b>Cavalier</b> : deux cases dans un axe puis une dans l’autre. Il peut sauter par-dessus les pièces.</li>"
                + "<li><b>Pion</b> : avance d’une case libre, ou de deux depuis sa case de départ si les deux sont libres. "
                + "Il capture une case en diagonale vers l’avant.</li>"
                + "</ul><p>Pour capturer une pièce adverse, on prend sa place, sauf lors d’une prise en passant.</p>"
                + "<h3>Coups spéciaux</h3><ul>"
                + "<li><b>Roque</b> : le roi se déplace de deux cases vers une tour, qui vient de l’autre côté du roi. "
                + "Aucun des deux ne doit avoir bougé. Les cases entre eux doivent être libres. Le roi ne peut ni "
                + "partir d’une case attaquée, ni en traverser une, ni y arriver.</li>"
                + "<li><b>Prise en passant</b> : après l’avance de deux cases d’un pion adverse, un pion voisin peut "
                + "le prendre sur la case franchie, uniquement au coup suivant.</li>"
                + "<li><b>Promotion</b> : sur la dernière rangée, le pion devient une dame, une tour, un fou ou un cavalier.</li>"
                + "</ul><h3>Partie nulle</h3>"
                + "<p>Il y a pat quand le joueur au trait n’a aucun coup légal et que son roi n’est pas en échec. "
                + "Le jeu reconnaît aussi les cas usuels de matériel insuffisant. Dans cette application, la nulle "
                + "est automatique à la troisième occurrence de la même position ou après 50 coups de chaque camp "
                + "sans capture ni mouvement de pion.</p></body></html>";
        JEditorPane contenu = new JEditorPane("text/html", html);
        contenu.setEditable(false);
        contenu.setCaretPosition(0);
        contenu.setBorder(BorderFactory.createEmptyBorder(7, 10, 8, 10));
        JScrollPane defilement = new JScrollPane(contenu);
        defilement.setPreferredSize(new Dimension(490, 410));
        JOptionPane.showMessageDialog(this, defilement, "À propos", JOptionPane.PLAIN_MESSAGE,
                new ImageIcon(Echiquier.tasse(44)));
    }
}

package echecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Position autonome. Les cases 0 à 63 vont de a8 à h1. */
final class Position {
    static final int PION = 1, CAVALIER = 2, FOU = 3, TOUR = 4, DAME = 5, ROI = 6;
    static final int BLANC = 1, NOIR = -1;
    private static final int[] PROMOTIONS = {DAME, TOUR, FOU, CAVALIER};
    private static final int[] ENLEVES = new int[64];
    private static final int[][] SAUTS = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
    private static final int[][] DIRECTIONS = {{-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}};

    static {
        ENLEVES[63] = 1;
        ENLEVES[56] = 2;
        ENLEVES[7] = 4;
        ENLEVES[0] = 8;
        ENLEVES[60] = 3;
        ENLEVES[4] = 12;
    }

    final int[] cases;
    final int trait, roques, passant, demiCoups;
    final int roiBlanc, roiNoir;

    private Position(int[] cases, int trait, int roques, int passant, int demiCoups, int roiBlanc, int roiNoir) {
        this.cases = cases;
        this.trait = trait;
        this.roques = roques;
        this.passant = passant;
        this.demiCoups = demiCoups;
        this.roiBlanc = roiBlanc;
        this.roiNoir = roiNoir;
    }

    static Position initiale() {
        return depuisFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    static Position depuisFen(String fen) {
        String[] donnees = fen.split(" ");
        int[] cases = new int[64];
        int index = 0, blanc = -1, noir = -1;
        for (char c : donnees[0].toCharArray()) {
            if (c == '/') {
                continue;
            }
            if (c >= '1' && c <= '8') {
                index += c - '0';
                continue;
            }
            int type = " pnbrqk".indexOf(Character.toLowerCase(c));
            if (c == 'K') blanc = index;
            if (c == 'k') noir = index;
            cases[index++] = Character.isUpperCase(c) ? type : -type;
        }
        int droits = 0;
        for (int i = 0; i < 4; i++) {
            if (donnees[2].indexOf("KQkq".charAt(i)) >= 0) droits |= 1 << i;
        }
        String ep = donnees[3];
        int cible = ep.equals("-") ? -1 : ('8' - ep.charAt(1)) * 8 + ep.charAt(0) - 'a';
        return new Position(cases, donnees[1].equals("w") ? BLANC : NOIR, droits, cible,
                Integer.parseInt(donnees[4]), blanc, noir);
    }

    static final class Coup {
        final int origine, arrivee, promotion;

        Coup(int origine, int arrivee, int promotion) {
            this.origine = origine;
            this.arrivee = arrivee;
            this.promotion = promotion;
        }

        String coordonnees() {
            return nomCase(origine) + nomCase(arrivee) + (promotion == 0 ? "" : " PNBRQK".substring(promotion, promotion + 1).toLowerCase(java.util.Locale.ROOT));
        }
    }

    static String nomCase(int i) {
        return "" + (char) ('a' + i % 8) + (8 - i / 8);
    }

    List<Coup> legaux() {
        List<Coup> possibles = new ArrayList<>(48);
        for (int i = 0; i < 64; i++) {
            int type = cases[i] * trait;
            if (type == PION) {
                pions(i, possibles);
            } else if (type == CAVALIER) {
                sauts(i, possibles, SAUTS, 0, SAUTS.length, false);
            } else if (type == FOU) {
                sauts(i, possibles, DIRECTIONS, 0, 4, true);
            } else if (type == TOUR) {
                sauts(i, possibles, DIRECTIONS, 4, 8, true);
            } else if (type == DAME || type == ROI) {
                sauts(i, possibles, DIRECTIONS, 0, 8, type == DAME);
                if (type == ROI) roques(i, possibles);
            }
        }
        possibles.removeIf(coup -> apres(coup).enEchec(trait));
        return possibles;
    }

    private void pions(int i, List<Coup> coups) {
        int ligne = i / 8, colonne = i % 8, devant = i - 8 * trait;
        if (cases[devant] == 0) {
            ajouterPion(i, devant, coups);
            if (ligne == (trait == BLANC ? 6 : 1) && cases[devant - 8 * trait] == 0) {
                coups.add(new Coup(i, devant - 8 * trait, 0));
            }
        }
        for (int decalage : new int[]{-1, 1}) {
            if (colonne + decalage < 0 || colonne + decalage > 7) continue;
            int cible = devant + decalage;
            if ((cases[cible] * trait < 0 && cases[cible] != -trait * ROI) || cible == passant) {
                ajouterPion(i, cible, coups);
            }
        }
    }

    private static void ajouterPion(int i, int j, List<Coup> coups) {
        if (j / 8 == 0 || j / 8 == 7) {
            for (int type : PROMOTIONS) coups.add(new Coup(i, j, type));
        } else {
            coups.add(new Coup(i, j, 0));
        }
    }

    private void sauts(int i, List<Coup> coups, int[][] pas, int debut, int fin, boolean rayon) {
        for (int d = debut; d < fin; d++) {
            int ligne = i / 8, colonne = i % 8;
            do {
                ligne += pas[d][0];
                colonne += pas[d][1];
                if (!surPlateau(ligne, colonne)) break;
                int j = ligne * 8 + colonne, occupant = cases[j] * trait;
                if (occupant <= 0 && occupant != -ROI) coups.add(new Coup(i, j, 0));
                if (occupant != 0) break;
            } while (rayon);
        }
    }

    private void roques(int i, List<Coup> coups) {
        int droits = trait == BLANC ? roques : roques >> 2;
        int base = trait == BLANC ? 60 : 4;
        if ((droits & 3) == 0 || i != base || enEchec(trait)) return;
        if ((droits & 1) != 0 && cases[i + 3] == trait * TOUR
                && cases[i + 1] == 0 && cases[i + 2] == 0
                && !apres(new Coup(i, i + 1, 0)).enEchec(trait)) {
            coups.add(new Coup(i, i + 2, 0));
        }
        if ((droits & 2) != 0 && cases[i - 4] == trait * TOUR
                && cases[i - 1] == 0 && cases[i - 2] == 0 && cases[i - 3] == 0
                && !apres(new Coup(i, i - 1, 0)).enEchec(trait)) {
            coups.add(new Coup(i, i - 2, 0));
        }
    }

    Position apres(Coup coup) {
        int[] resultat = Arrays.copyOf(cases, cases.length);
        int de = coup.origine, a = coup.arrivee;
        int piece = cases[de], type = Math.abs(piece);
        int enPassant = -1, rb = roiBlanc, rn = roiNoir;
        resultat[de] = 0;
        resultat[a] = coup.promotion == 0 ? piece : trait * coup.promotion;
        if (type == PION) {
            if (a == passant && cases[a] == 0) resultat[a + 8 * trait] = 0;
            if (Math.abs(a - de) == 16) enPassant = (a + de) / 2;
        }
        if (type == ROI) {
            if (trait == BLANC) rb = a;
            else rn = a;
            if (a - de == 2) {
                resultat[de + 1] = resultat[de + 3];
                resultat[de + 3] = 0;
            } else if (de - a == 2) {
                resultat[de - 1] = resultat[de - 4];
                resultat[de - 4] = 0;
            }
        }
        int nouveauxDroits = roques & ~ENLEVES[de] & ~ENLEVES[a];
        int compteur = type == PION || cases[a] != 0 ? 0 : demiCoups + 1;
        return new Position(resultat, -trait, nouveauxDroits, enPassant, compteur, rb, rn);
    }

    boolean enEchec(int couleur) {
        return attaquee(couleur == BLANC ? roiBlanc : roiNoir, -couleur);
    }

    private boolean attaquee(int cible, int couleur) {
        int l = cible / 8, c = cible % 8;
        for (int[] saut : SAUTS) {
            int ligne = l + saut[0], colonne = c + saut[1];
            if (surPlateau(ligne, colonne) && cases[ligne * 8 + colonne] == couleur * CAVALIER) return true;
        }
        for (int i = 0; i < DIRECTIONS.length; i++) {
            int dl = DIRECTIONS[i][0], dc = DIRECTIONS[i][1];
            int ligne = l + dl, colonne = c + dc;
            boolean voisine = true;
            while (surPlateau(ligne, colonne)) {
                int piece = cases[ligne * 8 + colonne];
                if (piece != 0) {
                    int type = piece * couleur;
                    if (type == DAME || type == (i < 4 ? FOU : TOUR)
                            || (voisine && (type == ROI || (type == PION && i < 4 && dl == couleur)))) return true;
                    break;
                }
                voisine = false;
                ligne += dl;
                colonne += dc;
            }
        }
        return false;
    }

    private static boolean surPlateau(int l, int c) {
        return l >= 0 && l < 8 && c >= 0 && c < 8;
    }

    boolean materielInsuffisant() {
        int mineures = 0, fous = 0, couleurs = 0;
        for (int i = 0; i < 64; i++) {
            switch (Math.abs(cases[i])) {
                case PION:
                case TOUR:
                case DAME:
                    return false;
                case CAVALIER:
                    mineures++;
                    break;
                case FOU:
                    mineures++;
                    fous++;
                    couleurs |= 1 << ((i / 8 + i % 8) % 2);
                    break;
                default:
                    break;
            }
        }
        return mineures <= 1 || (mineures == fous && couleurs != 3);
    }

    String cle(List<Coup> coups) {
        StringBuilder cle = new StringBuilder(67);
        for (int piece : cases) cle.append((char) ('G' + piece));
        cle.append((char) ('a' + roques)).append(trait == BLANC ? 'B' : 'N');
        if (passant >= 0) {
            for (Coup coup : coups) {
                if (coup.arrivee == passant && cases[coup.origine] == trait * PION) {
                    cle.append(nomCase(passant));
                    break;
                }
            }
        }
        return cle.toString();
    }
}


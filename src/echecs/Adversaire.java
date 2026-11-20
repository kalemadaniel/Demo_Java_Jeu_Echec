package echecs;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;

final class Adversaire {
    private static final int MAT = 100_000, INFINI = MAT + 1;
    private static final int[] VALEUR = {0, 100, 320, 335, 500, 925, 0};
    private final Random aleatoire = new Random();
    private long echeance;
    private int noeuds;

    enum Niveau {
        FACILE("Facile", 1, 400),
        MOYEN("Moyen", 2, 900),
        DIFFICILE("Difficile", 4, 2000);

        final String nom;
        final int profondeur, millisecondes;

        Niveau(String nom, int profondeur, int millisecondes) {
            this.nom = nom;
            this.profondeur = profondeur;
            this.millisecondes = millisecondes;
        }

        @Override public String toString() { return nom; }
    }

    Position.Coup choisir(Position position, Niveau niveau, Map<String, Integer> historique) {
        List<Position.Coup> coups = position.legaux();
        if (coups.isEmpty()) return null;
        ordonner(position, coups, null);
        Position.Coup meilleur = coups.get(0);
        echeance = System.nanoTime() + niveau.millisecondes * 1_000_000L;
        noeuds = 0;
        for (int profondeur = 1; profondeur <= niveau.profondeur; profondeur++) {
            int maximum = -INFINI, alpha = -INFINI;
            Position.Coup candidat = meilleur;
            ordonner(position, coups, meilleur);
            try {
                for (Position.Coup coup : coups) {
                    Position suite = position.apres(coup);
                    List<Position.Coup> reponses = suite.legaux();
                    int note;
                    if (reponses.isEmpty()) {
                        note = suite.enEchec(suite.trait) ? MAT - 1 : 0;
                    } else if (historique.getOrDefault(suite.cle(reponses), 0) >= 2) {
                        note = 0;
                    } else {
                        note = -chercher(suite, reponses, profondeur - 1, -INFINI, -alpha, 1);
                    }
                    if (niveau == Niveau.FACILE && Math.abs(note) < MAT / 2) {
                        note += aleatoire.nextInt(151) - 75;
                    }
                    if (note > maximum) {
                        maximum = note;
                        candidat = coup;
                    }
                    if (niveau != Niveau.FACILE) alpha = Math.max(alpha, note);
                    verifierDelai();
                }
            } catch (TempsEcoule fin) {
                break;
            }
            meilleur = candidat;
            if (maximum == MAT - 1) break;
        }
        return meilleur;
    }

    private int chercher(Position position, List<Position.Coup> coups, int profondeur, int alpha, int beta, int distance) {
        verifierDelai();
        if (coups.isEmpty()) return position.enEchec(position.trait) ? -MAT + distance : 0;
        if (position.demiCoups >= 100 || position.materielInsuffisant()) return 0;
        if (profondeur == 0) return evaluer(position);
        ordonner(position, coups, null);
        int meilleur = -INFINI;
        for (Position.Coup coup : coups) {
            Position suite = position.apres(coup);
            int note = -chercher(suite, suite.legaux(), profondeur - 1, -beta, -alpha, distance + 1);
            meilleur = Math.max(meilleur, note);
            alpha = Math.max(alpha, note);
            if (alpha >= beta) break;
        }
        return meilleur;
    }

    private void verifierDelai() {
        if ((++noeuds & 127) != 1) return;
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        if (System.nanoTime() >= echeance) throw new TempsEcoule();
    }

    private static final class TempsEcoule extends RuntimeException {
        TempsEcoule() { super(null, null, false, false); }
    }

    private static void ordonner(Position position, List<Position.Coup> coups, Position.Coup prioritaire) {
        coups.sort((a, b) -> Integer.compare(priorite(position, b, prioritaire), priorite(position, a, prioritaire)));
    }

    private static int priorite(Position position, Position.Coup coup, Position.Coup prioritaire) {
        if (coup == prioritaire) return 100_000;
        int type = Math.abs(position.cases[coup.origine]);
        int prise = Math.abs(position.cases[coup.arrivee]);
        if (type == Position.PION && coup.arrivee == position.passant) prise = Position.PION;
        return VALEUR[prise] * 10 + VALEUR[coup.promotion] * 8 - (prise == 0 ? 0 : VALEUR[type]);
    }

    private static int evaluer(Position position) {
        int total = 0;
        for (int i = 0; i < 64; i++) {
            int piece = position.cases[i];
            if (piece == 0) continue;
            int couleur = Integer.signum(piece), type = Math.abs(piece);
            int colonne = i % 8, ligne = i / 8;
            int centre = 14 - Math.abs(2 * colonne - 7) - Math.abs(2 * ligne - 7);
            int bonus = 0;
            switch (type) {
                case Position.PION:
                    bonus = (couleur == Position.BLANC ? 6 - ligne : ligne - 1) * 9 + centre * 2;
                    break;
                case Position.CAVALIER:
                case Position.FOU:
                    bonus = centre * (type == Position.CAVALIER ? 5 : 3);
                    break;
                case Position.DAME:
                    bonus = centre;
                    break;
                case Position.ROI:
                    if (colonne == 6 || colonne == 2) bonus = 24;
                    break;
                default:
                    break;
            }
            total += couleur * (VALEUR[type] + bonus);
        }
        return total * position.trait;
    }
}


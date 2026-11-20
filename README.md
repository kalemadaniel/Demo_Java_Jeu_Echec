# ☕ Demo_Java_Jeu_Echec

<img width="885" height="770" alt="Capture d&#39;écran 2026-11-20 154246" src="https://github.com/user-attachments/assets/c19ecf31-de07-4b1d-afc1-c386c07b3de9" />

Jeu d’échecs de bureau développé en Java avec Swing et le thème Nimbus. Vous jouez les blancs contre l’ordinateur, qui joue les noirs.

## Fonctionnalités

- Trois niveaux de difficulté **Facile**, **Moyen** et **Difficile**
- Affichage des destinations légales de la pièce sélectionnée
- Mise en évidence du dernier coup et du roi en échec
- Historique des coups de la partie
- Roque, prise en passant et promotion au choix
- Détection de l’échec et mat, du pat et des cas usuels de matériel insuffisant
- Partie nulle automatique par triple répétition ou après 50 coups de chaque camp sans capture ni déplacement de pion

Le menu **Aide → À propos** (`F1`) présente les règles du jeu et le nom de l’auteur.

## Démarrer dans NetBeans 8.0.2

Le projet utilise le **JDK 8** et **Apache Ant**, sans Maven ni bibliothèque externe.

1. Téléchargez et extrayez le projet.
2. Dans NetBeans, choisissez **File → Open Project**.
3. Sélectionnez le dossier `Demo_Java_Jeu_Echec` qui contient `build.xml`.
4. Sélectionnez le projet dans le panneau **Projects**, puis appuyez sur **F6** pour le lancer.

Pour générer le JAR exécutable, utilisez **Clean and Build** (`Maj` + `F11`). Le fichier est créé dans `dist/`.

```bash
java -jar dist/Demo_Java_Jeu_Echec.jar
```

Si vous avez téléchargé le JAR seul, lancez `java -jar Demo_Java_Jeu_Echec.jar` depuis le dossier qui le contient.

## Comment jouer

1. Choisissez le niveau de l’ordinateur.
2. Cliquez sur une pièce blanche pour afficher ses destinations légales.
3. Cliquez sur une destination pour jouer. L’ordinateur répond automatiquement.

Vous pouvez aussi jouer au clavier. Placez le focus sur l’échiquier, utilisez les flèches pour vous déplacer et **Entrée** pour sélectionner une pièce ou valider un coup.

## Organisation du code

| Fichier | Rôle |
| --- | --- |
| `src/echecs/Application.java` | Fenêtre Swing, menus, déroulement de la partie et historique |
| `src/echecs/Echiquier.java` | Dessin du plateau et des pièces, interactions souris et clavier |
| `src/echecs/Position.java` | État du jeu, génération et validation des coups |
| `src/echecs/Adversaire.java` | Évaluation des positions et choix des coups selon le niveau |

## Auteur

**Kalema Daniel Jonathan**

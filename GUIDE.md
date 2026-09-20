# TrustPay v1 : mise en ligne (environ 15 minutes)

Tu as besoin de : un compte Google, et ton dépôt GitHub `TRUSTPAY` (déjà publié avec GitHub Pages).

## Où vont les signalements ?
Dans un **Google Sheet à toi**, onglet « Signalements ». Chaque signalement arrive avec le statut **EN_ATTENTE** (jaune).
Le site ne montre rien tant que tu n'as pas choisi **CONFIRME** dans la colonne « Statut » (liste déroulante).
Tu reçois aussi un **e-mail** à chaque nouveau signalement.

## Étapes
1. **Crée le Google Sheet.** Va sur sheets.google.com, clique sur **Feuille de calcul vierge**, nomme-la `TrustPay`.
2. **Ouvre l'éditeur de script.** Menu **Extensions > Apps Script**.
3. **Colle le code.** Dans l'éditeur, sélectionne tout (Ctrl+A), supprime, puis colle le contenu de `apps-script/Code.gs`. Clique sur l'icône disquette (Ctrl+S).
4. **Prépare le fichier.** En haut de l'éditeur, dans la liste de fonctions, choisis `configurer` puis clique sur **Exécuter**.
   Google demande une autorisation : **Examiner les autorisations**, choisis ton compte, **Paramètres avancés**, **Accéder à TrustPay (non sécurisé)**, **Autoriser**.
   (Le message « non sécurisé » s'affiche car le script est le tien et n'a pas été validé par Google : c'est normal.)
   Retourne dans ton Sheet : les onglets **Signalements** et **Certifiés** existent maintenant.
5. **Publie le script.** En haut à droite : **Déployer > Nouveau déploiement**. Clique sur la roue dentée, choisis **Application Web**. Règle :
   - **Exécuter en tant que** : Moi
   - **Qui a accès** : Tout le monde
   Clique sur **Déployer**, puis **copie l'URL de l'application Web** (elle finit par `/exec`).
6. **Teste l'URL.** Colle-la dans ton navigateur en ajoutant `?action=sante` à la fin. Tu dois voir `{"ok":true}`.
7. **Branche le site.** Ouvre `index.html` avec le Bloc-notes, cherche `COLLE_ICI_L_ADRESSE_DU_SCRIPT`, remplace-le par ton URL (garde les guillemets), enregistre.
8. **Mets le site en ligne.** Dans le dossier de ton dépôt GitHub, supprime l'ancien contenu (`backend`, `schema*.sql`, etc.), copie le contenu de `trustpay-v1`, puis dans GitHub Desktop : **Commit to main** et **Push origin**. Le site se met à jour en une minute.
9. **Fais le test complet.**
   1. Sur le site, signale un numéro de test. Il apparaît en jaune dans ton Sheet, et tu reçois un e-mail.
   2. Vérifie ce numéro sur le site : **0 signalement** (il est en attente).
   3. Dans le Sheet, mets **CONFIRME** dans « Statut ».
   4. Revérifie le numéro : **1 signalement confirmé**. Remets **REJETE** pour effacer ton test.

## Ajouter un vendeur certifié
Onglet **Certifiés** : numéro (avec +237), nom, note sur 5, nombre de ventes. Le site le reconnaît tout de suite.

## Si tu modifies le code du script plus tard
**Déployer > Gérer les déploiements >** crayon **> Version : Nouvelle version > Déployer**. Sinon l'ancienne version reste active.

## À savoir
- Garde ton Google Sheet **privé** : il contient les descriptions et les numéros de ceux qui signalent. Le site, lui, ne reçoit que des nombres (signalements confirmés) et les noms des vendeurs certifiés.
- Quelqu'un peut envoyer de faux signalements : c'est pour ça que rien ne compte avant ton CONFIRME. Vérifie avant de confirmer (contacte la personne, demande une capture d'écran).

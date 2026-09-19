# Mettre TrustPay en ligne gratuitement (Aiven + Render + GitHub Pages)

Ordre conseillé : GitHub → Aiven → Render → site. Compter environ 1 heure.

## 1. Créer ton compte GitHub et envoyer le projet
1. Va sur github.com, clique **Sign up**, crée un compte gratuit.
2. Clique **New repository**, nom : `trustpay`, coche **Public**, puis **Create repository**.
3. Clique **uploading an existing file**, glisse tout le contenu du dossier `trustpay` (dézippé), puis **Commit changes**.

## 2. Base MySQL sur Aiven
1. Crée un compte sur aiven.io (sans carte bancaire), puis un service **MySQL** avec le plan **Free**.
2. Quand il est prêt, note : **Host**, **Port**, **User**, **Password**.
3. Importe le schéma depuis ton ordinateur (client MySQL installé) :
   `mysql -h HOST -P PORT -u USER -p --ssl-mode=REQUIRED defaultdb < schema-aiven.sql`

## 3. Serveur Java sur Render
1. Crée un compte sur render.com, puis **New > Web Service** et connecte ton dépôt GitHub `trustpay`.
2. **Root Directory** : `backend` · **Language/Runtime** : Docker · **Instance Type** : Free.
3. Dans **Environment**, ajoute :
   - `DB_URL` = `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED`
   - `DB_USER` = le User d'Aiven
   - `DB_PASSWORD` = le Password d'Aiven
   - `CORS_ORIGINS` = `*` pour le premier test (à remplacer à l'étape 5)
4. Clique **Create Web Service** et attends la fin du déploiement (plusieurs minutes).
5. Test : ouvre `https://TON-SERVICE.onrender.com/api/vendeurs/verifier?contact=%2B237690000001`
   Tu dois voir du JSON avec « Kamer Tech Store ».

Attention : sur le plan gratuit, le serveur s'endort après un moment sans visite. Le premier appel peut prendre environ une minute.

## 4. Brancher le site
Dans `index.html`, change la ligne `const API = "http://localhost:8080/api";` en :
`const API = "https://TON-SERVICE.onrender.com/api";`

## 5. Publier le site (GitHub Pages) et sécuriser
1. Dans ton dépôt : **Settings > Pages**, source : branche `main`, dossier `/ (root)`. Ton site sera sur `https://TON-PSEUDO.github.io/trustpay/`.
2. Sur Render, change `CORS_ORIGINS` en `https://TON-PSEUDO.github.io` (sans le chemin, sans « / » final).
3. Ouvre le site : le badge doit afficher **Serveur connecté**.

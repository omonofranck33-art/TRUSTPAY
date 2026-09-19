# TrustPay – Version 1

## Contenu
- `index.html` : le site (HTML + CSS + JavaScript, tout dans un fichier, commenté ligne par ligne)
- `schema.sql` : la base MySQL (3 tables + données de démo)
- `backend/` : le serveur Java (Spring Boot) qui relie le site à la base
- `assets/` : logo et affiche

## Lancer seulement le site (mode démo)
Double-cliquez sur `index.html`. Les 3 outils marchent dans le navigateur.

## Lancer avec la vraie base + Java
1. `mysql -u root -p < schema.sql`
2. Dans `backend/` : `export DB_PASSWORD=votre_mot_de_passe` puis `mvn spring-boot:run`
3. Rouvrez `index.html` : le badge en haut passe à **Serveur connecté**.

## Comment les pièces se parlent
Navigateur (`index.html`) → requête `/api/...` → Java (`TrustPayController`) → SQL (`*Dao`) → MySQL → réponse JSON → affichage.

## Ce qui reste à faire pour une vraie mise en production
- Paiement réel via Mobile Money (MTN MoMo / Orange Money) à la place de la simulation
- Comptes utilisateurs et connexion sécurisée
- Vérification d'identité des vendeurs avant certification
- Restreindre CORS à ton vrai domaine

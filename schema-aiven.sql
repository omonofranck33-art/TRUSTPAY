-- Version pour Aiven : à importer dans la base par défaut (defaultdb)
-- =====================================================================
-- TrustPay : base de données MySQL (version 1)
-- Lancer avec :  mysql -u root -p < schema.sql
-- =====================================================================

-- Crée la base si elle n'existe pas, avec les accents bien gérés (utf8mb4)

-- ---------------------------------------------------------------------
-- TABLE 1 : vendeurs (les personnes qui vendent, certifiées ou non)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vendeurs (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,           -- numéro unique, ajouté automatiquement
  nom           VARCHAR(120) NOT NULL,                       -- nom de la boutique
  telephone     VARCHAR(20)  NOT NULL UNIQUE,                -- UNIQUE : deux vendeurs ne peuvent pas avoir le même numéro
  certifie      BOOLEAN      NOT NULL DEFAULT FALSE,         -- TRUE = profil contrôlé par TrustPay
  note_moyenne  DECIMAL(2,1) NOT NULL DEFAULT 0.0,           -- ex : 4.8 sur 5
  nb_ventes     INT          NOT NULL DEFAULT 0,             -- nombre de ventes réussies
  cree_le       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- date d'inscription
  CONSTRAINT chk_note CHECK (note_moyenne BETWEEN 0 AND 5)   -- la note doit rester entre 0 et 5
);

-- ---------------------------------------------------------------------
-- TABLE 2 : transactions (chaque achat protégé par TrustPay)
-- statut : CREEE -> PAYE_BLOQUE -> LIVRE -> LIBERE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  vendeur_id     BIGINT       NOT NULL,                      -- quel vendeur reçoit l'argent
  acheteur       VARCHAR(20)  NOT NULL,                      -- téléphone de l'acheteur
  produit        VARCHAR(200) NOT NULL,
  montant_fcfa   BIGINT       NOT NULL,                      -- montant en FCFA (entier, pas de centimes)
  statut         VARCHAR(20)  NOT NULL DEFAULT 'CREEE',
  cree_le        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  mis_a_jour_le  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- se met à jour tout seul
  CONSTRAINT fk_trans_vendeur FOREIGN KEY (vendeur_id) REFERENCES vendeurs(id),  -- lien obligatoire vers un vendeur existant
  CONSTRAINT chk_montant CHECK (montant_fcfa > 0),
  CONSTRAINT chk_statut  CHECK (statut IN ('CREEE','PAYE_BLOQUE','LIVRE','LIBERE'))
);
CREATE INDEX idx_trans_statut ON transactions(statut);     -- accélère les recherches par statut

-- ---------------------------------------------------------------------
-- TABLE 3 : signalements (messages suspects analysés, pour améliorer la détection)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS signalements (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  message   TEXT        NOT NULL,
  score     INT         NOT NULL,                            -- score de risque de 0 à 100
  niveau    VARCHAR(10) NOT NULL,                            -- FAIBLE / MOYEN / ÉLEVÉ
  cree_le   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- DONNÉES DE DÉMO (mêmes vendeurs que le mode démo de index.html)
-- INSERT IGNORE : ne fait rien si le numéro existe déjà
-- ---------------------------------------------------------------------
INSERT IGNORE INTO vendeurs (nom, telephone, certifie, note_moyenne, nb_ventes) VALUES
  ('Kamer Tech Store',      '+237690000001', TRUE,  4.8, 132),
  ('Douala Mode',           '+237677000002', TRUE,  4.6, 87),
  ('Promo Express Gratuit', '+237655000003', FALSE, 2.1, 5);

-- ---------------------------------------------------------------------
-- REQUÊTE UTILE : total bloqué en ce moment chez TrustPay, par vendeur
-- ---------------------------------------------------------------------
-- SELECT v.nom, SUM(t.montant_fcfa) AS total_bloque
-- FROM transactions t JOIN vendeurs v ON v.id = t.vendeur_id
-- WHERE t.statut IN ('PAYE_BLOQUE','LIVRE')
-- GROUP BY v.nom;

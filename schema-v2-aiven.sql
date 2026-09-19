-- =====================================================================
-- TrustPay v2 : à exécuter UNE FOIS dans DBeaver (base defaultdb sur Aiven)
-- Ajoute : les comptes utilisateurs et les signalements d'arnaqueurs.
-- Sans danger si on le relance : "IF NOT EXISTS" ignore ce qui existe déjà.
-- =====================================================================

-- TABLE : utilisateurs (comptes créés depuis le site)
CREATE TABLE IF NOT EXISTS utilisateurs (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  nom               VARCHAR(100) NOT NULL,
  telephone         VARCHAR(20)  NOT NULL UNIQUE,      -- format normalisé : +237XXXXXXXXX
  mot_de_passe_hash VARCHAR(100) NOT NULL,             -- JAMAIS le mot de passe en clair : uniquement son empreinte BCrypt
  role              VARCHAR(10)  NOT NULL DEFAULT 'ACHETEUR',
  cree_le           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_role CHECK (role IN ('ACHETEUR', 'VENDEUR'))
);

-- TABLE : signalements_arnaque (personnes signalées par les utilisateurs)
CREATE TABLE IF NOT EXISTS signalements_arnaque (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  telephone_signale  VARCHAR(20)  NOT NULL,            -- numéro de l'arnaqueur présumé
  plateforme         VARCHAR(30)  NOT NULL,            -- WhatsApp, Facebook, etc.
  description        TEXT         NOT NULL,            -- ce qui s'est passé
  montant_perdu_fcfa BIGINT       NULL,                -- facultatif
  contact_declarant  VARCHAR(20)  NULL,                -- facultatif : pour recontacter la victime
  statut             VARCHAR(15)  NOT NULL DEFAULT 'A_VERIFIER',  -- A_VERIFIER -> CONFIRME ou REJETE
  cree_le            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sign_tel (telephone_signale),              -- accélère la recherche par numéro
  CONSTRAINT chk_statut_sign CHECK (statut IN ('A_VERIFIER', 'CONFIRME', 'REJETE'))
);

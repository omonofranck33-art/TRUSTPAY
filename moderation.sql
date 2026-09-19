-- =====================================================================
-- TrustPay : vérifier les signalements (à lancer dans DBeaver, connexion Aiven, base defaultdb)
-- Un signalement arrive avec le statut A_VERIFIER : il n'apparaît PAS sur le site.
-- Il n'avertit les acheteurs qu'après être passé à CONFIRME.
-- =====================================================================

-- 1. Voir les signalements en attente (les plus anciens d'abord)
SELECT id, telephone_signale, plateforme, description, montant_perdu_fcfa, contact_declarant, cree_le
FROM signalements_arnaque
WHERE statut = 'A_VERIFIER'
ORDER BY cree_le;

-- 2. Voir si ce numéro a déjà d'autres signalements (remplace le numéro)
-- SELECT id, statut, plateforme, description FROM signalements_arnaque WHERE telephone_signale = '+237600000000';

-- 3. CONFIRMER un signalement après vérification (remplace 12 par le numéro d'id)
-- UPDATE signalements_arnaque SET statut = 'CONFIRME' WHERE id = 12;

-- 4. REJETER un signalement faux ou impossible à prouver (remplace 12 par le numéro d'id)
-- UPDATE signalements_arnaque SET statut = 'REJETE' WHERE id = 12;

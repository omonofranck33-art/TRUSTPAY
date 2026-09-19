package cm.trustpay.model;

/** Une ligne de la table "utilisateurs". */
public record Utilisateur(long id, String nom, String telephone, String motDePasseHash, String role) { }

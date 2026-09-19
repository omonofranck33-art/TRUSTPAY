package cm.trustpay.model;

/** Une ligne de la table "transactions". */
public record Transaction(long id, long vendeurId, String acheteur, String produit,
                          long montantFcfa, StatutTransaction statut) { }

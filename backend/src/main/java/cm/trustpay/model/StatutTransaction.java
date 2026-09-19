package cm.trustpay.model;

/** Les 4 étapes d'un achat protégé. L'ordre ici = l'ordre réel de la vie d'une transaction. */
public enum StatutTransaction {
    CREEE, PAYE_BLOQUE, LIVRE, LIBERE;

    /** Renvoie l'étape suivante ; refuse d'aller plus loin après LIBERE. */
    public StatutTransaction suivant() {
        if (this == LIBERE) {
            throw new IllegalStateException("Transaction déjà terminée");
        }
        return values()[ordinal() + 1];   // ordinal() = position (0,1,2,3) ; +1 = étape d'après
    }
}

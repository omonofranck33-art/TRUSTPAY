package cm.trustpay.service;

import java.util.regex.Pattern;

/**
 * Met tous les numéros camerounais dans UN SEUL format : +237XXXXXXXXX (sans espace).
 * Ainsi "+237 691 000 001", "+237691000001", "691000001" et "00237 691 000 001"
 * désignent exactement le même numéro dans la base de données.
 */
public final class Telephone {

    // 9 chiffres commençant par 6 (mobiles) ou 2 (fixes)
    private static final Pattern LOCAL = Pattern.compile("^[26]\\d{8}$");

    private Telephone() { }                       // classe utilitaire : on ne la crée jamais

    /** Renvoie le numéro normalisé, ou null s'il est invalide. */
    public static String normaliser(String saisie) {
        if (saisie == null) return null;
        String chiffres = saisie.replaceAll("\\D", "");        // garde seulement les chiffres
        if (chiffres.startsWith("00237")) {                    // 00237 691... -> 691...
            chiffres = chiffres.substring(5);
        } else if (chiffres.length() == 12 && chiffres.startsWith("237")) {   // 237691... -> 691...
            chiffres = chiffres.substring(3);
        }
        return LOCAL.matcher(chiffres).matches() ? "+237" + chiffres : null;
    }
}

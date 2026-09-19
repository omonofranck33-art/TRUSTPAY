package cm.trustpay.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Détection d'arnaque par règles (version 1).
 * Chaque règle = un motif de texte + des points de risque.
 * Les règles sont identiques à celles de index.html.
 * Plus tard, on pourra remplacer par un vrai modèle d'IA sans changer le reste du code.
 */
@Service
public class AnalyseurArnaque {

    private record Regle(Pattern motif, int points, String raison) { }

    private static Regle r(String regex, int points, String raison) {
        return new Regle(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), points, raison);
    }

    private static final List<Regle> REGLES = List.of(
        r("western union|mtcn",                                   30, "Transfert via un service difficile à tracer"),
        r("avance|d'abord|en premier|payer avant",                25, "Demande de paiement à l'avance"),
        r("urgent|vite|dernière chance|aujourd'hui seulement",    15, "Pression sur l'urgence"),
        r("\\b(code|otp|mot de passe|pin)\\b",                    35, "Demande d'un code ou d'un mot de passe"),
        r("gagné|gagne|félicitations|loterie|cadeau",             30, "Promesse de gain ou de cadeau"),
        r("https?://|bit\\.ly|tinyurl",                           15, "Lien à vérifier avant de cliquer"),
        r("frais de (douane|dossier|livraison)|débloquer",        25, "Frais demandés pour débloquer un colis ou un gain"),
        r("ne dis à personne|ne le dis à personne|confidentiel",  15, "Demande de garder le secret")
    );

    /** Résultat renvoyé au site web (converti en JSON automatiquement). */
    public record Resultat(int score, String niveau, List<String> raisons) { }

    public Resultat analyser(String message) {
        int score = 0;
        List<String> raisons = new ArrayList<>();
        for (Regle regle : REGLES) {                        // on teste chaque règle
            if (regle.motif().matcher(message).find()) {    // le motif apparaît-il dans le message ?
                score += regle.points();
                raisons.add(regle.raison());
            }
        }
        score = Math.min(score, 100);                       // maximum 100
        String niveau = score >= 50 ? "ÉLEVÉ" : score >= 20 ? "MOYEN" : "FAIBLE";
        return new Resultat(score, niveau, raisons);
    }
}

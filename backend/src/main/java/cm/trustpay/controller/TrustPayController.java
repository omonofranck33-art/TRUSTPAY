package cm.trustpay.controller;

import cm.trustpay.model.StatutTransaction;
import cm.trustpay.model.Transaction;
import cm.trustpay.model.Vendeur;
import cm.trustpay.repository.TransactionDao;
import cm.trustpay.repository.VendeurDao;
import cm.trustpay.service.AnalyseurArnaque;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;

/** Le controller = la "réception" : il reçoit les requêtes du site et renvoie du JSON. */
@RestController
@RequestMapping("/api")                                   // toutes les URL commencent par /api
public class TrustPayController {

    private final VendeurDao vendeurs;
    private final TransactionDao transactions;
    private final AnalyseurArnaque analyseur;

    public TrustPayController(VendeurDao vendeurs, TransactionDao transactions, AnalyseurArnaque analyseur) {
        this.vendeurs = vendeurs; this.transactions = transactions; this.analyseur = analyseur;
    }

    // ----- Formats JSON échangés avec le site -----
    public record ReponseVendeur(boolean trouve, String nom, boolean certifie, BigDecimal note, int nbVentes) { }
    public record DemandeMessage(String message) { }
    public record DemandeTransaction(String contactVendeur, String acheteur, String produit, long montantFcfa) { }

    /** GET /api/vendeurs/verifier?contact=+237690000001 */
    @GetMapping("/vendeurs/verifier")
    public ReponseVendeur verifier(@RequestParam String contact) {
        String propre = contact.replaceAll("\\s+", "");   // enlève les espaces du numéro
        Optional<Vendeur> v = vendeurs.parTelephone(propre);
        return v.map(x -> new ReponseVendeur(true, x.nom(), x.certifie(), x.noteMoyenne(), x.nbVentes()))
                .orElse(new ReponseVendeur(false, null, false, BigDecimal.ZERO, 0));
    }

    /** POST /api/alertes/analyser  { "message": "..." } */
    @PostMapping("/alertes/analyser")
    public AnalyseurArnaque.Resultat analyser(@RequestBody DemandeMessage demande) {
        if (demande.message() == null || demande.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message vide");
        }
        AnalyseurArnaque.Resultat res = analyseur.analyser(demande.message());
        transactions.enregistrerSignalement(demande.message(), res.score(), res.niveau()); // on garde une trace
        return res;
    }

    /** POST /api/transactions : crée un achat protégé (uniquement chez un vendeur certifié). */
    @PostMapping("/transactions")
    public Transaction creer(@RequestBody DemandeTransaction d) {
        Vendeur v = vendeurs.parTelephone(d.contactVendeur().replaceAll("\\s+", ""))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendeur inconnu"));
        if (!v.certifie()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendeur non certifié");
        }
        if (d.montantFcfa() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant invalide");
        }
        long id = transactions.creer(v.id(), d.acheteur(), d.produit(), d.montantFcfa());
        return transactions.parId(id).orElseThrow();
    }

    /**
     * POST /api/transactions/{id}/avancer : passe à l'étape suivante.
     * Version 1 = simulation. Version réelle : chaque étape sera déclenchée par
     * Mobile Money (paiement) et par des vérifications d'identité (vendeur / acheteur).
     */
    @PostMapping("/transactions/{id}/avancer")
    public Transaction avancer(@PathVariable long id) {
        Transaction t = transactions.parId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction introuvable"));
        try {
            StatutTransaction suivant = t.statut().suivant();
            transactions.changerStatut(id, suivant);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return transactions.parId(id).orElseThrow();
    }
}

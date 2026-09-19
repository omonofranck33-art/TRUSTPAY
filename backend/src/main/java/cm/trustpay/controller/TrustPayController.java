package cm.trustpay.controller;

import cm.trustpay.model.StatutTransaction;
import cm.trustpay.model.Transaction;
import cm.trustpay.model.Vendeur;
import cm.trustpay.repository.SignalementArnaqueDao;
import cm.trustpay.repository.TransactionDao;
import cm.trustpay.repository.UtilisateurDao;
import cm.trustpay.repository.VendeurDao;
import cm.trustpay.service.AnalyseurArnaque;
import cm.trustpay.service.Telephone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Le controller = la "réception" : il reçoit les requêtes du site et renvoie du JSON. */
@RestController
@RequestMapping("/api")                                   // toutes les URL commencent par /api
public class TrustPayController {

    private static final List<String> PLATEFORMES = List.of("WhatsApp", "Facebook", "Telegram", "SMS ou appel", "Autre");

    private final VendeurDao vendeurs;
    private final TransactionDao transactions;
    private final UtilisateurDao utilisateurs;
    private final SignalementArnaqueDao signalements;
    private final AnalyseurArnaque analyseur;
    private final PasswordEncoder encodeur = new BCryptPasswordEncoder();   // chiffre les mots de passe

    public TrustPayController(VendeurDao vendeurs, TransactionDao transactions, UtilisateurDao utilisateurs,
                              SignalementArnaqueDao signalements, AnalyseurArnaque analyseur) {
        this.vendeurs = vendeurs; this.transactions = transactions; this.utilisateurs = utilisateurs;
        this.signalements = signalements; this.analyseur = analyseur;
    }

    // ----- Formats JSON échangés avec le site -----
    public record ReponseVendeur(boolean trouve, String nom, boolean certifie, BigDecimal note, int nbVentes, int signalements) { }
    public record DemandeMessage(String message) { }
    public record DemandeTransaction(String contactVendeur, String acheteur, String produit, long montantFcfa) { }
    public record DemandeCompte(String nom, String telephone, String motDePasse, String role) { }
    public record DemandeSignalement(String numero, String plateforme, String description, Long montantFcfa, String contactDeclarant) { }

    /** Petit "ping" : sert à réveiller le serveur gratuit dès l'ouverture du site. */
    @GetMapping("/sante")
    public Map<String, Boolean> sante() {
        return Map.of("ok", true);
    }

    /** Numéro valide (format +237XXXXXXXXX) ou erreur 400 avec un message clair. */
    private String numeroValide(String saisie, String nomChamp) {
        String tel = Telephone.normaliser(saisie);
        if (tel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, nomChamp + " invalide (9 chiffres, ex : +237 690 000 001)");
        }
        return tel;
    }

    /** GET /api/vendeurs/verifier?contact=+237690000001 (les espaces et le +237 sont facultatifs) */
    @GetMapping("/vendeurs/verifier")
    public ReponseVendeur verifier(@RequestParam String contact) {
        String tel = numeroValide(contact, "Numéro");
        int nbSignalements = signalements.compter(tel);
        Optional<Vendeur> v = vendeurs.parTelephone(tel);
        return v.map(x -> new ReponseVendeur(true, x.nom(), x.certifie(), x.noteMoyenne(), x.nbVentes(), nbSignalements))
                .orElse(new ReponseVendeur(false, null, false, BigDecimal.ZERO, 0, nbSignalements));
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

    /** POST /api/signalements/arnaqueurs : le signalement est mis en attente (A_VERIFIER) et ne compte qu'une fois confirmé par l'équipe. */
    @PostMapping("/signalements/arnaqueurs")
    public Map<String, String> signaler(@RequestBody DemandeSignalement d) {
        String numero = numeroValide(d.numero(), "Numéro de l'arnaqueur");
        String declarant = (d.contactDeclarant() == null || d.contactDeclarant().isBlank())
                ? null : numeroValide(d.contactDeclarant(), "Votre numéro");
        if (d.plateforme() == null || !PLATEFORMES.contains(d.plateforme())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez où l'arnaque a eu lieu");
        }
        if (d.description() == null || d.description().trim().length() < 10 || d.description().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Décrivez ce qui s'est passé (10 à 1000 caractères)");
        }
        if (d.montantFcfa() != null && d.montantFcfa() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant invalide");
        }
        long ref = signalements.enregistrer(numero, d.plateforme(), d.description().trim(), d.montantFcfa(), declarant);
        return Map.of("message", "Signalement reçu. Il sera vérifié par l'équipe TrustPay avant d'être pris en compte.", "reference", String.valueOf(ref));
    }

    /**
     * POST /api/comptes/creer : inscription.
     * Le mot de passe est chiffré (BCrypt) avant d'aller en base. Un vendeur reçoit aussi un profil NON certifié :
     * la certification reste un contrôle fait par TrustPay.
     */
    @PostMapping("/comptes/creer")
    public Map<String, String> creerCompte(@RequestBody DemandeCompte d) {
        String tel = numeroValide(d.telephone(), "Numéro de téléphone");
        String nom = d.nom() == null ? "" : d.nom().trim();
        if (nom.length() < 2 || nom.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom invalide (2 à 100 caractères)");
        }
        if (d.motDePasse() == null || d.motDePasse().length() < 8 || d.motDePasse().length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe : 8 à 72 caractères");
        }
        String role = "VENDEUR".equalsIgnoreCase(d.role()) ? "VENDEUR" : "ACHETEUR";
        if (utilisateurs.existe(tel)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro a déjà un compte");
        }
        try {
            utilisateurs.creer(nom, tel, encodeur.encode(d.motDePasse()), role);
        } catch (DuplicateKeyException e) {          // deux inscriptions au même instant
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro a déjà un compte");
        }
        if (role.equals("VENDEUR")) {
            vendeurs.creerNonCertifie(nom, tel);
        }
        return Map.of("nom", nom, "role", role, "message", "Compte créé");
    }

    /** POST /api/transactions : crée un achat protégé (uniquement chez un vendeur certifié). */
    @PostMapping("/transactions")
    public Transaction creer(@RequestBody DemandeTransaction d) {
        String telVendeur = numeroValide(d.contactVendeur(), "Numéro du vendeur");
        String telAcheteur = numeroValide(d.acheteur(), "Numéro de l'acheteur");
        Vendeur v = vendeurs.parTelephone(telVendeur)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendeur inconnu"));
        if (!v.certifie()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendeur non certifié");
        }
        if (d.montantFcfa() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant invalide");
        }
        long id = transactions.creer(v.id(), telAcheteur, d.produit(), d.montantFcfa());
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

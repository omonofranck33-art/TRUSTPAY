package cm.trustpay.repository;

import cm.trustpay.model.StatutTransaction;
import cm.trustpay.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class TransactionDao {
    private final JdbcTemplate jdbc;
    public TransactionDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Insère une nouvelle transaction et renvoie son numéro (id) généré par MySQL. */
    public long creer(long vendeurId, String acheteur, String produit, long montantFcfa) {
        KeyHolder cle = new GeneratedKeyHolder();    // récupère l'id créé automatiquement
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO transactions (vendeur_id, acheteur, produit, montant_fcfa) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, vendeurId);
            ps.setString(2, acheteur);
            ps.setString(3, produit);
            ps.setLong(4, montantFcfa);
            return ps;
        }, cle);
        return cle.getKey().longValue();
    }

    public Optional<Transaction> parId(long id) {
        return jdbc.query(
            "SELECT id, vendeur_id, acheteur, produit, montant_fcfa, statut FROM transactions WHERE id = ?",
            (rs, i) -> new Transaction(rs.getLong("id"), rs.getLong("vendeur_id"), rs.getString("acheteur"),
                                       rs.getString("produit"), rs.getLong("montant_fcfa"),
                                       StatutTransaction.valueOf(rs.getString("statut"))),
            id
        ).stream().findFirst();
    }

    public void changerStatut(long id, StatutTransaction statut) {
        jdbc.update("UPDATE transactions SET statut = ? WHERE id = ?", statut.name(), id);
    }

    /** Enregistre un message analysé (table signalements). */
    public void enregistrerSignalement(String message, int score, String niveau) {
        jdbc.update("INSERT INTO signalements (message, score, niveau) VALUES (?, ?, ?)", message, score, niveau);
    }
}

package cm.trustpay.repository;

import cm.trustpay.model.Vendeur;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/** DAO = classe dont le seul rôle est de parler à la base de données. */
@Repository
public class VendeurDao {
    private final JdbcTemplate jdbc;                 // outil Spring qui exécute le SQL

    public VendeurDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }  // Spring l'injecte automatiquement

    public Optional<Vendeur> parTelephone(String telephone) {
        // Les "?" sont remplacés par la valeur : cela bloque les injections SQL (piratage classique)
        return jdbc.query(
            "SELECT id, nom, telephone, certifie, note_moyenne, nb_ventes FROM vendeurs WHERE telephone = ?",
            (rs, i) -> new Vendeur(rs.getLong("id"), rs.getString("nom"), rs.getString("telephone"),
                                   rs.getBoolean("certifie"), rs.getBigDecimal("note_moyenne"), rs.getInt("nb_ventes")),
            telephone
        ).stream().findFirst();                      // premier résultat s'il existe, sinon vide
    }
}

package cm.trustpay.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UtilisateurDao {
    private final JdbcTemplate jdbc;
    public UtilisateurDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean existe(String telephone) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM utilisateurs WHERE telephone = ?", Integer.class, telephone);
        return n != null && n > 0;
    }

    /** On reçoit déjà le mot de passe chiffré (hash) : jamais le mot de passe en clair. role = ACHETEUR ou VENDEUR. */
    public void creer(String nom, String telephone, String motDePasseHash, String role) {
        jdbc.update("INSERT INTO utilisateurs (nom, telephone, mot_de_passe_hash, role) VALUES (?, ?, ?, ?)",
                    nom, telephone, motDePasseHash, role);
    }
}

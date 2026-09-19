package cm.trustpay.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

@Repository
public class SignalementArnaqueDao {
    private final JdbcTemplate jdbc;
    public SignalementArnaqueDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Enregistre un signalement et renvoie son numéro de référence. */
    public long enregistrer(String telephoneSignale, String plateforme, String description,
                            Long montantFcfa, String contactDeclarant) {
        KeyHolder cle = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO signalements_arnaque (telephone_signale, plateforme, description, montant_perdu_fcfa, contact_declarant) "
              + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, telephoneSignale);
            ps.setString(2, plateforme);
            ps.setString(3, description);
            if (montantFcfa == null) ps.setNull(4, Types.BIGINT); else ps.setLong(4, montantFcfa);   // champ facultatif
            if (contactDeclarant == null) ps.setNull(5, Types.VARCHAR); else ps.setString(5, contactDeclarant);
            return ps;
        }, cle);
        return cle.getKey().longValue();
    }

    /**
     * Combien de signalements CONFIRMÉS par l'équipe pour ce numéro.
     * Les signalements A_VERIFIER (en attente) et REJETE ne comptent pas : personne n'est accusé sans contrôle.
     */
    public int compter(String telephoneSignale) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM signalements_arnaque WHERE telephone_signale = ? AND statut = 'CONFIRME'",
            Integer.class, telephoneSignale);
        return n == null ? 0 : n;
    }
}

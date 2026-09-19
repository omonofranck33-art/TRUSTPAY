package cm.trustpay.model;

import java.math.BigDecimal;

/** "record" = classe compacte : Java crée tout seul le constructeur et les getters (id(), nom()...). */
public record Vendeur(long id, String nom, String telephone, boolean certifie,
                      BigDecimal noteMoyenne, int nbVentes) { }

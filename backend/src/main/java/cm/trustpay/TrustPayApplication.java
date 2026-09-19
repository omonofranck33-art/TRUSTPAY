package cm.trustpay;                                  // "adresse" du fichier dans le projet

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication                                // dit à Spring : "configure tout automatiquement"
public class TrustPayApplication {
    public static void main(String[] args) {          // point d'entrée : premier code exécuté
        SpringApplication.run(TrustPayApplication.class, args); // démarre le serveur web
    }
}

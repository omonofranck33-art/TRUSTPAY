/**
 * TrustPay – serveur version 1 (Google Apps Script + Google Sheets)
 * ------------------------------------------------------------------
 * Ce script vit DANS ton Google Sheet. Il fait 3 choses :
 *   1. recevoir un signalement d'arnaqueur -> il est écrit dans l'onglet « Signalements » avec le statut EN_ATTENTE
 *   2. vérifier un numéro -> il compte SEULEMENT les signalements passés à CONFIRME par toi
 *   3. te prévenir par e-mail à chaque nouveau signalement
 * Rien n'est visible du public tant que tu n'as pas mis CONFIRME dans la colonne « Statut ».
 */

const FEUILLE_SIGNALEMENTS = "Signalements";
const FEUILLE_CERTIFIES = "Certifiés";
const PLATEFORMES = ["WhatsApp", "Facebook", "Telegram", "SMS ou appel", "Autre"];
const STATUTS = ["EN_ATTENTE", "CONFIRME", "REJETE"];

/* ===================== FONCTIONS SIMPLES (sans Google) ===================== */

// Met un numéro camerounais sous la forme +237XXXXXXXXX ; renvoie null s'il est invalide.
// "+237 691 000 001", "+237691000001", "691000001" et "00237691000001" donnent le même résultat.
function normaliser(saisie) {
  let c = String(saisie || "").replace(/\D/g, "");                         // garde seulement les chiffres
  if (c.indexOf("00237") === 0) c = c.slice(5);                            // 00237691... -> 691...
  else if (c.length === 12 && c.indexOf("237") === 0) c = c.slice(3);      // 237691... -> 691...
  return /^[26]\d{8}$/.test(c) ? "+237" + c : null;                        // 9 chiffres commençant par 6 ou 2
}

// Clé de comparaison : les 9 derniers chiffres (Google Sheets peut transformer "+237..." en nombre)
function cle(numero) {
  return String(numero || "").replace(/\D/g, "").slice(-9);
}

// Cherche un numéro dans les lignes des deux onglets. Ne renvoie QUE des chiffres et des noms de vendeurs
// certifiés : jamais le texte des signalements ni le contact de ceux qui signalent.
function verifierDansDonnees(numero, lignesSignalements, lignesCertifies) {
  const n = normaliser(numero);
  if (!n) return { ok: false, message: "Numéro invalide : il faut 9 chiffres, par exemple +237 690 000 001." };
  const k = cle(n);

  let confirmes = 0;
  lignesSignalements.forEach(function (l) {
    if (cle(l[1]) === k && String(l[6]).trim() === "CONFIRME") confirmes++;
  });

  let certifie = false, nom = "", note = "", nbVentes = "";
  lignesCertifies.forEach(function (l) {
    if (!certifie && cle(l[0]) === k) { certifie = true; nom = String(l[1]); note = String(l[2]); nbVentes = String(l[3]); }
  });

  return { ok: true, numero: n, certifie: certifie, nom: nom, note: note, nbVentes: nbVentes, signalements: confirmes };
}

function json(objet) {
  return ContentService.createTextOutput(JSON.stringify(objet)).setMimeType(ContentService.MimeType.JSON);
}

/* ===================== INSTALLATION (à lancer UNE seule fois) ===================== */

function configurer() {
  const classeur = SpreadsheetApp.getActiveSpreadsheet();

  // Onglet 1 : les signalements reçus
  let sig = classeur.getSheetByName(FEUILLE_SIGNALEMENTS) || classeur.insertSheet(FEUILLE_SIGNALEMENTS);
  if (sig.getLastRow() === 0) {
    sig.appendRow(["Date", "Numéro signalé", "Plateforme", "Description", "Somme perdue (FCFA)",
                   "Contact du déclarant", "Statut", "Note interne"]);
  }
  sig.setFrozenRows(1);
  sig.getRange("A1:H1").setFontWeight("bold").setBackground("#222222").setFontColor("#ffffff");
  sig.setColumnWidth(4, 420);                                   // colonne Description plus large
  const regles = [
    SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("EN_ATTENTE").setBackground("#fce8b2").setRanges([sig.getRange("G2:G")]).build(),
    SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("CONFIRME").setBackground("#b7e1cd").setRanges([sig.getRange("G2:G")]).build(),
    SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("REJETE").setBackground("#dddddd").setRanges([sig.getRange("G2:G")]).build()
  ];
  sig.setConditionalFormatRules(regles);                         // jaune = à vérifier, vert = confirmé, gris = rejeté

  // Onglet 2 : les vendeurs que TU as vérifiés et certifiés (tu les ajoutes à la main)
  let cert = classeur.getSheetByName(FEUILLE_CERTIFIES) || classeur.insertSheet(FEUILLE_CERTIFIES);
  if (cert.getLastRow() === 0) {
    cert.appendRow(["Numéro", "Nom du vendeur", "Note (sur 5)", "Nombre de ventes"]);
  }
  cert.setFrozenRows(1);
  cert.getRange("A1:D1").setFontWeight("bold").setBackground("#222222").setFontColor("#ffffff");
  cert.getRange("A:A").setNumberFormat("@");                     // le numéro reste du texte (+237...)

  const feuilleVide = classeur.getSheetByName("Feuille 1") || classeur.getSheetByName("Sheet1");
  if (feuilleVide && classeur.getSheets().length > 1) classeur.deleteSheet(feuilleVide);
}

/* ===================== RÉPONSES AU SITE ===================== */

// Le site demande : ?action=verifier&numero=+237690000001   (ou ?action=sante pour tester)
function doGet(e) {
  const action = (e && e.parameter && e.parameter.action) || "sante";
  if (action === "verifier") {
    const classeur = SpreadsheetApp.getActiveSpreadsheet();
    return json(verifierDansDonnees(e.parameter.numero, lignes(classeur, FEUILLE_SIGNALEMENTS, 8), lignes(classeur, FEUILLE_CERTIFIES, 4)));
  }
  return json({ ok: true });
}

// Lit les lignes d'un onglet (sans la ligne d'en-tête)
function lignes(classeur, nomFeuille, nbColonnes) {
  const f = classeur.getSheetByName(nomFeuille);
  if (!f || f.getLastRow() < 2) return [];
  return f.getRange(2, 1, f.getLastRow() - 1, nbColonnes).getValues();
}

// Le site envoie un signalement (POST)
function doPost(e) {
  try {
    const d = JSON.parse(e.postData.contents);

    if (d.site) return json({ ok: true, reference: "0" });       // piège anti-robots : ce champ caché doit rester vide

    const numero = normaliser(d.numero);
    if (!numero) return json({ ok: false, message: "Numéro de l'arnaqueur invalide (9 chiffres)." });
    if (PLATEFORMES.indexOf(d.plateforme) === -1) return json({ ok: false, message: "Choisissez où l'arnaque a eu lieu." });
    const description = String(d.description || "").trim();
    if (description.length < 10 || description.length > 1000) return json({ ok: false, message: "Décrivez ce qui s'est passé (10 à 1000 caractères)." });

    let contact = "";
    if (d.contact) {
      contact = normaliser(d.contact);
      if (!contact) return json({ ok: false, message: "Votre numéro est invalide (9 chiffres), ou laissez-le vide." });
    }
    let somme = "";
    if (d.montant !== null && d.montant !== undefined && d.montant !== "") {
      const m = Number(d.montant);
      if (!isFinite(m) || m < 0) return json({ ok: false, message: "Somme perdue invalide." });
      somme = String(Math.round(m));
    }

    const verrou = LockService.getScriptLock();                   // évite que 2 signalements simultanés s'écrasent
    verrou.waitLock(15000);
    let classeur, ligne;
    try {
      classeur = SpreadsheetApp.getActiveSpreadsheet();
      const feuille = classeur.getSheetByName(FEUILLE_SIGNALEMENTS);
      ligne = feuille.getLastRow() + 1;
      const date = dateDuJour();
      // Format « texte » sur toute la ligne : une description qui commence par "=" ne peut pas s'exécuter comme une formule
      feuille.getRange(ligne, 1, 1, 8).setNumberFormats([["@", "@", "@", "@", "@", "@", "@", "@"]])
             .setValues([[date, numero, d.plateforme, description, somme, contact, "EN_ATTENTE", ""]]);
      feuille.getRange(ligne, 7).setDataValidation(               // liste déroulante : EN_ATTENTE / CONFIRME / REJETE
        SpreadsheetApp.newDataValidation().requireValueInList(STATUTS, true).build());
    } finally {
      verrou.releaseLock();
    }

    try {                                                          // e-mail au propriétaire du fichier
      MailApp.sendEmail(Session.getEffectiveUser().getEmail(), "TrustPay : nouveau signalement à vérifier",
        "Numéro signalé : " + numero + "\nPlateforme : " + d.plateforme + "\n\nDescription :\n" + description +
        "\n\nOuvre le fichier, onglet « Signalements », puis mets CONFIRME ou REJETE dans la colonne Statut :\n" + classeur.getUrl());
    } catch (erreurMail) { /* si l'e-mail échoue, le signalement est quand même enregistré */ }

    return json({ ok: true, reference: String(ligne - 1) });
  } catch (erreur) {
    return json({ ok: false, message: "Erreur du serveur, réessayez dans un instant." });
  }
}

function dateDuJour() {
  return Utilities.formatDate(new Date(), "Africa/Douala", "dd/MM/yyyy HH:mm");
}

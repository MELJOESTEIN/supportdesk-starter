-- Jeu de donnees de developpement.
--
-- Deux comptes portes par des utilisateurs Keycloak : CLI-0001 (alice) et
-- CLI-0002 (david). Les autres references peuplent la file agent et ne sont
-- visibles que d'un AGENT. C'est ce decoupage qui rend la demonstration BOLA
-- du J2 possible : alice ne doit jamais lire un ticket de CLI-0002.

INSERT INTO agent (username, nom_complet, niveau, equipe, actif) VALUES
    ('bob',   'Bob Lefevre',  'Niveau 2', 'Facturation', TRUE),
    ('carol', 'Carol Nguyen', 'Niveau 3', 'Facturation', TRUE);

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4821', 'Facture de mars non telechargeable', 'Depuis ce matin le bouton de telechargement de la facture de mars ne repond plus. Les factures d''avril et mai se telechargent normalement. J''ai essaye sur Chrome et Firefox, meme resultat.', 'FACTURATION', 'OUVERT', 'NORMALE',
     'CLI-0001', 'alice', 'bob', '2026-08-24T08:40:00+00:00', '2026-08-29T08:40:00+00:00',
     'alice', '2026-08-24T10:40:00+00:00', '2026-08-24T09:14:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Bonjour, depuis ce matin le bouton de telechargement de la facture de mars ne repond plus. Les factures d''avril et mai se telechargent normalement.', 'PUBLIC', '2026-08-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Documents anterieurs a la migration du 12 mars : le lien signe pointe vers l''ancien bucket. Ticket infra INF-217 ouvert. Ne pas mentionner la migration au client, la communication officielle part vendredi.', 'INTERNE', '2026-08-24T08:52:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Bonjour, merci pour ces precisions. Je reproduis le probleme sur la facture de mars uniquement. Je la regenere et vous reviens aujourd''hui.', 'PUBLIC', '2026-08-24T09:14:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Regeneration lancee, INF-217 toujours en attente cote infra. Si rien lundi, j''envoie le PDF a la main.', 'INTERNE', '2026-08-28T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Oui, par courriel ce serait parfait. Merci.', 'PUBLIC', '2026-08-29T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4821';

-- TCK-4821 est le ticket lu en seance : son fil et son journal doivent raconter une
-- histoire possible. Tout y etait date de 08:40 — la note interne de carol precedait donc
-- la creation du ticket, et le journal se lisait dans un ordre qui dependait du plan
-- d'execution de Postgres. Les horodatages sont desormais monotones ; le depart d'egalite
-- par identifiant, cote requete, couvre le cas general (voir EvenementRepository).
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-08-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-24T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'carol', 'note interne ajoutee', '2026-08-24T08:52:00+00:00' FROM ticket WHERE reference = 'TCK-4821';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-28T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4821';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4790', 'Acces refuse au tableau de bord depuis lundi', 'Depuis lundi matin, le tableau de bord renvoie une page blanche apres la connexion.', 'ACCES', 'EN_COURS', 'HAUTE',
     'CLI-0001', 'alice', 'bob', '2026-08-19T08:40:00+00:00', '2026-08-29T08:40:00+00:00',
     'bob', '2026-08-19T10:40:00+00:00', '2026-08-20T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Depuis lundi, impossible d''acceder au tableau de bord.', 'PUBLIC', '2026-08-19T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Bonjour, je regarde cela tout de suite.', 'PUBLIC', '2026-08-20T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Role manquant sur le compte apres la migration du realm. A verifier avec l''equipe identite.', 'INTERNE', '2026-08-20T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Merci, tenez-moi au courant.', 'PUBLIC', '2026-08-21T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Le role a ete retabli, pouvez-vous reessayer ?', 'PUBLIC', '2026-08-29T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-08-19T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-19T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4790';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-20T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4790';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4712', 'Ajouter un second utilisateur au contrat', 'Nous souhaitons ajouter un second utilisateur au contrat en cours.', 'EVOLUTION', 'EN_COURS', 'BASSE',
     'CLI-0001', 'alice', 'carol', '2026-08-11T08:40:00+00:00', '2026-08-28T08:40:00+00:00',
     'carol', '2026-08-11T10:40:00+00:00', '2026-08-12T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Bonjour, nous souhaitons ajouter un utilisateur.', 'PUBLIC', '2026-08-11T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4712';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Bonjour, je transmets au service commercial.', 'PUBLIC', '2026-08-12T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4712';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Avenant a produire, voir Sophie au commercial.', 'INTERNE', '2026-08-12T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4712';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'L''avenant vous a ete envoye par courriel.', 'PUBLIC', '2026-08-28T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4712';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-08-11T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4712';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-08-11T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4712';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'carol', 'note interne ajoutee', '2026-08-12T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4712';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4655', 'Export CSV tronque au-dela de 5 000 lignes', 'L''export CSV s''arrete a 5 000 lignes sans message d''erreur.', 'ANOMALIE', 'RESOLU', 'NORMALE',
     'CLI-0001', 'alice', 'bob', '2026-08-02T08:40:00+00:00', '2026-08-14T08:40:00+00:00',
     'bob', '2026-08-02T10:40:00+00:00', '2026-08-03T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'L''export s''arrete a 5 000 lignes.', 'PUBLIC', '2026-08-02T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4655';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Reproduit. Correctif prevu cette semaine.', 'PUBLIC', '2026-08-03T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4655';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Limite codee en dur dans le service d''export. Ticket DEV-882.', 'INTERNE', '2026-08-03T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4655';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Le correctif est en ligne, l''export est complet.', 'PUBLIC', '2026-08-14T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4655';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-08-02T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4655';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-02T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4655';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-03T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4655';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4519', 'Changement d''adresse de facturation', 'Merci de mettre a jour l''adresse de facturation.', 'FACTURATION', 'FERME', 'BASSE',
     'CLI-0001', 'alice', 'carol', '2026-07-21T08:40:00+00:00', '2026-07-28T08:40:00+00:00',
     'carol', '2026-07-21T10:40:00+00:00', '2026-07-22T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Nouvelle adresse : 12 rue des Ateliers, Lille.', 'PUBLIC', '2026-07-21T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4519';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'C''est fait, la prochaine facture portera la nouvelle adresse.', 'PUBLIC', '2026-07-22T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4519';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-07-21T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4519';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-07-21T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4519';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4488', 'Double prelevement sur juin', 'Deux prelevements identiques ont ete constates en juin.', 'FACTURATION', 'FERME', 'NORMALE',
     'CLI-0001', 'alice', 'bob', '2026-07-03T08:40:00+00:00', '2026-07-09T08:40:00+00:00',
     'alice', '2026-07-03T10:40:00+00:00', '2026-07-04T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Deux prelevements sur juin.', 'PUBLIC', '2026-07-03T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4488';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Confirme, le remboursement part sous 5 jours.', 'PUBLIC', '2026-07-04T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4488';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Incident de rejeu cote banque, voir OPS-441.', 'INTERNE', '2026-07-04T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4488';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Bien recu, merci.', 'PUBLIC', '2026-07-09T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4488';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-07-03T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4488';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-07-03T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4488';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-07-04T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4488';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4377', 'Certificat expire sur le portail de suivi', 'Le navigateur signale un certificat expire.', 'ANOMALIE', 'RESOLU', 'HAUTE',
     'CLI-0001', 'alice', 'bob', '2026-06-05T08:40:00+00:00', '2026-06-06T08:40:00+00:00',
     'bob', '2026-06-05T10:40:00+00:00', '2026-06-06T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Avertissement de certificat au chargement.', 'PUBLIC', '2026-06-05T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4377';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Certificat renouvele, merci du signalement.', 'PUBLIC', '2026-06-06T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4377';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-06-05T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4377';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-06-05T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4377';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4302', 'Relance sur le devis de renouvellement', 'Relance concernant le devis de renouvellement annuel.', 'AUTRE', 'FERME', 'BASSE',
     'CLI-0001', 'alice', 'carol', '2026-06-18T08:40:00+00:00', '2026-06-30T08:40:00+00:00',
     'carol', '2026-06-18T10:40:00+00:00', '2026-06-19T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'alice', 'CLIENT', 'Ou en est le devis ?', 'PUBLIC', '2026-06-18T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4302';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Il part aujourd''hui.', 'PUBLIC', '2026-06-19T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4302';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'alice', 'ticket cree par le client', '2026-06-18T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4302';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-06-18T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4302';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4818', 'Relance : avoir non recu apres annulation', 'L''avoir promis apres l''annulation de la commande n''est pas arrive.', 'FACTURATION', 'EN_COURS', 'NORMALE',
     'CLI-0002', 'david', 'bob', '2026-08-23T08:40:00+00:00', '2026-08-29T08:40:00+00:00',
     'bob', '2026-08-23T10:40:00+00:00', '2026-08-24T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'david', 'AGENT', 'L''avoir n''est toujours pas arrive.', 'PUBLIC', '2026-08-23T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4818';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Je relance la comptabilite.', 'PUBLIC', '2026-08-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4818';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Avoir bloque en validation, montant au-dessus du seuil. Escalade a Carol.', 'INTERNE', '2026-08-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4818';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'L''avoir est valide, vous le recevrez demain.', 'PUBLIC', '2026-08-29T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4818';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'david', 'ticket cree par le client', '2026-08-23T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4818';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-23T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4818';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4818';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4771', 'Modification du RIB de prelevement', 'Nous changeons de banque, merci de mettre a jour le RIB.', 'FACTURATION', 'EN_COURS', 'NORMALE',
     'CLI-0002', 'david', 'carol', '2026-08-17T08:40:00+00:00', '2026-08-28T08:40:00+00:00',
     'carol', '2026-08-17T10:40:00+00:00', '2026-08-28T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'david', 'AGENT', 'Nouveau RIB transmis par courrier.', 'PUBLIC', '2026-08-17T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4771';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Ne jamais accepter un RIB par simple courriel : verification telephonique obligatoire.', 'INTERNE', '2026-08-18T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4771';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'La verification est faite, le RIB est enregistre.', 'PUBLIC', '2026-08-28T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4771';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'david', 'ticket cree par le client', '2026-08-17T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4771';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-08-17T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4771';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'carol', 'note interne ajoutee', '2026-08-18T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4771';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4690', 'Erreur 500 a la validation du panier', 'Une erreur 500 apparait a la validation du panier depuis mardi.', 'ANOMALIE', 'RESOLU', 'HAUTE',
     'CLI-0002', 'david', 'bob', '2026-08-09T08:40:00+00:00', '2026-08-12T08:40:00+00:00',
     'bob', '2026-08-09T10:40:00+00:00', '2026-08-10T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'david', 'AGENT', 'Erreur 500 a la validation.', 'PUBLIC', '2026-08-09T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4690';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Reproduit, correctif en cours.', 'PUBLIC', '2026-08-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4690';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Regression introduite par le lot 42. Rollback demande.', 'INTERNE', '2026-08-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4690';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'C''est corrige.', 'PUBLIC', '2026-08-12T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4690';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'david', 'ticket cree par le client', '2026-08-09T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4690';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-09T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4690';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4690';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4601', 'Demande de duplicata de facture 2025', 'Merci de nous transmettre un duplicata des factures 2025.', 'FACTURATION', 'FERME', 'BASSE',
     'CLI-0002', 'david', 'carol', '2026-07-15T08:40:00+00:00', '2026-07-20T08:40:00+00:00',
     'carol', '2026-07-15T10:40:00+00:00', '2026-07-20T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'david', 'AGENT', 'Duplicata des factures 2025 svp.', 'PUBLIC', '2026-07-15T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4601';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Les duplicatas vous ont ete envoyes.', 'PUBLIC', '2026-07-20T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4601';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'david', 'ticket cree par le client', '2026-07-15T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4601';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-07-15T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4601';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4545', 'Ajout d''un contact technique au compte', 'Merci d''ajouter notre nouveau responsable technique.', 'ACCES', 'RESOLU', 'BASSE',
     'CLI-0002', 'david', 'bob', '2026-06-30T08:40:00+00:00', '2026-07-05T08:40:00+00:00',
     'bob', '2026-06-30T10:40:00+00:00', '2026-07-05T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'david', 'AGENT', 'Merci d''ajouter Karim au compte.', 'PUBLIC', '2026-06-30T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4545';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'C''est fait.', 'PUBLIC', '2026-07-05T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4545';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'david', 'ticket cree par le client', '2026-06-30T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4545';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-06-30T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4545';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4402', 'Prelevement en double sur le contrat annuel', 'Deux prelevements pour le contrat annuel ont ete debites le meme jour.', 'FACTURATION', 'OUVERT', 'HAUTE',
     'CLI-0004', 'contact.lauziere', NULL, '2026-08-26T08:40:00+00:00', '2026-08-26T08:40:00+00:00',
     'contact.lauziere', '2026-08-26T10:40:00+00:00', NULL);

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.lauziere', 'CLIENT', 'Deux prelevements le meme jour, merci de regulariser.', 'PUBLIC', '2026-08-26T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4402';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.lauziere', 'ticket cree par le client', '2026-08-26T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4402';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4805', 'Demande de duplicata de facture 2025', 'Nous avons besoin d''un duplicata des factures de l''exercice 2025.', 'FACTURATION', 'OUVERT', 'NORMALE',
     'CLI-0003', 'contact.vernet', 'carol', '2026-08-21T08:40:00+00:00', '2026-08-28T08:40:00+00:00',
     'carol', '2026-08-21T10:40:00+00:00', '2026-08-28T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.vernet', 'CLIENT', 'Duplicata 2025 svp.', 'PUBLIC', '2026-08-21T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4805';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Je prepare l''envoi.', 'PUBLIC', '2026-08-28T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4805';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.vernet', 'ticket cree par le client', '2026-08-21T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4805';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-08-21T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4805';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4744', 'Le rapport mensuel arrive vide', 'Le rapport mensuel recu par courriel ne contient aucune donnee.', 'ANOMALIE', 'EN_COURS', 'NORMALE',
     'CLI-0005', 'contact.merieux', 'bob', '2026-08-14T08:40:00+00:00', '2026-08-27T08:40:00+00:00',
     'bob', '2026-08-14T10:40:00+00:00', '2026-08-15T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.merieux', 'CLIENT', 'Le rapport de juillet est vide.', 'PUBLIC', '2026-08-14T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4744';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Je regarde la generation.', 'PUBLIC', '2026-08-15T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4744';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'La tache de generation echoue en silence quand le compte n''a aucune commande sur la periode.', 'INTERNE', '2026-08-15T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4744';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Correctif en cours de deploiement.', 'PUBLIC', '2026-08-27T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4744';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.merieux', 'ticket cree par le client', '2026-08-14T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4744';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-14T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4744';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-15T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4744';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4701', 'Ajouter la TVA intracommunautaire au contrat', 'Merci d''ajouter notre numero de TVA intracommunautaire.', 'EVOLUTION', 'EN_COURS', 'BASSE',
     'CLI-0006', 'contact.bellart', 'carol', '2026-08-10T08:40:00+00:00', '2026-08-25T08:40:00+00:00',
     'carol', '2026-08-10T10:40:00+00:00', '2026-08-25T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.bellart', 'CLIENT', 'Numero de TVA a ajouter.', 'PUBLIC', '2026-08-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4701';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Transmis au service comptable.', 'PUBLIC', '2026-08-25T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4701';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.bellart', 'ticket cree par le client', '2026-08-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4701';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-08-10T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4701';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4668', 'Connexion impossible depuis la mise a jour', 'Depuis la mise a jour de mardi, plus personne ne peut se connecter.', 'ACCES', 'RESOLU', 'HAUTE',
     'CLI-0007', 'contact.kessler', 'bob', '2026-08-06T08:40:00+00:00', '2026-08-08T08:40:00+00:00',
     'bob', '2026-08-06T10:40:00+00:00', '2026-08-07T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.kessler', 'CLIENT', 'Plus aucune connexion possible.', 'PUBLIC', '2026-08-06T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4668';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Incident identifie, correctif en cours.', 'PUBLIC', '2026-08-07T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4668';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Cache de sessions non purge apres la montee de version.', 'INTERNE', '2026-08-07T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4668';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'C''est retabli.', 'PUBLIC', '2026-08-08T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4668';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.kessler', 'ticket cree par le client', '2026-08-06T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4668';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-08-06T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4668';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-08-07T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4668';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4620', 'Resiliation du contrat au 31/12', 'Nous souhaitons resilier le contrat a echeance.', 'AUTRE', 'FERME', 'NORMALE',
     'CLI-0003', 'contact.vernet', 'carol', '2026-07-30T08:40:00+00:00', '2026-08-06T08:40:00+00:00',
     'carol', '2026-07-30T10:40:00+00:00', '2026-08-06T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.vernet', 'CLIENT', 'Resiliation au 31/12.', 'PUBLIC', '2026-07-30T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4620';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Resiliation enregistree.', 'PUBLIC', '2026-08-06T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4620';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.vernet', 'ticket cree par le client', '2026-07-30T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4620';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-07-30T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4620';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4590', 'Facture non conforme au bon de commande', 'Le montant de la facture ne correspond pas au bon de commande.', 'FACTURATION', 'OUVERT', 'NORMALE',
     'CLI-0004', 'contact.lauziere', 'bob', '2026-07-27T08:40:00+00:00', '2026-08-28T08:40:00+00:00',
     'bob', '2026-07-27T10:40:00+00:00', '2026-08-28T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.lauziere', 'CLIENT', 'Ecart entre la facture et le bon de commande.', 'PUBLIC', '2026-07-27T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4590';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Je verifie avec la comptabilite.', 'PUBLIC', '2026-08-28T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4590';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.lauziere', 'ticket cree par le client', '2026-07-27T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4590';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-07-27T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4590';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4551', 'Le webhook de notification ne part plus', 'Aucun webhook recu depuis le 22 juillet.', 'ANOMALIE', 'EN_COURS', 'HAUTE',
     'CLI-0003', 'contact.vernet', NULL, '2026-07-24T08:40:00+00:00', '2026-08-22T08:40:00+00:00',
     'contact.vernet', '2026-07-24T10:40:00+00:00', NULL);

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.vernet', 'CLIENT', 'Plus aucun webhook recu.', 'PUBLIC', '2026-07-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4551';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.vernet', 'ticket cree par le client', '2026-07-24T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4551';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4503', 'Creer un acces en lecture seule pour la compta', 'Nous souhaitons un acces en lecture seule pour le service comptable.', 'ACCES', 'RESOLU', 'BASSE',
     'CLI-0005', 'contact.merieux', 'carol', '2026-07-19T08:40:00+00:00', '2026-07-23T08:40:00+00:00',
     'carol', '2026-07-19T10:40:00+00:00', '2026-07-23T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.merieux', 'CLIENT', 'Acces lecture seule pour la compta.', 'PUBLIC', '2026-07-19T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4503';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Le compte est cree.', 'PUBLIC', '2026-07-23T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4503';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.merieux', 'ticket cree par le client', '2026-07-19T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4503';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-07-19T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4503';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4460', 'Mise a jour des coordonnees du contact principal', 'Le contact principal a change.', 'AUTRE', 'FERME', 'BASSE',
     'CLI-0006', 'contact.bellart', 'carol', '2026-07-10T08:40:00+00:00', '2026-07-14T08:40:00+00:00',
     'carol', '2026-07-10T10:40:00+00:00', '2026-07-14T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.bellart', 'CLIENT', 'Nouveau contact principal : Henri Bellart.', 'PUBLIC', '2026-07-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4460';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Coordonnees mises a jour.', 'PUBLIC', '2026-07-14T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4460';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.bellart', 'ticket cree par le client', '2026-07-10T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4460';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-07-10T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4460';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4433', 'Lenteur a l''ouverture des rapports', 'Les rapports mettent plus de trente secondes a s''ouvrir.', 'ANOMALIE', 'RESOLU', 'NORMALE',
     'CLI-0007', 'contact.kessler', 'bob', '2026-06-26T08:40:00+00:00', '2026-07-02T08:40:00+00:00',
     'bob', '2026-06-26T10:40:00+00:00', '2026-07-02T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.kessler', 'CLIENT', 'Les rapports sont tres lents.', 'PUBLIC', '2026-06-26T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4433';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Index manquant sur la table de faits. DEV-905.', 'INTERNE', '2026-06-27T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4433';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'bob', 'AGENT', 'Un index a ete ajoute, l''ouverture est immediate.', 'PUBLIC', '2026-07-02T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4433';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.kessler', 'ticket cree par le client', '2026-06-26T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4433';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'bob', 'assigne a bob', '2026-06-26T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4433';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'NOTE_INTERNE', 'bob', 'note interne ajoutee', '2026-06-27T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4433';

INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
    crm_client_ref, auteur_username, assigne_a, cree_le, derniere_activite_le,
    derniere_activite_par, echeance_sla_le, premiere_reponse_le) VALUES
    ('TCK-4391', 'Question sur le calcul du prorata', 'Comment est calcule le prorata en cas de changement de formule ?', 'AUTRE', 'FERME', 'BASSE',
     'CLI-0005', 'contact.merieux', 'carol', '2026-06-12T08:40:00+00:00', '2026-06-16T08:40:00+00:00',
     'carol', '2026-06-12T10:40:00+00:00', '2026-06-16T08:40:00+00:00');

INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'contact.merieux', 'CLIENT', 'Comment fonctionne le prorata ?', 'PUBLIC', '2026-06-12T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4391';
INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
    SELECT id, 'carol', 'AGENT', 'Le prorata est calcule au jour pres, voici le detail.', 'PUBLIC', '2026-06-16T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4391';

INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'CREATION', 'contact.merieux', 'ticket cree par le client', '2026-06-12T08:40:00+00:00' FROM ticket WHERE reference = 'TCK-4391';
INSERT INTO evenement_ticket (ticket_id, type, auteur_username, detail, cree_le)
    SELECT id, 'ASSIGNATION', 'carol', 'assigne a carol', '2026-06-12T09:00:00+00:00' FROM ticket WHERE reference = 'TCK-4391';


#!/usr/bin/env bash
#
# Appelle le CRM legacy en SOAP, sans avoir à taper l'enveloppe XML.
#
#   ./verif/crm.sh get CLI-0001            une fiche client
#   ./verif/crm.sh search atelier          une recherche par raison sociale
#   ./verif/crm.sh wsdl                    le contrat
#   ./verif/crm.sh brut '<XML…>'           une enveloppe que tu écris toi-même
#
# Chaque appel affiche la réponse, puis le code HTTP et le temps écoulé.
# Un script plutôt qu'une fonction shell : ça survit à l'ouverture d'un onglet.

set -uo pipefail

CRM="${CRM:-http://localhost:8082}"
NS="http://legacy.acme.fr/crm"

enveloppe() {
	printf '<?xml version="1.0"?><soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>%s</soap:Body></soap:Envelope>' "$1"
}

envoyer() {
	curl -s -X POST "$CRM/services" \
		-H 'Content-Type: text/xml;charset=UTF-8' \
		-w '\n__ HTTP %{http_code} · %{time_total}s\n' \
		-d "$1"
}

# Découpe le XML d'une balise par ligne : illisible autrement.
lisible() { sed 's/></>\n</g'; }

usage() {
	sed -n '3,12p' "$0" | sed 's/^# \{0,1\}//'
	exit 1
}

case "${1:-}" in
	get)
		[ $# -ge 2 ] || { echo "Il manque la référence client. Exemple : $0 get CLI-0001" >&2; exit 1; }
		envoyer "$(enveloppe "<GetClientRequest xmlns=\"$NS\"><clientRef>$2</clientRef></GetClientRequest>")" | lisible
		;;
	search)
		# Volontairement sans garde-fou sur l'argument vide : le CRM doit répondre
		# CRITERE_OBLIGATOIRE, et c'est ce fault qu'on vient observer.
		envoyer "$(enveloppe "<SearchClientsRequest xmlns=\"$NS\"><namePattern>${2:-}</namePattern></SearchClientsRequest>")" | lisible
		;;
	wsdl)
		curl -s -w '\n__ HTTP %{http_code} · %{size_download} octets\n' "$CRM/services/clients.wsdl"
		;;
	brut)
		[ $# -ge 2 ] || { echo "Il manque le corps XML. Exemple : $0 brut '<GetClientRequest …>'" >&2; exit 1; }
		envoyer "$(enveloppe "$2")" | lisible
		;;
	""|-h|--help|aide)
		usage
		;;
	*)
		echo "Opération inconnue : $1" >&2
		usage
		;;
esac

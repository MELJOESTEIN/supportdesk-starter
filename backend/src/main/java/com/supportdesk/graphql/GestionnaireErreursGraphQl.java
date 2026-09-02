package com.supportdesk.graphql;

import java.util.Map;

import com.supportdesk.client.ClientCrmInconnuException;
import com.supportdesk.client.CritereRechercheManquantException;
import com.supportdesk.client.CrmIndisponibleException;
import com.supportdesk.client.FicheClientRefuseeException;
import com.supportdesk.ticket.AccesRefuseException;
import com.supportdesk.ticket.CompteNonRattacheException;
import com.supportdesk.ticket.CreationReserveeAuClientException;
import com.supportdesk.ticket.TicketIntrouvableException;
import com.supportdesk.ticket.TransitionInterditeException;
import com.supportdesk.ticket.VisibiliteInterditeException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Traduit les exceptions métier en erreurs GraphQL lisibles.
 *
 * <p>Le pendant de {@code GestionnaireErreurs} pour {@code /graphql}. Sans lui, toute exception
 * remonte en {@code INTERNAL_ERROR} accompagnée d'un identifiant de corrélation — ce qui masque
 * la cause au client et n'apprend rien à personne.
 *
 * <p><b>Deux différences avec REST, et elles sont le sujet du J3.</b> D'abord, la réponse HTTP
 * reste <b>200</b> : en GraphQL, la requête a réussi, c'est un <i>champ</i> qui a échoué. Ensuite,
 * l'erreur porte un {@code path} qui désigne exactement le champ fautif, ce qui permet à un écran
 * de se rendre partiellement au lieu de tout perdre.
 *
 * <p>Les codes REST n'existent pas ici : {@link ErrorType} en tient lieu. La correspondance est
 * volontairement la même que celle du gestionnaire REST, pour qu'une règle métier ne change pas
 * de sens selon la porte d'entrée.
 */
@Component
public class GestionnaireErreursGraphQl extends DataFetcherExceptionResolverAdapter {

	@Override
	protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
		if (ex instanceof TicketIntrouvableException || ex instanceof ClientCrmInconnuException) {
			return erreur(ex, env, ErrorType.NOT_FOUND, "RESSOURCE_INTROUVABLE");
		}
		if (ex instanceof AccesRefuseException || ex instanceof FicheClientRefuseeException
				|| ex instanceof VisibiliteInterditeException
				|| ex instanceof CompteNonRattacheException
				|| ex instanceof CreationReserveeAuClientException
				|| ex instanceof AccessDeniedException) {
			return erreur(ex, env, ErrorType.FORBIDDEN, "ACCES_REFUSE");
		}
		if (ex instanceof TransitionInterditeException) {
			return erreur(ex, env, ErrorType.BAD_REQUEST, "TRANSITION_INTERDITE");
		}
		if (ex instanceof CritereRechercheManquantException
				|| ex instanceof IllegalArgumentException) {
			return erreur(ex, env, ErrorType.BAD_REQUEST, "REQUETE_INVALIDE");
		}
		// Le référentiel legacy est injoignable. Ce n'est pas la faute de l'appelant, et le dire
		// permet à l'écran d'afficher la référence brute plutôt qu'une erreur générique.
		if (ex instanceof CrmIndisponibleException) {
			return erreur(ex, env, ErrorType.INTERNAL_ERROR, "REFERENTIEL_INDISPONIBLE");
		}
		// Tout le reste reste un INTERNAL_ERROR anonyme : une exception qu'on n'a pas prévue ne
		// doit pas divulguer son message, qui peut contenir des détails d'implémentation.
		return null;
	}

	private static GraphQLError erreur(Throwable ex, DataFetchingEnvironment env, ErrorType type,
			String code) {
		return GraphqlErrorBuilder.newError(env)
				.errorType(type)
				.message(ex.getMessage())
				.extensions(Map.of("code", code))
				.build();
	}

}

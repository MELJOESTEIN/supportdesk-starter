package com.supportdesk.ticket;

/**
 * Un commentaire INTERNE n'est jamais lisible par un client.
 *
 * <p>Cette règle se tient dans la requête qui charge les commentaires, pas dans le mapping
 * ni dans le front : une note interne ne doit pas quitter la base pour un appel client.
 */
public enum VisibiliteCommentaire {
	PUBLIC,
	INTERNE
}

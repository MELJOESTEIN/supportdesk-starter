package com.supportdesk.core;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Page renvoyée par l'API.
 *
 * <p>Ce n'est volontairement pas {@code PageImpl} : sa forme JSON n'est pas un contrat
 * stable, et Spring lui-même déconseille de l'exposer. Le contrat est ici, et il
 * correspond mot pour mot à {@code PageReponse<T>} du frontend.
 */
public record PageReponse<T>(List<T> contenu, int page, int taille, long total, int totalPages) {

	public static <T> PageReponse<T> de(Page<T> page) {
		return new PageReponse<>(page.getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	public <R> PageReponse<R> transformer(java.util.function.Function<T, R> mapping) {
		return new PageReponse<>(this.contenu.stream().map(mapping).toList(), this.page, this.taille,
				this.total, this.totalPages);
	}
}

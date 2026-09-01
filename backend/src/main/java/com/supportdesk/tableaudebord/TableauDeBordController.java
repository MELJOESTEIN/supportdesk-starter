package com.supportdesk.tableaudebord;

import com.supportdesk.tableaudebord.TableauDeBordDtos.TableauDeBord;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableau de bord agrégé.
 *
 * <p>Sécurité : cet endpoint expose l'activité de <b>tous</b> les comptes. Il sera réservé
 * aux rôles AGENT et ADMIN au lot 4. Sans authentification, il est aujourd'hui ouvert —
 * c'est la seconde face de la même dette, celle de l'autorisation au niveau fonction.
 */
@RestController
@RequestMapping("/api/tableau-de-bord")
public class TableauDeBordController {

	private final TableauDeBordService service;

	public TableauDeBordController(TableauDeBordService service) {
		this.service = service;
	}

	@GetMapping
	public TableauDeBord obtenir(@RequestParam(defaultValue = "14") int jours) {
		return this.service.construire(Math.clamp(jours, 1, 90));
	}
}

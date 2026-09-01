package com.supportdesk.client;

/** Le CRM ne répond pas, ou pas à temps. Se traduit en 503, jamais en trace. */
public class CrmIndisponibleException extends RuntimeException {

	public CrmIndisponibleException(String message, Throwable cause) {
		super(message, cause);
	}
}

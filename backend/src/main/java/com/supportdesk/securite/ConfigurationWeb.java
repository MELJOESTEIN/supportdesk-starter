package com.supportdesk.securite;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Branche le résolveur qui construit {@link UtilisateurCourant} depuis le jeton. */
@Configuration(proxyBeanMethods = false)
public class ConfigurationWeb implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new UtilisateurCourantArgumentResolver());
	}
}

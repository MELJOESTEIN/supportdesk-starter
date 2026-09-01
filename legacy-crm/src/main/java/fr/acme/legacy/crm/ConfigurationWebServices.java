package fr.acme.legacy.crm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * En Boot 4, {@code @EnableWs} et l'enregistrement manuel du MessageDispatcherServlet ne sont
 * plus nécessaires : l'autoconfiguration s'en charge, sur {@code spring.webservices.path}.
 *
 * <p>Le nom du bean donne l'URL du contrat : {@code clients} -> /services/clients.wsdl
 */
@Configuration(proxyBeanMethods = false)
public class ConfigurationWebServices {

	@Bean
	public XsdSchema clientsSchema() {
		return new SimpleXsdSchema(new ClassPathResource("schemas/clients.xsd"));
	}

	@Bean(name = "clients")
	public DefaultWsdl11Definition clientsWsdl(XsdSchema clientsSchema) {
		DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
		definition.setPortTypeName("ClientsPort");
		definition.setLocationUri("/services");
		definition.setTargetNamespace(CrmClientsEndpoint.NAMESPACE);
		definition.setSchema(clientsSchema);
		return definition;
	}
}

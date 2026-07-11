package br.gov.caixa.simtr.hub.arquitetura.seguranca;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class RequestHeaderFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "simtr.apikey", defaultValue="apikey")
    String apikey;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("apikey", apikey);
        return headers;
    }

}
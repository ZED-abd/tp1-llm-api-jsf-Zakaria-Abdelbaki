package com.zakariaabdelbaki.tp1.llm;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class LlmClientPourGemini {

    private static final String MODELE = "gemini-3.5-flash-lite";
    private final String cle;
    private final Client clientRest;
    private final WebTarget target;

    public LlmClientPourGemini() {
        cle = System.getenv("GEMINI_KEY");
        clientRest = ClientBuilder.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
        target = clientRest.target("https://generativelanguage.googleapis.com/v1beta/models/"
                + MODELE + ":generateContent");
    }

    public Response envoyerRequete(Entity<String> requete) throws RequeteException {
        if (cle == null || cle.isBlank()) {
            throw new RequeteException("La variable d’environnement GEMINI_KEY est absente.");
        }
        // La clé passe dans un en-tête HTTP et ne fait pas partie du JSON.
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header("x-goog-api-key", cle)
                .post(requete);
    }

    String masquerCle(String texte) {
        return texte.replace(cle, "[clé masquée]");
    }

    @PreDestroy
    public void closeClient() {
        clientRest.close();
    }
}

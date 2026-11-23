package com.zakariaabdelbaki.tp1.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

@Dependent
public class JsonAdapterPourGemini implements Serializable {

    @Inject
    private LlmClientPourGemini geminiClient;

    private String systemRole = "helpful assistant";
    private String historiqueJson;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public LlmInteraction envoyerRequete(String question) throws RequeteException {
        JsonObject requete = historiqueJson == null
                ? creerRequeteJson(systemRole, question)
                : ajouterMessage(lireObjet(historiqueJson), creerMessage("user", question));
        String questionJson = formater(requete);

        try (Response response = geminiClient.envoyerRequete(Entity.entity(questionJson, MediaType.APPLICATION_JSON_TYPE))) {
            String reponseJson = geminiClient.masquerCle(response.readEntity(String.class));
            if (response.getStatus() != 200) {
                throw new RequeteException(response.getStatus(), response.getStatus() + " : "
                        + response.getStatusInfo().getReasonPhrase() + " Réponse API : " + reponseJson, questionJson);
            }

            JsonArray candidats = lireObjet(reponseJson).getJsonArray("candidates");
            JsonObject message = candidats == null || candidats.isEmpty()
                    ? null : candidats.getJsonObject(0).getJsonObject("content");
            String texte = extraireTexte(message);
            if (texte.isBlank()) {
                throw new RequeteException("Gemini n’a retourné aucune réponse textuelle.", questionJson);
            }

            // L'historique ne change qu'après une réponse exploitable.
            historiqueJson = ajouterMessage(requete, message).toString();
            return new LlmInteraction(questionJson, reponseJson, texte);
        } catch (ProcessingException e) {
            throw new RequeteException("Impossible de joindre l’API Gemini.", questionJson);
        } catch (JsonException e) {
            throw new RequeteException("La réponse de Gemini ne contient pas de JSON valide.", questionJson);
        }
    }

    private JsonObject creerRequeteJson(String systemRole, String question) {
        return Json.createObjectBuilder()
                .add("system_instruction", Json.createObjectBuilder().add("parts", creerParties(systemRole)))
                .add("contents", Json.createArrayBuilder().add(creerMessage("user", question)))
                .build();
    }

    private JsonObject creerMessage(String role, String texte) {
        return Json.createObjectBuilder()
                .add("role", role)
                .add("parts", creerParties(texte))
                .build();
    }

    private JsonArray creerParties(String texte) {
        return Json.createArrayBuilder().add(Json.createObjectBuilder().add("text", texte)).build();
    }

    private JsonObject ajouterMessage(JsonObject requete, JsonObject message) {
        return Json.createPointer("/contents/-").add(requete, message);
    }

    private String extraireTexte(JsonObject message) {
        if (message == null || !message.containsKey("parts")) {
            return "";
        }
        StringBuilder texte = new StringBuilder();
        for (JsonValue part : message.getJsonArray("parts")) {
            JsonObject partie = part.asJsonObject();
            if (partie.containsKey("text") && !partie.getBoolean("thought", false)) {
                texte.append(partie.getString("text"));
            }
        }
        return texte.toString();
    }

    private JsonObject lireObjet(String texte) {
        try (JsonReader reader = Json.createReader(new StringReader(texte))) {
            return reader.readObject();
        }
    }

    private String formater(JsonObject objet) {
        StringWriter texte = new StringWriter();
        try (JsonWriter writer = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(texte)) {
            writer.writeObject(objet);
        }
        return texte.toString();
    }
}

# TP 1 - Accès direct à l'API d'un LLM, avec Jakarta EE et en particulier JSF

Ce TP reprend l'interface utilisateur Web du TP 0. Cette fois-ci le serveur répond à la question tapée par l'utilisateur en l'envoyant à un LLM et en retournant la réponse du LLM. L'interface utilisateur Web affiche cette réponse.

Ce TP vous montre comment accéder **directement** aux endpoints REST de l'API d'un LLM, avec le format JSON pour le corps des requêtes et des réponses HTTP. Le format JSON dépend des LLMs ; par exemple, si vous utilisez OpenAI à la place de Gemini, le format JSON est différent.

Les TPs suivants utiliseront le framework LangChain4j pour accéder à l'API d'un LLM. Il sera ainsi plus facile de changer de LLM en changeant seulement une ou deux lignes du code.

---

## Ressources sur l'API du LLM

Pour les TPs le LLM Gemini a été choisi car son accès est gratuit pour un usage non intensif : [https://ai.google.dev/gemini-api/docs/pricing](https://ai.google.dev/gemini-api/docs/pricing).

Pour faire les TPs vous risquez d'épuiser quelquefois votre quota ; en ce cas il vous faudra attendre un peu pour que le quota soit renouvelé. Je vous conseille d'utiliser le modèle le moins coûteux. [Cette page](https://ai.google.dev/gemini-api/docs/models) liste les modèles disponibles ; les modèles les moins coûteux sont les plus anciens et s'il y a un modèle "lite", choisissez-le. Par exemple, à la date de l'écriture de ces lignes, le modèle le moins coûteux est le modèle Gemini 2.5 Flash-Lite (nom `gemini-2.5-flash-lite` ou `gemini-3.5-flash-lite`).

### Liens pour Gemini :
- Référence API : [https://ai.google.dev/api](https://ai.google.dev/api)
- Documentation sur l'API : [https://ai.google.dev/gemini-api/docs](https://ai.google.dev/gemini-api/docs)
- Guide pour utiliser l'API de génération : [https://ai.google.dev/gemini-api/docs/text-generation](https://ai.google.dev/gemini-api/docs/text-generation)
- Formats des requêtes et réponses REST : [https://github.com/google/generative-ai-docs/blob/main/site/en/gemini-api/docs/get-started/rest.ipynb](https://github.com/google/generative-ai-docs/blob/main/site/en/gemini-api/docs/get-started/rest.ipynb)
- Limites de débit : [https://ai.google.dev/gemini-api/docs/rate-limits?hl=fr](https://ai.google.dev/gemini-api/docs/rate-limits?hl=fr)

---

## Code fourni pour le TP

### 1. Code de la classe `JsonAdapterPourGemini` (pris sur la page du cours)

```java
package com.zakariaabdelbaki.tp1.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe pour gérer le JSON des requêtes à l'API de Gemini.
 * Contient l'état JSON de la conversation et des méthodes pour manipuler le JSON.
 */
@Dependent // Il faut une portée CDI pour pouvoir injecter une instance de cette classe
public class JsonAdapterPourGemini implements Serializable {

    private String systemRole; // = "helpful assistant";
    /**
     * Pour ajouter une nouvelle valeur à la fin du tableau JSON "messages" dans le document JSON de la requête.
     * Le "-" final indique que la valeur sera ajoutée à la fin du tableau.
     */
    private final JsonPointer pointer = Json.createPointer(("/contents/-"));

    /**
     * Requête JSON, à partir du prompt de l'utilisateur.
     */
    private JsonObject requeteJson;
    private String texteRequeteJson;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    /**
     * Pour envoyer une requête à l'API de Gemini.
     */
    @Inject
    private LlmClientPourGemini geminiClient;

    /**
     * Envoi une requête à l'API de Gemini.
     * Format du document JSON envoyé dans la requête vers l'API :
     * {
     *     "contents": [
     *         {
     *             "role": "user",
     *             "parts": [
     *                 {
     *                     "text": "Capitale de la France ?"
     *                 }
     *             ]
     *         },
     *         {
     *             "role": "model",
     *             "parts": [
     *                 {
     *                     "text": "Paris est la capitale de la France."
     *                 }
     *             ]
     *         },
     *         ...
     *     ]
     * }
     *
     * @param question question posée par l'utilisateur
     * @return la réponse de l'API, sous la forme d'un texte simple et du JSON reçu de l'API du LLM.
     * @throws RequeteException exception lancée dans le cas où la requête a été rejetée par l'API.
     */
    public LlmInteraction envoyerRequete(String question) throws RequeteException {
        String requestBody;
        if (this.requeteJson == null) {
            // Si c'est la première question, crée la requête JSON avec le rôle système.
            requestBody = creerRequeteJson(this.systemRole, question);
        } else {
            // Ajout de la question.
            // Ce qui sera envoyé dans le corps de la requête POST.
            // Un message associé à la question doit être ajouté aux messages associés au début de la conversation.
            requestBody = ajouteQuestionDansJsonRequete(question);
        }

        Entity<String> entity = Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE);
        // Pour un affichage plus lisible de la requête JSON
        this.texteRequeteJson = prettyPrinting(requeteJson);
        // Envoi la requête par l'intermédiaire du client de l'API de Gemini.
        try (Response response = geminiClient.envoyerRequete(entity)) {
            // Entité incluse dans la réponse (texte au format JSON qui englobe la réponse à la question)
            String texteReponseJson = response.readEntity(String.class);
            if (response.getStatus() == 200) {
                return new LlmInteraction(this.texteRequeteJson, texteReponseJson, extractReponse(texteReponseJson));
            } else {
                // Pour voir la requête JSON s'il y a eu un problème.
                JsonObject objet = Json.createReader(new StringReader(requestBody)).readObject();
                throw new RequeteException(
                    response.getStatus() + " : " + response.getStatusInfo() 
                        + " Reponse API : " + texteReponseJson,    
                    prettyPrinting(objet));
            }
        }
    }

    /**
     * Crée une requête JSON pour envoyer à l'API de Gemini.
     * Il y a le rôle du système et la question de l'utilisateur.
     * Format du document JSON envoyé dans la requête vers l'API :
     * {
     *    "system_instruction": {
     *      "parts": [ {"text": "helpful assistant"} ]
     *    },
     *    "contents": [
     *        { "role": "user",
     *          "parts": [ { "text": "Capitale de la France ?" } ]
     *        }
     *    ]
     * }
     *
     * @param systemRole rôle donné au LLM.
     * @param question question posée par l'utilisateur.
     * @return le texte du document JSON de la requête.
     */
    private String creerRequeteJson(String systemRole, String question) {
        // Création de l'objet "system_instruction"
        JsonArray systemInstructionParts = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("text", systemRole))
                .build();
        JsonObject systemInstruction = Json.createObjectBuilder()
                .add("parts", systemInstructionParts)
                .build();
        // Création de l'objet "contents"
        JsonArray userContentParts = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("text", question))
                .build();
        JsonObject userContent = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", userContentParts)
                .build();
        JsonArray contents = Json.createArrayBuilder()
                .add(userContent)
                .build();
        // Création de l'objet racine
        JsonObject rootJson = Json.createObjectBuilder()
                .add("system_instruction", systemInstruction)
                .add("contents", contents)
                .build();
        this.requeteJson = rootJson;

        return rootJson.toString();
    }

    /**
     * Modifie le JSON de la requête pour ajouter le JsonObject lié à la nouvelle question.
     * Il faut l'ajouter au tableau JSON qui contient déjà les questions et réponses précédentes.
     *
     * @param nouvelleQuestion question posée par l'utilisateur.
     * @return le texte du document JSON de la requête.
     */
    private String ajouteQuestionDansJsonRequete(String nouvelleQuestion) {
        // Crée le nouveau JsonObject qui correspond à la nouvelle question
        JsonObject nouveauMessageJson = Json.createObjectBuilder()
                .add("text", nouvelleQuestion)
                .build();
        // Crée le JsonArray parts
        JsonObjectBuilder newPartBuilder = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", Json.createArrayBuilder()
                        .add(nouveauMessageJson)
                        .build());
        // Ajoute ce nouveau JsonObjet dans this.requeteJson
        this.requeteJson = this.pointer.add(this.requeteJson, newPartBuilder.build());
        // La requête sous la forme d'une String avec mise en forme (passage à la ligne et indentation).
        this.texteRequeteJson = prettyPrinting(requeteJson);
        return this.requeteJson.toString();
    }

    /**
     * Retourne le texte formaté du document JSON pour un affichage plus agréable.
     *
     * @param jsonObject l'objet JSON dont on veut une forme formatée.
     * @return la forme formatée
     */
    private String prettyPrinting(JsonObject jsonObject) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
            jsonWriter.write(jsonObject);
        }
        return stringWriter.toString();
    }

    /**
     * Extrait la réponse de l'API et ajoute la réponse à this.requeteJson pour garder la conversation dans
     * la prochaine requête.
     * @param json le document JSON de la réponse.
     * @return juste la valeur de content qui contient la réponse à la question.
     */
    private String extractReponse(String json) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            JsonObject jsonObject = jsonReader.readObject();
            JsonObject messageReponse = jsonObject
                    .getJsonArray("candidates")
                    .getJsonObject(0)
                    .getJsonObject("content");
            // Ajoute l'objet JSON de la réponse de l'API au JSON de la prochaine requête
            this.requeteJson = this.pointer.add(this.requeteJson, messageReponse);
            // Extrait le texte de la réponse. Gemini peut découper sa réponse en plusieurs "parts" :
            // il faut les concaténer toutes (sinon la réponse affichée est tronquée).
            // Seules les parties textuelles sont récupérées, en ignorant les éventuelles parties de réflexion.
            StringBuilder texte = new StringBuilder();
            for (JsonValue part : messageReponse.getJsonArray("parts")) {
                JsonObject partie = part.asJsonObject();

                // On ne récupère que les parties textuelles,
                // en ignorant les éventuelles parties de réflexion.
                if (partie.containsKey("text")
                        && !partie.getBoolean("thought", false)) {
                    texte.append(partie.getString("text"));
                }
            }
            return texte.toString();
        }
    }
}
```

---

### 2. Squelette de la classe `LlmClientPourGemini` (fourni sur le cours)

```java
package com.zakariaabdelbaki.tp1.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.PreDestroy;

import java.io.Serializable;

/**
 * Gère l'interface avec l'API de Gemini.
 * Son rôle est essentiellement de lancer une requête à chaque nouvelle
 * question qu'on veut envoyer à l'API.
 *
 * De portée dependent pour réinitialiser la conversation à chaque fois que
 * l'instance qui l'utilise est renouvelée.
 * Par exemple, si l'instance qui l'utilise est de portée View, la conversation est
 * réinitialisée à chaque fois que l'utilisateur quitte la page en cours.
 */
@Dependent
public class LlmClientPourGemini implements Serializable {
    // Client REST. Facilite les échanges avec une API REST.
    private Client clientRest; // Pour pouvoir le fermer
    // Représente un endpoint de serveur REST
    private final WebTarget target;

    public LlmClientPourGemini() {
        // Récupère la clé secrète pour travailler avec l'API du LLM, mise dans une variable d'environnement
        // du système d'exploitation.
        String key = System.getenv("GEMINI_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("LLM_KEY");
        }
        if (key == null) {
            key = "";
        }

        // Client REST pour envoyer des requêtes vers les endpoints de l'API du LLM
        this.clientRest = ClientBuilder.newClient();

        // Endpoint REST pour envoyer la question à l'API.
        // Modèle : gemini-2.5-flash-lite (ou gemini-2.0-flash)
        String model = System.getenv("GEMINI_MODEL");
        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash-lite";
        }
        this.target = clientRest.target("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .queryParam("key", key);
    }

    /**
     * Envoie une requête à l'API de Gemini.
     * @param requestEntity le corps de la requête (en JSON).
     * @return réponse REST de l'API (corps en JSON).
     */
    public Response envoyerRequete(Entity<String> requestEntity) {
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON_TYPE);
        // Envoie la requête POST au LLM
        return request.post(requestEntity);
    }

    @PreDestroy
    public void closeClient() {
        if (this.clientRest != null) {
             this.clientRest.close();
        }
    }
}
```

---

### 3. Classe `RequeteException`

```java
package com.zakariaabdelbaki.tp1.llm;

/**
 * Exception levée si on envoie une mauvaise requête à l'API du LLM.
 */
public class RequeteException extends Exception {
    private int status;
    private String requeteJson;

    public RequeteException(int status) {
        this.status = status;
    }

    public RequeteException(String message) {
        super(message);
    }

    public RequeteException(String message, String requeteJson) {
        super(message);
        this.requeteJson = requeteJson;
    }

    public RequeteException() {
    }

    public int getStatus() {
        return status;
    }

    public String getRequeteJson() {
        return requeteJson;
    }
}
```

---

### 4. Record `LlmInteraction`

```java
package com.zakariaabdelbaki.tp1.llm;

import java.io.Serializable;

/**
 * Record représentant l'interaction complète avec le LLM.
 */
public record LlmInteraction(String questionJson, String reponseJson, String reponseExtraite) implements Serializable {
}
```

---

### 5. Composants UI pour le Mode Debug dans `index.xhtml`

#### Le toggle switch PrimeFaces :
```xml
<p:outputPanel style="display:flex; align-items: center; gap: 10px">
    <h:outputText value="Mode debug : "/>
    <p:toggleSwitch id="debugSwitch" value="#{bb.debug}" styleClass="mb-2" label="Mode debug">
        <p:ajax update="paneldebugwrapper"/>
    </p:toggleSwitch>
    <p:tooltip for="debugSwitch" value="Active ou non l'affichage du JSON sur les *prochaines* requêtes et réponses du LLM." />
</p:outputPanel>
```

#### Le panneau debug avec les textareas :
```xml
<h:panelGroup id="paneldebugwrapper" layout="block">
    <h:panelGrid id="paneldebug" columns="1" rendered="#{bb.debug}">
        <h:panelGroup>
            <h:outputText value="JSON envoyé dans la requête :"/>
            <button type="button" onclick="copyToClipboard('jsonrequete')">Copier requête JSON</button>
        </h:panelGroup>
        <h:inputTextarea id="jsonrequete" value="#{bb.texteRequeteJson}" title="Requête JSON"
                         cols="50" rows="15" readonly="true"/>
        <h:panelGroup>
            <h:outputText value="JSON retourné dans la réponse :"/>
            <button type="button" onclick="copyToClipboard('jsonreponse')">Copier réponse JSON</button>
        </h:panelGroup>
        <h:inputTextarea id="jsonreponse" value="#{bb.texteReponseJson}" title="Réponse JSON"
                         cols="50" rows="15" readonly="true"/>
    </h:panelGrid>
</h:panelGroup>
```

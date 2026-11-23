package com.zakariaabdelbaki.tp1.llm;

import java.io.Serializable;

// Contient le JSON échangé et le texte à afficher.
public record LlmInteraction(String questionJson, String reponseJson, String reponseExtraite) implements Serializable {
}

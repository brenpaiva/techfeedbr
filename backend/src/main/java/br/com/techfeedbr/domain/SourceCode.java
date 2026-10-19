package br.com.techfeedbr.domain;

public enum SourceCode {
    GNEWS("GNews"),
    EBC("Agência Brasil"),
    DEV("DEV Community"),
    TABNEWS("TabNews"),
    TECNOBLOG("Tecnoblog"),
    OLHARDIGITAL("Olhar Digital"),
    TUDOCELULAR("TudoCelular"),
    HARDWARE("Hardware.com.br"),
    MUNDOCONECTADO("Mundo Conectado"),
    MACMAGAZINE("MacMagazine"),
    ADRENALINE("Adrenaline");

    private final String displayName;

    SourceCode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

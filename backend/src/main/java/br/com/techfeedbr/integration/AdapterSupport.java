package br.com.techfeedbr.integration;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;

final class AdapterSupport {
    private static final Pattern HTML = Pattern.compile("<[^>]+>");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private AdapterSupport() {}

    static String cleanText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String clean = SPACES.matcher(StringEscapeUtils.unescapeHtml4(
                HTML.matcher(value).replaceAll(" "))).replaceAll(" ").trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength - 1).trim() + "…";
    }

    static Set<String> inferCategories(String text) {
        String value = " " + (text == null ? "" : text.toLowerCase(Locale.ROOT)) + " ";
        if (containsAny(value, "inteligência artificial", "inteligencia artificial", "machine learning", " ia ", " ai ")) {
            return Set.of("Inteligência Artificial");
        }
        if (containsAny(value, "segurança", "security", "vulnerabilidade", "cyber")) {
            return Set.of("Segurança");
        }
        if (containsAny(value, "antivírus", "antivirus", "privacidade", "vazamento de dados")) {
            return Set.of("Segurança");
        }
        if (containsAny(value, "dados", "data", "banco de dados")) {
            return Set.of("Dados");
        }
        if (containsAny(value, "android", " ios ", "mobile", "celular", "iphone", "smartphone", "galaxy")) {
            return Set.of("Mobile");
        }
        if (containsAny(value, "notebook", "computador", "processador", " gpu ", " ssd ",
                "memória ram", "placa de vídeo", "monitor", "hardware")) {
            return Set.of("Hardware");
        }
        if (containsAny(value, "whatsapp", "instagram", "tiktok", "rede social", "navegador")) {
            return Set.of("Internet e Apps");
        }
        if (containsAny(value, "cloud", "nuvem", "devops", "kubernetes", "docker")) {
            return Set.of("Cloud e DevOps");
        }
        if (containsAny(value, "carreira", "vaga", "emprego", "career")) {
            return Set.of("Carreira");
        }
        return Set.of("Tecnologia");
    }

    static boolean isTechnologyRelevant(String text) {
        String value = " " + (text == null ? "" : text.toLowerCase(Locale.ROOT)) + " ";
        return containsAny(value, "programação", "programacao", "programming", "programador", "software", "código", "codigo", "desenvolv", "developer",
                "javascript", "typescript", "python", " java ", "kotlin", "android", " ios ", " api ",
                "linux", "open source", "github", " git ", "comput", "tecnolog", "internet", " web ",
                "segurança", "security", "cyber", "inteligência artificial", "inteligencia artificial",
                "machine learning", " ia ", " ai ", "dados", "database", "cloud", "devops", "docker",
                "kubernetes", "framework", "algoritmo", "startup", "hardware", " chip ", "mobile");
    }

    static boolean isPublisherTechnologyRelevant(String title) {
        String value = " " + (title == null ? "" : title.toLowerCase(Locale.ROOT)) + " ";
        return isTechnologyRelevant(title) || containsAny(value,
                "iphone", "samsung", "galaxy", "windows", "microsoft", "apple", "google",
                "whatsapp", "instagram", "tiktok", "smartphone", "notebook", "antivírus",
                "antivirus", "privacidade", "vazamento", "processador", " gpu ", " ssd ",
                "memória ram", "placa de vídeo", "monitor", "copilot", "chatgpt", "android",
                "tablet", "ipad", "kindle", "wi-fi", "roteador", "smart home", "casa conectada",
                "ps5", "playstation", "xbox", "steam", "gpu", "nvidia", "intel", "amd");
    }

    static boolean isPublisherEditorialArticle(String title) {
        String value = " " + (title == null ? "" : title.toLowerCase(Locale.ROOT)) + " ";
        return !containsAny(value, "oferta", "desconto", "cupom", "promoção", "promocao",
                "cashback", "menor preço", "menor preco");
    }

    /** General news feeds need an explicit technology signal in the headline. */
    static boolean isGeneralNewsTechnologyRelevant(String title) {
        String value = " " + (title == null ? "" : title.toLowerCase(Locale.ROOT)) + " ";
        return containsAny(value, "tecnolog", "software", "computador", "programação", "programacao",
                "inteligência artificial", "inteligencia artificial", "machine learning", " internet ",
                "aplicativo", " app ", "digital", "cibernétic", "cibernetic", "cyber", "startup",
                " android ", " ios ", "smartphone", "semicondutor", " chip ",
                " robô", " robot", " 5g ", " web ", " api ", " linux ");
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}

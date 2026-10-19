package br.com.techfeedbr.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techfeed")
public class TechFeedProperties {
    private final Sources sources = new Sources();
    private Duration staleAfter = Duration.ofDays(7);

    public Sources getSources() {
        return sources;
    }

    public Duration getStaleAfter() {
        return staleAfter;
    }

    public void setStaleAfter(Duration staleAfter) {
        this.staleAfter = staleAfter;
    }

    public static class Sources {
        private final Gnews gnews = new Gnews();
        private final Ebc ebc = new Ebc();
        private final Dev dev = new Dev();
        private final Tabnews tabnews = new Tabnews();
        private final Tecnoblog tecnoblog = new Tecnoblog();
        private final OlharDigital olharDigital = new OlharDigital();
        private final Publisher tudoCelular = new Publisher("https://www.tudocelular.com/rss/");
        private final Publisher hardware = new Publisher("https://www.hardware.com.br/feed/");
        private final Publisher mundoConectado = new Publisher("https://www.mundoconectado.com.br/feed/");
        private final Publisher macMagazine = new Publisher("https://macmagazine.com.br/feed/");
        private final Publisher adrenaline = new Publisher("https://www.adrenaline.com.br/feed/");

        public Gnews getGnews() { return gnews; }
        public Ebc getEbc() { return ebc; }
        public Dev getDev() { return dev; }
        public Tabnews getTabnews() { return tabnews; }
        public Tecnoblog getTecnoblog() { return tecnoblog; }
        public OlharDigital getOlharDigital() { return olharDigital; }
        public Publisher getTudoCelular() { return tudoCelular; }
        public Publisher getHardware() { return hardware; }
        public Publisher getMundoConectado() { return mundoConectado; }
        public Publisher getMacMagazine() { return macMagazine; }
        public Publisher getAdrenaline() { return adrenaline; }
    }

    public static class Publisher {
        private boolean enabled = true;
        private String feedUrl;
        private Duration ttl = Duration.ofMinutes(30);

        public Publisher(String feedUrl) { this.feedUrl = feedUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFeedUrl() { return feedUrl; }
        public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Gnews {
        private boolean enabled = true;
        private String baseUrl = "https://gnews.io/api/v4";
        private String apiKey = "";
        private Duration ttl = Duration.ofHours(1);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Ebc {
        private boolean enabled = true;
        private String feedUrl = "https://agenciabrasil.ebc.com.br/radioagencia-nacional/rss/pesquisa-e-inovacao/feed.xml";
        private String backupFeedUrl = "https://agenciabrasil.ebc.com.br/rss/ultimasnoticias/feed.xml";
        private Duration ttl = Duration.ofMinutes(15);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFeedUrl() { return feedUrl; }
        public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
        public String getBackupFeedUrl() { return backupFeedUrl; }
        public void setBackupFeedUrl(String backupFeedUrl) { this.backupFeedUrl = backupFeedUrl; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Dev {
        private boolean enabled = true;
        private String baseUrl = "https://dev.to/api";
        private Duration ttl = Duration.ofMinutes(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Tabnews {
        private boolean enabled = true;
        private String baseUrl = "https://www.tabnews.com.br/api/v1";
        private Duration ttl = Duration.ofMinutes(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class Tecnoblog {
        private boolean enabled = true;
        private String feedUrl = "https://tecnoblog.net/noticias/feed/";
        private Duration ttl = Duration.ofMinutes(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFeedUrl() { return feedUrl; }
        public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class OlharDigital {
        private boolean enabled = true;
        private String aiFeedUrl = "https://olhardigital.com.br/editorias/inteligencia-artificial/feed/";
        private String securityFeedUrl = "https://olhardigital.com.br/editorias/seguranca/feed/";
        private Duration ttl = Duration.ofMinutes(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAiFeedUrl() { return aiFeedUrl; }
        public void setAiFeedUrl(String aiFeedUrl) { this.aiFeedUrl = aiFeedUrl; }
        public String getSecurityFeedUrl() { return securityFeedUrl; }
        public void setSecurityFeedUrl(String securityFeedUrl) { this.securityFeedUrl = securityFeedUrl; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }
}

package br.com.techfeedbr.api;

import br.com.techfeedbr.domain.SourceCode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private static final List<String> CATEGORIES = List.of(
            "Inteligência Artificial", "Desenvolvimento", "Segurança", "Dados",
            "Mobile", "Cloud e DevOps", "Carreira", "Hardware", "Internet e Apps", "Tecnologia");
    private static final List<String> TECHNOLOGIES = List.of(
            "Android", "Java", "Kotlin", "JavaScript", "Python", "Cloud", "DevOps", "IA");

    @GetMapping
    Map<String, Object> catalog() {
        List<Map<String, String>> sources = Arrays.stream(SourceCode.values())
                .filter(source -> source != SourceCode.TABNEWS && source != SourceCode.GNEWS)
                .map(source -> Map.of("code", source.name(), "name", source.displayName()))
                .toList();
        return Map.of("sources", sources, "categories", CATEGORIES, "technologies", TECHNOLOGIES);
    }
}


package br.com.finlumia.document.services;

import java.util.Map;

/**
 * Metadados (label/cor/sigla) de categorias e instituições usados nos
 * relatórios. O módulo document não compartilha código com o movement (são
 * JVMs/deploys separados), então esses valores são replicados aqui —
 * copiados exatamente da paleta usada no frontend (src/config/transactions.ts),
 * que é a fonte visual real vista pelo usuário em Movimentações. Os valores
 * hardcoded em movement/CategoryService e InstitutionService divergem dessa
 * paleta, mas nunca aparecem na tela porque o frontend já conhece os ids
 * padrão e usa suas próprias cores; os relatórios não têm esse filtro, então
 * precisam bater com o frontend diretamente.
 */
public final class ReportCatalog {

    public record CategoryMeta(String label, String color) {}

    public record InstitutionMeta(String label, String color, String abbr) {}

    private static final Map<String, CategoryMeta> CATEGORIES = Map.ofEntries(
            Map.entry("alimentacao", new CategoryMeta("Alimentação", "#E69F00")),
            Map.entry("saude", new CategoryMeta("Saúde", "#56B4E9")),
            Map.entry("educacao", new CategoryMeta("Educação", "#0072B2")),
            Map.entry("transporte", new CategoryMeta("Transporte", "#CC79A7")),
            Map.entry("lazer", new CategoryMeta("Lazer", "#009E73")),
            Map.entry("moradia", new CategoryMeta("Moradia", "#F0E442")),
            Map.entry("salario", new CategoryMeta("Salário", "#56D364")),
            Map.entry("vendas", new CategoryMeta("Vendas", "#56D364")),
            Map.entry("tecnologia", new CategoryMeta("Tecnologia", "#56B4E9")),
            Map.entry("marketing", new CategoryMeta("Marketing", "#CC79A7")),
            Map.entry("servicos", new CategoryMeta("Serviços", "#9DAAB8")),
            Map.entry("investimento", new CategoryMeta("Investimento", "#FFB74D")),
            Map.entry("outros", new CategoryMeta("Outros", "#9DAAB8"))
    );

    private static final Map<String, InstitutionMeta> INSTITUTIONS = Map.ofEntries(
            Map.entry("nubank", new InstitutionMeta("Nubank", "#820AD1", "Nu")),
            Map.entry("itau", new InstitutionMeta("Itaú", "#EC7000", "Itaú")),
            Map.entry("bb", new InstitutionMeta("Banco do Brasil", "#FCDE00", "BB")),
            Map.entry("bradesco", new InstitutionMeta("Bradesco", "#CC092F", "Br")),
            Map.entry("santander", new InstitutionMeta("Santander", "#EC0000", "San")),
            Map.entry("picpay", new InstitutionMeta("PicPay", "#11C76F", "Pic")),
            Map.entry("inter", new InstitutionMeta("Inter", "#FF7A00", "In")),
            Map.entry("c6", new InstitutionMeta("C6 Bank", "#222222", "C6")),
            Map.entry("xp", new InstitutionMeta("XP Investimentos", "#1B1A1C", "XP"))
    );

    private ReportCatalog() {
    }

    public static CategoryMeta categoryMeta(String id) {
        CategoryMeta meta = CATEGORIES.get(id);
        return meta != null ? meta : new CategoryMeta(id, "#9DAAB8");
    }

    public static InstitutionMeta institutionMeta(String id) {
        InstitutionMeta meta = INSTITUTIONS.get(id);
        if (meta != null) return meta;
        String abbr = id.length() >= 2 ? id.substring(0, 2).toUpperCase() : id.toUpperCase();
        return new InstitutionMeta(id, "#9DAAB8", abbr);
    }
}

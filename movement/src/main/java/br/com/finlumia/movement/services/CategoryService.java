package br.com.finlumia.movement.services;

import java.util.List;

import br.com.finlumia.movement.views.CategoryView;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private static final List<CategoryView> CATEGORIES = List.of(
            new CategoryView("alimentacao",  "Alimentação",  "#F97316", "#FFF7ED", "🍽️"),
            new CategoryView("saude",        "Saúde",        "#EF4444", "#FEF2F2", "🏥"),
            new CategoryView("educacao",     "Educação",     "#3B82F6", "#EFF6FF", "📚"),
            new CategoryView("transporte",   "Transporte",   "#8B5CF6", "#F5F3FF", "🚗"),
            new CategoryView("lazer",        "Lazer",        "#EC4899", "#FDF2F8", "🎮"),
            new CategoryView("moradia",      "Moradia",      "#14B8A6", "#F0FDFA", "🏠"),
            new CategoryView("salario",      "Salário",      "#22C55E", "#F0FDF4", "💰"),
            new CategoryView("vendas",       "Vendas",       "#10B981", "#ECFDF5", "🛒"),
            new CategoryView("tecnologia",   "Tecnologia",   "#6366F1", "#EEF2FF", "💻"),
            new CategoryView("marketing",    "Marketing",    "#F59E0B", "#FFFBEB", "📣"),
            new CategoryView("servicos",     "Serviços",     "#64748B", "#F8FAFC", "🔧"),
            new CategoryView("investimento", "Investimento", "#0EA5E9", "#F0F9FF", "📈"),
            new CategoryView("outros",       "Outros",       "#94A3B8", "#F8FAFC", null)
    );

    public List<CategoryView> list() {
        return CATEGORIES;
    }
}

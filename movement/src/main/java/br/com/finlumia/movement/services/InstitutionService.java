package br.com.finlumia.movement.services;

import java.util.List;

import br.com.finlumia.movement.views.InstitutionView;
import org.springframework.stereotype.Service;

@Service
public class InstitutionService {

    private static final List<InstitutionView> INSTITUTIONS = List.of(
            new InstitutionView("nubank",    "Nubank",           "#820AD1", "NU"),
            new InstitutionView("itau",      "Itaú",             "#F77F00", "IT"),
            new InstitutionView("bb",        "Banco do Brasil",  "#FFDD00", "BB"),
            new InstitutionView("bradesco",  "Bradesco",         "#CC0000", "BR"),
            new InstitutionView("santander", "Santander",        "#EC0000", "SA"),
            new InstitutionView("picpay",    "PicPay",           "#21C25E", "PP"),
            new InstitutionView("inter",     "Banco Inter",      "#FF7A00", "IN"),
            new InstitutionView("c6",        "C6 Bank",          "#222222", "C6"),
            new InstitutionView("xp",        "XP Investimentos", "#000000", "XP")
    );

    public List<InstitutionView> list() {
        return INSTITUTIONS;
    }
}

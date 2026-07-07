package br.com.finlumia.document.views;

import java.math.BigDecimal;
import java.util.List;

public record NetWorthView(
        List<NetWorthDataView> data,
        BigDecimal current,
        BigDecimal initial,
        BigDecimal growth
) {
}

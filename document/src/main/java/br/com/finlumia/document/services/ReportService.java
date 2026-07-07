package br.com.finlumia.document.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import br.com.finlumia.document.models.InsightType;
import br.com.finlumia.document.models.Period;
import br.com.finlumia.document.repositorys.ReportRepository;
import br.com.finlumia.document.views.CategoryBreakdownListView;
import br.com.finlumia.document.views.CategoryBreakdownView;
import br.com.finlumia.document.views.CashFlowView;
import br.com.finlumia.document.views.InsightView;
import br.com.finlumia.document.views.InsightsView;
import br.com.finlumia.document.views.InstitutionBreakdownListView;
import br.com.finlumia.document.views.InstitutionBreakdownView;
import br.com.finlumia.document.views.KpiSummaryView;
import br.com.finlumia.document.views.MonthlyComparisonItemView;
import br.com.finlumia.document.views.MonthlyComparisonView;
import br.com.finlumia.document.views.MonthlySummaryView;
import br.com.finlumia.document.views.NetWorthDataView;
import br.com.finlumia.document.views.NetWorthView;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final String[] MONTH_ABBR_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    private record DateRange(LocalDate start, LocalDate end) {}

    // ── Período ──────────────────────────────────────────────────────────

    private DateRange resolveRange(Period period, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate today = LocalDate.now();

        if (periodStart != null || periodEnd != null || period == Period.CUSTOM) {
            LocalDate start = periodStart != null ? periodStart : today.minusMonths(6);
            LocalDate end = periodEnd != null ? periodEnd : today;
            if (start.isAfter(end)) {
                throw new FinlumiaException(422, "Período inválido", "A data inicial não pode ser depois da data final.");
            }
            return new DateRange(start, end);
        }

        Period effective = period != null ? period : Period.SIX_MONTHS;
        return switch (effective) {
            case THREE_MONTHS -> new DateRange(today.minusMonths(3), today);
            case SIX_MONTHS -> new DateRange(today.minusMonths(6), today);
            case TWELVE_MONTHS -> new DateRange(today.minusMonths(12), today);
            case YTD -> new DateRange(today.withDayOfYear(1), today);
            case CUSTOM -> new DateRange(today.minusMonths(6), today);
        };
    }

    /** Janela imediatamente anterior, com a mesma duração — usada para calcular tendências. */
    private DateRange previousRange(DateRange range) {
        long days = ChronoUnit.DAYS.between(range.start(), range.end()) + 1;
        LocalDate prevEnd = range.start().minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);
        return new DateRange(prevStart, prevEnd);
    }

    private int periodMonths(LocalDate start, LocalDate end) {
        long months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1)) + 1;
        return (int) Math.max(1, months);
    }

    private List<YearMonth> monthsBetween(LocalDate start, LocalDate end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    // ── Matemática de percentuais ────────────────────────────────────────

    private static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.divide(whole, 4, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
        if (from == null || from.compareTo(BigDecimal.ZERO) == 0) {
            return to != null && to.compareTo(BigDecimal.ZERO) != 0 ? HUNDRED : BigDecimal.ZERO;
        }
        return to.subtract(from).divide(from.abs(), 4, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private static BigDecimal scale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP);
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) return "despesa";
        String normalized = type.trim().toLowerCase();
        if (!normalized.equals("receita") && !normalized.equals("despesa")) {
            throw new FinlumiaException(422, "Tipo inválido", "O parâmetro 'type' deve ser 'receita' ou 'despesa'.");
        }
        return normalized;
    }

    // ── KPIs ─────────────────────────────────────────────────────────────

    private KpiSummaryView computeKpis(UUID userKey, DateRange range) {
        ReportRepository.Totals totals = reportRepository.totals(userKey, range.start(), range.end());
        BigDecimal totalReceitas = totals.totalIncome();
        BigDecimal totalDespesas = totals.totalExpenses();
        BigDecimal saldoLiquido = totalReceitas.subtract(totalDespesas);
        BigDecimal taxaPoupanca = percentOf(saldoLiquido, totalReceitas);

        BigDecimal patrimonioAtual = reportRepository.cumulativeBalanceUpTo(userKey, range.end());
        BigDecimal patrimonioAnterior = reportRepository.cumulativeBalanceUpTo(userKey, range.start().minusDays(1));
        BigDecimal crescimentoPatrimonio = percentChange(patrimonioAnterior, patrimonioAtual);

        return new KpiSummaryView(
                scale2(totalReceitas), scale2(totalDespesas), scale2(saldoLiquido),
                scale1(taxaPoupanca), scale2(patrimonioAtual), scale1(crescimentoPatrimonio),
                periodMonths(range.start(), range.end()));
    }

    public KpiSummaryView getKpis(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd) {
        return computeKpis(userKey, resolveRange(period, periodStart, periodEnd));
    }

    // ── Fluxo de caixa / série mensal ────────────────────────────────────

    private List<MonthlySummaryView> monthlySeries(UUID userKey, DateRange range) {
        List<ReportRepository.MonthlyRow> rows = reportRepository.monthlyTotals(userKey, range.start(), range.end());
        Map<YearMonth, ReportRepository.MonthlyRow> byMonth = rows.stream()
                .collect(Collectors.toMap(r -> YearMonth.of(r.year(), r.month()), r -> r));

        List<MonthlySummaryView> result = new ArrayList<>();
        for (YearMonth ym : monthsBetween(range.start(), range.end())) {
            ReportRepository.MonthlyRow row = byMonth.get(ym);
            BigDecimal income = row != null ? row.income() : BigDecimal.ZERO;
            BigDecimal expenses = row != null ? row.expenses() : BigDecimal.ZERO;
            BigDecimal saldo = income.subtract(expenses);
            LocalDate cumulativeAsOf = ym.equals(YearMonth.from(range.end())) ? range.end() : ym.atEndOfMonth();
            BigDecimal patrimonio = reportRepository.cumulativeBalanceUpTo(userKey, cumulativeAsOf);
            result.add(new MonthlySummaryView(
                    MONTH_ABBR_PT[ym.getMonthValue() - 1], ym.getYear(),
                    scale2(income), scale2(expenses), scale2(saldo), scale2(patrimonio)));
        }
        return result;
    }

    public CashFlowView getCashFlow(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd) {
        DateRange range = resolveRange(period, periodStart, periodEnd);
        return new CashFlowView(monthlySeries(userKey, range), computeKpis(userKey, range));
    }

    public MonthlyComparisonView getMonthlyComparison(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd) {
        DateRange range = resolveRange(period, periodStart, periodEnd);
        List<MonthlyComparisonItemView> data = monthlySeries(userKey, range).stream()
                .map(m -> new MonthlyComparisonItemView(
                        m.month() + "/" + String.valueOf(m.year()).substring(2),
                        m.receitas(), m.despesas(), m.saldo()))
                .toList();
        return new MonthlyComparisonView(data);
    }

    // ── Por categoria ────────────────────────────────────────────────────

    public CategoryBreakdownListView getByCategory(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd, String type) {
        DateRange range = resolveRange(period, periodStart, periodEnd);
        String normalizedType = normalizeType(type);
        List<ReportRepository.CategoryRow> rows = reportRepository.categoryTotals(userKey, range.start(), range.end(), normalizedType);
        BigDecimal grandTotal = rows.stream().map(ReportRepository.CategoryRow::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        DateRange prevRange = previousRange(range);

        List<CategoryBreakdownView> data = new ArrayList<>();
        for (ReportRepository.CategoryRow row : rows) {
            ReportCatalog.CategoryMeta meta = ReportCatalog.categoryMeta(row.category());
            BigDecimal percent = percentOf(row.total(), grandTotal);
            BigDecimal prevTotal = reportRepository.categoryTotal(userKey, row.category(), prevRange.start(), prevRange.end(), normalizedType);
            BigDecimal trend = percentChange(prevTotal, row.total());
            data.add(new CategoryBreakdownView(
                    row.category(), meta.label(), meta.color(),
                    scale2(row.total()), scale1(percent), scale1(trend), row.transactions()));
        }
        return new CategoryBreakdownListView(data, scale2(grandTotal));
    }

    // ── Por instituição ──────────────────────────────────────────────────

    public InstitutionBreakdownListView getByInstitution(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd) {
        DateRange range = resolveRange(period, periodStart, periodEnd);
        // Sempre despesas: a tela ("Distribuição de gastos por banco") não expõe filtro de tipo aqui.
        List<ReportRepository.InstitutionRow> rows = reportRepository.institutionTotals(userKey, range.start(), range.end(), "despesa");
        BigDecimal grandTotal = rows.stream().map(ReportRepository.InstitutionRow::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InstitutionBreakdownView> data = new ArrayList<>();
        for (ReportRepository.InstitutionRow row : rows) {
            ReportCatalog.InstitutionMeta meta = ReportCatalog.institutionMeta(row.institution());
            BigDecimal percent = percentOf(row.total(), grandTotal);
            data.add(new InstitutionBreakdownView(row.institution(), meta.label(), meta.color(), meta.abbr(), scale2(row.total()), scale1(percent)));
        }
        return new InstitutionBreakdownListView(data, scale2(grandTotal));
    }

    // ── Patrimônio ───────────────────────────────────────────────────────

    public NetWorthView getNetWorth(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd) {
        DateRange range = resolveRange(period, periodStart, periodEnd);
        List<NetWorthDataView> data = monthlySeries(userKey, range).stream()
                .map(m -> new NetWorthDataView(m.month(), m.year(), m.patrimonio()))
                .toList();
        BigDecimal current = reportRepository.cumulativeBalanceUpTo(userKey, range.end());
        BigDecimal initial = reportRepository.cumulativeBalanceUpTo(userKey, range.start().minusDays(1));
        BigDecimal growth = percentChange(initial, current);
        return new NetWorthView(data, scale2(current), scale2(initial), scale1(growth));
    }

    // ── Insights automáticos ─────────────────────────────────────────────

    public InsightsView getInsights(UUID userKey, Period period, LocalDate periodStart, LocalDate periodEnd, Integer limit, List<InsightType> types) {
        DateRange range = resolveRange(period, periodStart, periodEnd);
        KpiSummaryView kpis = computeKpis(userKey, range);
        CategoryBreakdownListView despesas = getByCategory(userKey, period, periodStart, periodEnd, "despesa");

        DateRange prevRange = previousRange(range);
        ReportRepository.Totals prevTotals = reportRepository.totals(userKey, prevRange.start(), prevRange.end());
        BigDecimal expenseTrend = percentChange(prevTotals.totalExpenses(), kpis.totalDespesas());

        Instant generatedAt = Instant.now();
        List<InsightView> candidates = new ArrayList<>();

        if (kpis.taxaPoupanca().compareTo(BigDecimal.ZERO) < 0) {
            candidates.add(insight(InsightType.ALERT, "Despesas maiores que receitas",
                    "Suas despesas superaram as receitas no período, reduzindo seu saldo em R$ " + kpis.saldoLiquido().abs() + ".",
                    "trending-down", null, kpis.taxaPoupanca(), generatedAt));
        } else if (kpis.taxaPoupanca().compareTo(BigDecimal.valueOf(20)) >= 0) {
            candidates.add(insight(InsightType.POSITIVE, "Boa taxa de poupança",
                    "Você guardou " + kpis.taxaPoupanca() + "% do que recebeu no período — acima da meta de 20%.",
                    "piggy", null, kpis.taxaPoupanca(), generatedAt));
        } else {
            candidates.add(insight(InsightType.NEUTRAL, "Taxa de poupança abaixo da meta",
                    "Você guardou " + kpis.taxaPoupanca() + "% do que recebeu — a meta recomendada é 20%.",
                    "piggy", null, kpis.taxaPoupanca(), generatedAt));
        }

        if (!despesas.data().isEmpty()) {
            CategoryBreakdownView top = despesas.data().get(0);
            if (top.percent().compareTo(BigDecimal.valueOf(35)) >= 0) {
                candidates.add(insight(InsightType.ALERT, "Gasto concentrado em " + top.label(),
                        top.label() + " representa " + top.percent() + "% das suas despesas no período.",
                        "alert", top.categoryId(), top.percent(), generatedAt));
            }
        }

        if (expenseTrend.compareTo(BigDecimal.valueOf(10)) > 0) {
            candidates.add(insight(InsightType.NEGATIVE, "Despesas em alta",
                    "Suas despesas subiram " + expenseTrend + "% em relação ao período anterior.",
                    "trending-up", null, expenseTrend, generatedAt));
        } else if (expenseTrend.compareTo(BigDecimal.valueOf(-10)) < 0) {
            candidates.add(insight(InsightType.POSITIVE, "Despesas em queda",
                    "Suas despesas caíram " + expenseTrend.abs() + "% em relação ao período anterior.",
                    "trending-down", null, expenseTrend, generatedAt));
        }

        if (kpis.crescimentoPatrimonio().compareTo(BigDecimal.ZERO) > 0) {
            candidates.add(insight(InsightType.POSITIVE, "Patrimônio em crescimento",
                    "Seu patrimônio cresceu " + kpis.crescimentoPatrimonio() + "% no período.",
                    "chart", null, kpis.crescimentoPatrimonio(), generatedAt));
        } else if (kpis.crescimentoPatrimonio().compareTo(BigDecimal.ZERO) < 0) {
            candidates.add(insight(InsightType.NEGATIVE, "Patrimônio em queda",
                    "Seu patrimônio caiu " + kpis.crescimentoPatrimonio().abs() + "% no período.",
                    "chart", null, kpis.crescimentoPatrimonio(), generatedAt));
        }

        List<InsightView> filtered = (types == null || types.isEmpty())
                ? candidates
                : candidates.stream().filter(i -> types.contains(i.type())).toList();

        int effectiveLimit = (limit != null && limit > 0) ? limit : 5;
        List<InsightView> result = filtered.stream().limit(effectiveLimit).toList();

        return new InsightsView(result, generatedAt);
    }

    private InsightView insight(InsightType type, String title, String description, String icon,
                                 String relatedCategory, BigDecimal delta, Instant generatedAt) {
        return new InsightView(UUID.randomUUID(), type, title, description, icon, relatedCategory, delta, generatedAt);
    }
}

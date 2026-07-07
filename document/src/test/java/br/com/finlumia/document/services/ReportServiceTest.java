package br.com.finlumia.document.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.document.models.InsightType;
import br.com.finlumia.document.models.Period;
import br.com.finlumia.document.repositorys.ReportRepository;
import br.com.finlumia.document.views.CategoryBreakdownListView;
import br.com.finlumia.document.views.InsightsView;
import br.com.finlumia.document.views.KpiSummaryView;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;

    @InjectMocks
    ReportService reportService;

    UUID userKey;

    @BeforeEach
    void setUp() {
        userKey = UUID.randomUUID();
        // Stubs neutros — cada teste sobrescreve o que precisa. lenient() não é
        // necessário porque nem todo teste aciona todo caminho (ex.: getByCategory
        // não chama monthlyTotals), mas os testes abaixo só fixam o que usam.
    }

    // ── KPIs / percentuais ───────────────────────────────────────────────

    @Test
    @DisplayName("getKpis: calcula saldo, taxa de poupança e crescimento de patrimônio corretamente")
    void getKpis_computesTotalsCorrectly() {
        when(reportRepository.totals(eq(userKey), any(), any()))
                .thenReturn(new ReportRepository.Totals(new BigDecimal("1000.00"), new BigDecimal("600.00")));
        // patrimônio atual (fim do período) = 5000; anterior (véspera do início) = 4000 -> crescimento 25%
        when(reportRepository.cumulativeBalanceUpTo(eq(userKey), any()))
                .thenReturn(new BigDecimal("5000.00"))
                .thenReturn(new BigDecimal("4000.00"));

        KpiSummaryView kpi = reportService.getKpis(userKey, Period.SIX_MONTHS, null, null);

        assertThat(kpi.totalReceitas()).isEqualByComparingTo("1000.00");
        assertThat(kpi.totalDespesas()).isEqualByComparingTo("600.00");
        assertThat(kpi.saldoLiquido()).isEqualByComparingTo("400.00");
        assertThat(kpi.taxaPoupanca()).isEqualByComparingTo("40.0"); // 400/1000 * 100
        assertThat(kpi.patrimonioAtual()).isEqualByComparingTo("5000.00");
        assertThat(kpi.crescimentoPatrimonio()).isEqualByComparingTo("25.0"); // (5000-4000)/4000 * 100
        assertThat(kpi.periodMonths()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("getKpis: sem receitas no período, taxa de poupança é 0 (não divide por zero)")
    void getKpis_noIncome_taxaPoupancaIsZero() {
        when(reportRepository.totals(eq(userKey), any(), any()))
                .thenReturn(new ReportRepository.Totals(BigDecimal.ZERO, new BigDecimal("300.00")));
        when(reportRepository.cumulativeBalanceUpTo(eq(userKey), any()))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO);

        KpiSummaryView kpi = reportService.getKpis(userKey, Period.THREE_MONTHS, null, null);

        assertThat(kpi.taxaPoupanca()).isEqualByComparingTo("0");
        assertThat(kpi.crescimentoPatrimonio()).isEqualByComparingTo("0");
        assertThat(kpi.saldoLiquido()).isEqualByComparingTo("-300.00");
    }

    @Test
    @DisplayName("getKpis: período customizado com data inicial depois da final lança 422")
    void getKpis_invalidCustomRange_throws422() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> reportService.getKpis(userKey, Period.CUSTOM, start, end))
                .isInstanceOf(FinlumiaException.class)
                .hasFieldOrPropertyWithValue("code", 422);
    }

    @Test
    @DisplayName("getKpis: período customizado usa exatamente as datas informadas")
    void getKpis_customRange_usesGivenDates() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        when(reportRepository.totals(userKey, start, end))
                .thenReturn(new ReportRepository.Totals(new BigDecimal("100.00"), new BigDecimal("50.00")));
        when(reportRepository.cumulativeBalanceUpTo(eq(userKey), any())).thenReturn(BigDecimal.ZERO);

        KpiSummaryView kpi = reportService.getKpis(userKey, Period.CUSTOM, start, end);

        assertThat(kpi.saldoLiquido()).isEqualByComparingTo("50.00");
    }

    // ── Categorias ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getByCategory: calcula percentual sobre o total e tendência vs período anterior")
    void getByCategory_computesPercentAndTrend() {
        when(reportRepository.categoryTotals(eq(userKey), any(), any(), eq("despesa")))
                .thenReturn(List.of(
                        new ReportRepository.CategoryRow("alimentacao", new BigDecimal("300.00"), 5),
                        new ReportRepository.CategoryRow("transporte", new BigDecimal("100.00"), 2)));
        // período anterior: alimentacao gastou 200 (tendência de alta de 50%); transporte gastou 0 (tendência 100%)
        when(reportRepository.categoryTotal(eq(userKey), eq("alimentacao"), any(), any(), eq("despesa")))
                .thenReturn(new BigDecimal("200.00"));
        when(reportRepository.categoryTotal(eq(userKey), eq("transporte"), any(), any(), eq("despesa")))
                .thenReturn(BigDecimal.ZERO);

        CategoryBreakdownListView result = reportService.getByCategory(userKey, Period.SIX_MONTHS, null, null, "despesa");

        assertThat(result.total()).isEqualByComparingTo("400.00");
        assertThat(result.data()).hasSize(2);
        assertThat(result.data().get(0).categoryId()).isEqualTo("alimentacao");
        assertThat(result.data().get(0).percent()).isEqualByComparingTo("75.0"); // 300/400
        assertThat(result.data().get(0).trend()).isEqualByComparingTo("50.0");   // (300-200)/200
        assertThat(result.data().get(0).label()).isEqualTo("Alimentação");
        assertThat(result.data().get(1).trend()).isEqualByComparingTo("100.0"); // era 0, virou 100 -> alta de 100%
    }

    @Test
    @DisplayName("getByCategory: tipo inválido lança 422 e não chega a consultar o repositório")
    void getByCategory_invalidType_throws422() {
        assertThatThrownBy(() -> reportService.getByCategory(userKey, Period.SIX_MONTHS, null, null, "poupanca"))
                .isInstanceOf(FinlumiaException.class)
                .hasFieldOrPropertyWithValue("code", 422);
    }

    @Test
    @DisplayName("getByCategory: tipo nulo assume despesa por padrão")
    void getByCategory_nullType_defaultsToDespesa() {
        when(reportRepository.categoryTotals(eq(userKey), any(), any(), eq("despesa")))
                .thenReturn(List.of());

        reportService.getByCategory(userKey, Period.SIX_MONTHS, null, null, null);

        // Nenhuma exceção e a consulta foi feita com "despesa" — verificado pelo stub acima
        // (Mockito falharia com UnnecessaryStubbingException se o stub não fosse usado, via strict stubs).
    }

    // ── Insights ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getInsights: taxa de poupança negativa gera insight de alerta")
    void getInsights_negativeSavingsRate_generatesAlert() {
        when(reportRepository.totals(eq(userKey), any(), any()))
                .thenReturn(new ReportRepository.Totals(new BigDecimal("100.00"), new BigDecimal("500.00")));
        when(reportRepository.cumulativeBalanceUpTo(eq(userKey), any())).thenReturn(BigDecimal.ZERO);
        when(reportRepository.categoryTotals(eq(userKey), any(), any(), anyString())).thenReturn(List.of());

        InsightsView insights = reportService.getInsights(userKey, Period.SIX_MONTHS, null, null, 5, null);

        assertThat(insights.data()).isNotEmpty();
        assertThat(insights.data().get(0).type()).isEqualTo(InsightType.ALERT);
    }

    @Test
    @DisplayName("getInsights: respeita o limite máximo de itens retornados")
    void getInsights_respectsLimit() {
        when(reportRepository.totals(eq(userKey), any(), any()))
                .thenReturn(new ReportRepository.Totals(new BigDecimal("1000.00"), new BigDecimal("200.00")));
        when(reportRepository.cumulativeBalanceUpTo(eq(userKey), any()))
                .thenReturn(new BigDecimal("2000.00"))
                .thenReturn(new BigDecimal("1000.00"));
        when(reportRepository.categoryTotals(eq(userKey), any(), any(), anyString())).thenReturn(List.of());

        InsightsView insights = reportService.getInsights(userKey, Period.SIX_MONTHS, null, null, 1, null);

        assertThat(insights.data()).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("getInsights: filtro por tipo só retorna insights daquele tipo")
    void getInsights_filtersByType() {
        // Cenário com pelo menos duas categorias de insight candidatas (ALERT de
        // poupança negativa + POSITIVE de patrimônio em crescimento), para que o
        // filtro por ALERT realmente exclua algo — sem isso, um allMatch em lista
        // vazia passaria de forma vazia (sempre verdadeiro) e não provaria nada.
        when(reportRepository.totals(eq(userKey), any(), any()))
                .thenReturn(new ReportRepository.Totals(new BigDecimal("100.00"), new BigDecimal("500.00")));
        when(reportRepository.cumulativeBalanceUpTo(eq(userKey), any()))
                .thenReturn(new BigDecimal("2000.00"))
                .thenReturn(new BigDecimal("1000.00"));
        when(reportRepository.categoryTotals(eq(userKey), any(), any(), anyString())).thenReturn(List.of());

        InsightsView insights = reportService.getInsights(userKey, Period.SIX_MONTHS, null, null, 10, List.of(InsightType.ALERT));

        assertThat(insights.data()).isNotEmpty();
        assertThat(insights.data()).allMatch(i -> i.type() == InsightType.ALERT);
    }
}

package br.com.finlumia.document.controllers.external;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.document.controllers.ExternalApi;
import br.com.finlumia.document.models.InsightType;
import br.com.finlumia.document.models.Period;
import br.com.finlumia.document.services.ReportService;
import br.com.finlumia.document.views.CategoryBreakdownListView;
import br.com.finlumia.document.views.CashFlowView;
import br.com.finlumia.document.views.InsightsView;
import br.com.finlumia.document.views.InstitutionBreakdownListView;
import br.com.finlumia.document.views.KpiSummaryView;
import br.com.finlumia.document.views.MonthlyComparisonView;
import br.com.finlumia.document.views.NetWorthView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ExternalApi
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Relatórios e análises financeiras")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/kpis")
    @Operation(summary = "KPIs do período", description = "Retorna KPIs agregados do período selecionado.")
    @ApiResponse(responseCode = "200", description = "KPIs retornados com sucesso")
    public ResponseEntity<KpiSummaryView> getKpis(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(reportService.getKpis(userKey, period, periodStart, periodEnd));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Fluxo de caixa", description = "Série mensal de receitas, despesas e saldo para o gráfico de fluxo de caixa.")
    @ApiResponse(responseCode = "200", description = "Fluxo de caixa retornado com sucesso")
    public ResponseEntity<CashFlowView> getCashFlow(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(reportService.getCashFlow(userKey, period, periodStart, periodEnd));
    }

    @GetMapping("/by-category")
    @Operation(summary = "Breakdown por categoria", description = "Breakdown de despesas por categoria com % de participação e tendência vs período anterior.")
    @ApiResponse(responseCode = "200", description = "Breakdown retornado com sucesso")
    public ResponseEntity<CategoryBreakdownListView> getByCategory(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false, defaultValue = "despesa") String type) {
        return ResponseEntity.ok(reportService.getByCategory(userKey, period, periodStart, periodEnd, type));
    }

    @GetMapping("/by-institution")
    @Operation(summary = "Breakdown por instituição", description = "Distribuição de gastos/receitas por instituição financeira.")
    @ApiResponse(responseCode = "200", description = "Breakdown retornado com sucesso")
    public ResponseEntity<InstitutionBreakdownListView> getByInstitution(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(reportService.getByInstitution(userKey, period, periodStart, periodEnd));
    }

    @GetMapping("/net-worth")
    @Operation(summary = "Patrimônio líquido", description = "Série histórica do patrimônio líquido acumulado mês a mês.")
    @ApiResponse(responseCode = "200", description = "Série de patrimônio retornada com sucesso")
    public ResponseEntity<NetWorthView> getNetWorth(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(reportService.getNetWorth(userKey, period, periodStart, periodEnd));
    }

    @GetMapping("/monthly-comparison")
    @Operation(summary = "Comparativo mensal", description = "Dados mensais agrupados para o gráfico de barras comparativo.")
    @ApiResponse(responseCode = "200", description = "Comparativo mensal retornado com sucesso")
    public ResponseEntity<MonthlyComparisonView> getMonthlyComparison(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(reportService.getMonthlyComparison(userKey, period, periodStart, periodEnd));
    }

    @GetMapping("/insights")
    @Operation(summary = "Insights automáticos", description = "Lista de insights automáticos gerados pela engine de análise.")
    @ApiResponse(responseCode = "200", description = "Insights retornados com sucesso")
    public ResponseEntity<InsightsView> getInsights(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false) List<InsightType> types) {
        return ResponseEntity.ok(reportService.getInsights(userKey, period, periodStart, periodEnd, limit, types));
    }
}

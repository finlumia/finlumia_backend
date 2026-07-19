package br.com.finlumia.document.services;

import java.time.LocalDate;
import java.util.UUID;

import br.com.finlumia.document.models.ExportFormat;
import br.com.finlumia.document.models.ExportReportRequest;
import br.com.finlumia.document.models.Period;
import br.com.finlumia.document.views.ExportJobView;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    public byte[] exportTransactions(UUID userKey, ExportFormat format, Period period, LocalDate periodStart, LocalDate periodEnd, String type, String category) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Object exportReport(UUID userKey, ExportReportRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ExportJobView getJobStatus(UUID userKey, UUID jobId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}

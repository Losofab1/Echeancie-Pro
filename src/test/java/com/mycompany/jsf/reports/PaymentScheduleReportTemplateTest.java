package com.mycompany.jsf.reports;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentScheduleReportTemplateTest {

    @Test
    void receiptTemplateExportsApprovedPaymentAsPdf() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("receiptNumber", "0e16e8ea-6473-48ca-85fa-4a2ffdc48517");
        parameters.put("clientName", "Client de test");
        parameters.put("clientEmail", "client@example.test");
        parameters.put("scheduleReference", "ECH-TEST-001");
        parameters.put("installmentNumber", (short) 1);
        parameters.put("installmentAmount", 1250L);
        parameters.put("amount", 500L);
        parameters.put("channel", "Virement");
        parameters.put("paymentReference", "VIR-001");
        parameters.put("paidAt", new Date());
        parameters.put("managerName", "Gestionnaire de test");

        JasperPrint print;
        try (InputStream template = getClass().getResourceAsStream(
                "/reports/payment-receipt.jrxml")) {
            print = JasperFillManager.fillReport(
                    JasperCompileManager.compileReport(template),
                    parameters,
                    new JREmptyDataSource(1));
        }

        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)
                .startsWith("%PDF-"));
    }

    @Test
    void templateExportsScheduleAsPdfAndExcel() throws Exception {
        Map<String, Object> installment = new HashMap<>();
        installment.put("installmentNumber", (short) 1);
        installment.put("amount", 1250L);
        installment.put("dueDate", new Date());
        installment.put("paid", Boolean.FALSE);
        installment.put("paidAt", null);
        installment.put("channel", null);
        installment.put("paymentReference", null);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scheduleReference", "ECH-TEST-001");
        parameters.put("scheduleTotal", 1250L);
        parameters.put("confirmationStatus", "Confirmé");
        parameters.put("createdAt", new Date());
        parameters.put("clientName", "Client de test");
        parameters.put("clientEmail", "client@example.test");
        parameters.put("clientPhone", "");
        parameters.put("managerName", "Gestionnaire de test");

        JasperPrint print;
        try (InputStream template = getClass().getResourceAsStream(
                "/reports/payment-schedule.jrxml")) {
            print = JasperFillManager.fillReport(
                    JasperCompileManager.compileReport(template),
                    parameters,
                    new JRMapCollectionDataSource(List.of(installment)));
        }

        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)
                .startsWith("%PDF-"));

        ByteArrayOutputStream excel = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(excel));
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(false);
        configuration.setDetectCellType(true);
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        assertEquals((byte) 'P', excel.toByteArray()[0]);
        assertEquals((byte) 'K', excel.toByteArray()[1]);
    }
}

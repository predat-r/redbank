package com.redmath.redbank.statement;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.redmath.redbank.statement.dto.StatementData;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StatementPdfGenerator {

  private static final Logger log = LoggerFactory.getLogger(StatementPdfGenerator.class);

  private static final Color PRIMARY_600 = Color.decode("#89221C");
  private static final Color NEUTRAL_0 = Color.decode("#FFFFFF");
  private static final Color NEUTRAL_100 = Color.decode("#EEF0F3");
  private static final Color NEUTRAL_200 = Color.decode("#DEE2E8");
  private static final Color NEUTRAL_500 = Color.decode("#707886");
  private static final Color NEUTRAL_700 = Color.decode("#363C48");
  private static final Color NEUTRAL_800 = Color.decode("#22262F");
  private static final Color SLATE_600 = Color.decode("#384558");
  private static final Color SUCCESS_GREEN = Color.decode("#1E7B34");

  private final String logoPath;

  public StatementPdfGenerator(
      @Value("${redbank.branding.logo-path:static/branding/logo.png}") String logoPath) {
    this.logoPath = logoPath;
  }

  public byte[] generatePdf(StatementData data) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4, 36, 36, 36, 54);
      PdfWriter writer = PdfWriter.getInstance(document, baos);
      writer.setPageEvent(new FooterEvent(data));
      document.open();

      addHeader(document);
      addAccountDetails(document, data);
      addTransactionsTable(document, data);

      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate PDF statement", e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  private void addHeader(Document document) throws Exception {
    PdfPTable headerTable = new PdfPTable(2);
    headerTable.setWidthPercentage(100);
    headerTable.setWidths(new float[]{1.5f, 1f});
    headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

    PdfPCell leftCell = new PdfPCell();
    leftCell.setBorder(Rectangle.NO_BORDER);
    leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    try {
      URL logoUrl = getClass().getClassLoader().getResource(logoPath);
      if (logoUrl != null) {
        Image logo = Image.getInstance(logoUrl);
        logo.scaleToFit(120, 120);
        leftCell.addElement(logo);
      }
    } catch (BadElementException | IOException e) {
      log.warn("Logo not found or could not be loaded from path: {}", logoPath);
    }
    headerTable.addCell(leftCell);

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, NEUTRAL_800);
    PdfPCell rightCell = new PdfPCell(new Phrase("Account Statement", titleFont));
    rightCell.setBorder(Rectangle.NO_BORDER);
    rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    rightCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
    rightCell.setPaddingBottom(10f);
    headerTable.addCell(rightCell);

    document.add(headerTable);
    document.add(new Paragraph(" "));
  }

  private void addAccountDetails(Document document, StatementData data) throws Exception {
    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);
    table.setSpacingBefore(10f);
    table.setSpacingAfter(20f);

    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_600);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, NEUTRAL_700);

    DecimalFormat df = new DecimalFormat("#,##0.00");

    addDetailCell(table, "Account Title", data.getAccountHolderName(), labelFont, valueFont);
    addDetailCell(table, "Account Number", data.getAccountNumber(), labelFont, valueFont);
    addDetailCell(table, "", "", labelFont, valueFont);

    addDetailCell(table, "Currency", data.getCurrency(), labelFont, valueFont);
    addDetailCell(table, "From Date", data.getFromDate().toString(), labelFont, valueFont);
    addDetailCell(table, "To Date", data.getToDate().toString(), labelFont, valueFont);

    addDetailCell(table, "Opening Balance", df.format(data.getOpeningBalance()), labelFont,
        valueFont);
    addDetailCell(table, "Closing Balance", df.format(data.getClosingBalance()), labelFont,
        valueFont);
    addDetailCell(table, "Address", data.getAddress(), labelFont, valueFont);

    document.add(table);
  }

  private void addDetailCell(PdfPTable table, String label, String value, Font labelFont,
      Font valueFont) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPaddingBottom(10f);

    if (!label.isEmpty()) {
      cell.addElement(new Paragraph(label, labelFont));
      Paragraph val = new Paragraph(value != null ? value : "N/A", valueFont);
      cell.addElement(val);
    }
    table.addCell(cell);
  }

  private void addTransactionsTable(Document document, StatementData data) throws Exception {
    if (data.getTransactions() == null || data.getTransactions().isEmpty()) {
      Font font = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, NEUTRAL_500);
      document.add(new Paragraph("No transactions in this period.", font));
      return;
    }

    PdfPTable table = new PdfPTable(new float[]{1.5f, 3f, 1.2f, 1.2f, 1.5f});
    table.setWidthPercentage(100);
    table.setHeaderRows(1);

    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NEUTRAL_0);
    String[] headers = {"Booking Date", "Description", "Credit", "Debit", "Available Balance"};

    for (String h : headers) {
      PdfPCell hc = new PdfPCell(new Phrase(h, headerFont));
      hc.setBackgroundColor(PRIMARY_600);
      hc.setBorder(Rectangle.NO_BORDER);
      hc.setPadding(8);
      if (h.equals("Credit") || h.equals("Debit") || h.equals("Available Balance")) {
        hc.setHorizontalAlignment(Element.ALIGN_RIGHT);
      }
      table.addCell(hc);
    }

    Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, NEUTRAL_700);
    Font secRowFont = FontFactory.getFont(FontFactory.HELVETICA, 8, NEUTRAL_500);
    Font creditFont = FontFactory.getFont(FontFactory.HELVETICA, 9, SUCCESS_GREEN);
    Font debitFont = FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_600);
    Font balFont = FontFactory.getFont(FontFactory.HELVETICA, 9, NEUTRAL_800);

    DecimalFormat df = new DecimalFormat("#,##0.00");
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    for (StatementData.StatementTransactionData t : data.getTransactions()) {
      PdfPCell dateCell = getCell(t.getDateTime().format(dtf), rowFont, Element.ALIGN_LEFT);
      table.addCell(dateCell);

      PdfPCell descCell = new PdfPCell();
      descCell.setBorder(Rectangle.BOTTOM);
      descCell.setBorderColorBottom(NEUTRAL_200);
      descCell.setBorderWidthBottom(0.5f);
      descCell.setPadding(8);

      String mainDesc =
          t.getCounterparty() != null && !t.getCounterparty().isEmpty() ? t.getCounterparty()
              : t.getType();
      descCell.addElement(new Paragraph(mainDesc, rowFont));
      Paragraph ref = new Paragraph(t.getReference(), secRowFont);
      ref.setSpacingBefore(2f);
      descCell.addElement(ref);
      table.addCell(descCell);

      BigDecimal amt = t.getAmount();
      if (amt.compareTo(BigDecimal.ZERO) >= 0) {
        table.addCell(getCell("+" + df.format(amt), creditFont, Element.ALIGN_RIGHT));
        table.addCell(getCell("", rowFont, Element.ALIGN_RIGHT));
      } else {
        table.addCell(getCell("", rowFont, Element.ALIGN_RIGHT));
        table.addCell(getCell("-" + df.format(amt.negate()), debitFont, Element.ALIGN_RIGHT));
      }

      table.addCell(getCell(df.format(t.getRunningBalance()), balFont, Element.ALIGN_RIGHT));
    }

    document.add(table);
  }

  private PdfPCell getCell(String text, Font font, int alignment) {
    PdfPCell c = new PdfPCell(new Phrase(text, font));
    c.setBorder(Rectangle.BOTTOM);
    c.setBorderColorBottom(NEUTRAL_200);
    c.setBorderWidthBottom(0.5f);
    c.setPadding(8);
    c.setHorizontalAlignment(alignment);
    return c;
  }

  private static class FooterEvent extends PdfPageEventHelper {

    private final StatementData data;

    public FooterEvent(StatementData data) {
      this.data = data;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfPTable footer = new PdfPTable(2);
      footer.setTotalWidth(document.right() - document.left());

      PdfPCell lineCell = new PdfPCell(new Phrase(""));
      lineCell.setBorder(Rectangle.TOP);
      lineCell.setBorderColorTop(NEUTRAL_200);
      lineCell.setBorderWidthTop(0.5f);
      lineCell.setColspan(2);
      footer.addCell(lineCell);

      Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 8, NEUTRAL_500);
      Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA, 8, NEUTRAL_700);

      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");
      String generatedAt = data.getGenerationTimestamp().format(dtf);

      PdfPCell left = new PdfPCell(new Phrase("Generated at " + generatedAt, labelFont));
      left.setBorder(Rectangle.NO_BORDER);
      left.setPaddingTop(5);

      Chunk badgeChunk = new Chunk(" " + writer.getPageNumber() + " ", badgeFont);
      badgeChunk.setBackground(NEUTRAL_100);
      Paragraph badgePara = new Paragraph();
      badgePara.add(new Chunk("Page ", labelFont));
      badgePara.add(badgeChunk);

      PdfPCell badgeCell = new PdfPCell(badgePara);
      badgeCell.setBorder(Rectangle.NO_BORDER);
      badgeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
      badgeCell.setPaddingTop(5);

      footer.addCell(left);
      footer.addCell(badgeCell);

      footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10,
          writer.getDirectContent());
    }
  }
}

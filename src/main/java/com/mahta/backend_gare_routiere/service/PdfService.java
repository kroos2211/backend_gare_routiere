package com.mahta.backend_gare_routiere.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.entity.Stop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final QrSecurityService qrSecurityService;

    private static final DeviceRgb PRIMARY = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb DARK = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(245, 248, 252);
    private static final DeviceRgb MUTED = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb SUCCESS = new DeviceRgb(22, 163, 74);

    public byte[] generateTicket(Booking booking) {
        try {
            ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(pdfOutput);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            DateTimeFormatter dateFormatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            String boardingCity = resolveBoardingCity(booking);
            String dropoffCity = resolveDropoffCity(booking);

            LocalDateTime boardingTime = resolveDepartureTime(booking);
            LocalDateTime dropoffTime = resolveArrivalTime(booking);

            Table header = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
            header.setWidth(UnitValue.createPercentValue(100));

            Cell brandCell = new Cell()
                    .setBorder(null)
                    .setBackgroundColor(DARK)
                    .setPadding(20);

            brandCell.add(new Paragraph("Ma7ta.ma")
                    .setBold()
                    .setFontSize(28)
                    .setFontColor(ColorConstants.WHITE));

            brandCell.add(new Paragraph("Ticket officiel de voyage")
                    .setFontSize(12)
                    .setFontColor(new DeviceRgb(203, 213, 225)));

            Cell statusCell = new Cell()
                    .setBorder(null)
                    .setBackgroundColor(DARK)
                    .setPadding(20)
                    .setTextAlignment(TextAlignment.RIGHT);

            statusCell.add(new Paragraph("STATUT")
                    .setFontSize(10)
                    .setFontColor(new DeviceRgb(148, 163, 184)));

            statusCell.add(new Paragraph(booking.getStatus().name())
                    .setBold()
                    .setFontSize(18)
                    .setFontColor(SUCCESS));

            header.addCell(brandCell);
            header.addCell(statusCell);

            document.add(header);
            document.add(new Paragraph("\n"));

            Table routeCard = new Table(UnitValue.createPercentArray(new float[]{1, 0.25f, 1}));
            routeCard.setWidth(UnitValue.createPercentValue(100));
            routeCard.setBackgroundColor(LIGHT_BG);
            routeCard.setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1));

            Cell fromCell = new Cell()
                    .setBorder(null)
                    .setPadding(22);

            fromCell.add(new Paragraph("DÉPART")
                    .setFontSize(10)
                    .setBold()
                    .setFontColor(MUTED));

            fromCell.add(new Paragraph(boardingCity)
                    .setFontSize(26)
                    .setBold()
                    .setFontColor(DARK));

            fromCell.add(new Paragraph(boardingTime.format(dateFormatter))
                    .setFontSize(11)
                    .setFontColor(MUTED));

            Cell arrowCell = new Cell()
                    .setBorder(null)
                    .setPadding(22)
                    .setTextAlignment(TextAlignment.CENTER);

            arrowCell.add(new Paragraph("→")
                    .setFontSize(32)
                    .setBold()
                    .setFontColor(PRIMARY));

            Cell toCell = new Cell()
                    .setBorder(null)
                    .setPadding(22)
                    .setTextAlignment(TextAlignment.RIGHT);

            toCell.add(new Paragraph("ARRIVÉE")
                    .setFontSize(10)
                    .setBold()
                    .setFontColor(MUTED));

            toCell.add(new Paragraph(dropoffCity)
                    .setFontSize(26)
                    .setBold()
                    .setFontColor(DARK));

            toCell.add(new Paragraph(dropoffTime.format(dateFormatter))
                    .setFontSize(11)
                    .setFontColor(MUTED));

            routeCard.addCell(fromCell);
            routeCard.addCell(arrowCell);
            routeCard.addCell(toCell);

            document.add(routeCard);
            document.add(new Paragraph("\n"));

            String data = "BOOKING_ID:" + booking.getId();
            String signature = qrSecurityService.generateSignature(data);
            String qrContent = data + "|SIGN:" + signature;

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(qrContent, BarcodeFormat.QR_CODE, 220, 220);

            ByteArrayOutputStream qrOutput = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", qrOutput);

            Image qrImage = new Image(
                    ImageDataFactory.create(qrOutput.toByteArray())
            ).setWidth(150).setHeight(150);

            Table mainInfo = new Table(UnitValue.createPercentArray(new float[]{1.4f, 0.8f}));
            mainInfo.setWidth(UnitValue.createPercentValue(100));

            Cell infoCell = new Cell()
                    .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                    .setPadding(20);

            infoCell.add(new Paragraph("Informations de réservation")
                    .setBold()
                    .setFontSize(17)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Réservation : #" + booking.getId())
                    .setMarginTop(12)
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Client : " + booking.getUser().getEmail())
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Montée : " + boardingCity)
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Descente : " + dropoffCity)
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Heure montée : " + boardingTime.format(dateFormatter))
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Heure descente : " + dropoffTime.format(dateFormatter))
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Catégorie : " + booking.getTariffCategory().name())
                    .setFontSize(12)
                    .setFontColor(DARK));

            infoCell.add(new Paragraph("Prix total : " + String.format("%.2f", booking.getTotalPrice()) + " MAD")
                    .setBold()
                    .setFontSize(16)
                    .setFontColor(PRIMARY));

            infoCell.add(new Paragraph("Créé le : " + String.valueOf(booking.getCreatedAt()))
                    .setFontSize(12)
                    .setFontColor(MUTED));

            Cell qrCell = new Cell()
                    .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                    .setPadding(20)
                    .setTextAlignment(TextAlignment.CENTER);

            qrCell.add(new Paragraph("QR Code")
                    .setBold()
                    .setFontSize(15)
                    .setFontColor(DARK));

            qrCell.add(qrImage);

            qrCell.add(new Paragraph("À présenter lors du départ")
                    .setFontSize(10)
                    .setFontColor(MUTED));

            mainInfo.addCell(infoCell);
            mainInfo.addCell(qrCell);

            document.add(mainInfo);
            document.add(new Paragraph("\n"));

            Table options = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}));
            options.setWidth(UnitValue.createPercentValue(100));

            options.addCell(optionCell("PMR", booking.isPrioritySeat() ? "Oui" : "Non"));
            options.addCell(optionCell("Bébé", booking.isBaby() ? "Oui" : "Non"));
            options.addCell(optionCell("Sécurité", "QR signé"));

            document.add(options);
            document.add(new Paragraph("\n"));

            SolidLine line = new SolidLine();
            line.setColor(new DeviceRgb(226, 232, 240));

            document.add(new LineSeparator(line));

            Paragraph footer = new Paragraph(
                    "Ce ticket est généré automatiquement par Ma7ta.ma. " +
                            "Le QR code est sécurisé et vérifiable une seule fois."
            )
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setFontColor(MUTED)
                    .setMarginTop(16);

            document.add(footer);

            document.close();

            return pdfOutput.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating ticket PDF", e);
        }
    }

    private String resolveBoardingCity(Booking booking) {
        if (booking.getBoardingCity() != null && !booking.getBoardingCity().isBlank()) {
            return booking.getBoardingCity();
        }

        return booking.getTrip().getDepartureCity();
    }

    private String resolveDropoffCity(Booking booking) {
        if (booking.getDropoffCity() != null && !booking.getDropoffCity().isBlank()) {
            return booking.getDropoffCity();
        }

        return booking.getTrip().getArrivalCity();
    }

    private LocalDateTime resolveDepartureTime(Booking booking) {
        String boardingCity = resolveBoardingCity(booking);

        if (boardingCity.equals(booking.getTrip().getDepartureCity())) {
            return booking.getTrip().getDepartureTime();
        }

        return booking.getTrip()
                .getStops()
                .stream()
                .filter(stop -> stop.getCity().equals(boardingCity))
                .findFirst()
                .map(Stop::getScheduledTime)
                .orElse(booking.getTrip().getDepartureTime());
    }

    private LocalDateTime resolveArrivalTime(Booking booking) {
        String dropoffCity = resolveDropoffCity(booking);

        if (dropoffCity.equals(booking.getTrip().getArrivalCity())) {
            return booking.getTrip().getArrivalTime();
        }

        return booking.getTrip()
                .getStops()
                .stream()
                .filter(stop -> stop.getCity().equals(dropoffCity))
                .findFirst()
                .map(Stop::getScheduledTime)
                .orElse(booking.getTrip().getArrivalTime());
    }

    private Cell optionCell(String label, String value) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                .setBackgroundColor(LIGHT_BG)
                .setPadding(14)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(label)
                .setFontSize(10)
                .setBold()
                .setFontColor(MUTED));

        cell.add(new Paragraph(value)
                .setFontSize(14)
                .setBold()
                .setFontColor(DARK));

        return cell;
    }
}
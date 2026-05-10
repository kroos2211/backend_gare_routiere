package com.mahta.backend_gare_routiere.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class QrCodeService {

    private static final int QR_SIZE = 200;

    public BufferedImage generateQRCode(String text) throws WriterException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        var bitMatrix = qrCodeWriter.encode(
                text,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE
        );

        BufferedImage image = new BufferedImage(
                QR_SIZE,
                QR_SIZE,
                BufferedImage.TYPE_INT_RGB
        );

        for (int x = 0; x < QR_SIZE; x++) {
            for (int y = 0; y < QR_SIZE; y++) {
                image.setRGB(
                        x,
                        y,
                        bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF
                );
            }
        }

        return image;
    }
}
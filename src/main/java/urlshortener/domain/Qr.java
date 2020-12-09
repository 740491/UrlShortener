package urlshortener.domain;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Qr {
    private String hash;
    private byte[] qrByteArray;

    public Qr(String hash, byte[] qrByteArray) {
        this.hash = hash;
        this.qrByteArray = qrByteArray;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public byte[] getQrByteArray() {
        return qrByteArray;
    }

    public void setQrByteArray(byte[] qrByteArray) {
        this.qrByteArray = qrByteArray;
    }

    /*
        This method takes the text to be encoded, the width and height of the QR Code,
        and returns the QR Code in the form of a byte array.
        */
    public byte[] getQRCodeImage(String url, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }
}

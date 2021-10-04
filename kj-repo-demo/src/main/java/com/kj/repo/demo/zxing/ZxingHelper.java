package com.kj.repo.demo.zxing;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kj.repo.infra.utils.RunUtil;

/**
 * @author kj
 */
public class ZxingHelper {

    private static final MultiFormatWriter WRITER = new MultiFormatWriter();

    public static byte[] encode(String data) {
        return RunUtil.run(() -> encodeInternal(data), ZxingHelper.class.getName(), "encodeInternal");
    }

    public static String decode(byte[] data) {
        return RunUtil.run(() -> decodeInternal(data), ZxingHelper.class.getName(), "decodeInternal");
    }

    public static byte[] encodeInternal(String data) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = Maps.newHashMap();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = WRITER.encode(data, BarcodeFormat.QR_CODE, 300, 300, hints);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        MatrixToImageWriter.writeToStream(bitMatrix, "jpg", bos);
        bos.close();
        return baos.toByteArray();
    }

    public static String decodeInternal(byte[] data) throws IOException, NotFoundException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        Binarizer binarizer = new HybridBinarizer(source);
        BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
        Map<DecodeHintType, Object> hints = Maps.newHashMap();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        Result result = new MultiFormatReader().decode(binaryBitmap, hints);
        return result.getText();
    }
}

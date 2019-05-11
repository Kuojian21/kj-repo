package com.kj.repo.infra.helper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 
 * @author kuojian21
 *
 */
public class ZxingHelper {

	private static Logger logger = LoggerFactory.getLogger(ZxingHelper.class);
	private static final MultiFormatWriter WRITER = new MultiFormatWriter();

	public static byte[] matrix(String content) {
		try {
			Map<EncodeHintType, Object> hints = Maps.newHashMap();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
			hints.put(EncodeHintType.MARGIN, 1);
			BitMatrix bitMatrix = WRITER.encode(content, BarcodeFormat.QR_CODE, 300, 300, hints);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(baos);
			MatrixToImageWriter.writeToStream(bitMatrix, "jpg", bos);
			bos.close();
			return baos.toByteArray();
		} catch (WriterException | IOException e) {
			logger.error("", e);
			return null;
		}
	}

}

package com.google.zxing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.Deflater;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

public class Program {

  /**
   * @param args
   * @throws IOException 
   * @throws FileNotFoundException 
   * @throws WriterException 
   */
  public static void main(String[] args) throws Exception {
    if(args.length != 4 && args.length != 5) {
      System.out.println("Usage: QRCodeGenerator.jar <file> <error_correction_level> <image_width_px> <quiet_zone_px>");
      System.out.println("  <error_correction_level> must be L, M, Q or H");
      System.out.println();
      System.out.println("Example: QRCodeGenerator.jar \"test.bin\" M 512 16");
      return;
    }
    ErrorCorrectionLevel level;
    switch(args[1].toUpperCase().charAt(0)) {
    case 'L':
      level = ErrorCorrectionLevel.L;
      break;
    case 'M':
      level = ErrorCorrectionLevel.M;
      break;
    case 'Q':
      level = ErrorCorrectionLevel.Q;
      break;
    case 'H':
      level = ErrorCorrectionLevel.H;
      break;
    default:
      System.out.println("ERROR: invalid error_correction_level. Must be L, M, Q or H.");
      return;
    }
    int width = Integer.parseInt(args[2]);
    int quietZone = Integer.parseInt(args[3]);
    boolean deflate = false;
    if(args.length == 5) {
      switch (args[4].toUpperCase().charAt(0)) {
      case 'Y':
        deflate = true;
        break;
      case 'N':
        deflate = false;
        break;
      default:
        System.out.println("ERROR: invalid deflate argument. Must be Y or N."); 
        return;
      }
    }
    
    FileChannel channel = new FileInputStream(args[0]).getChannel();
    MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
    int length = buffer.remaining();
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    if(deflate) {
      byte[] tmp = new byte[length];
      Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
      deflater.setInput(bytes);
      deflater.finish();
      length = deflater.deflate(tmp);
      bytes = tmp;
    }
    QRCode qrCode = new QRCode();
    Encoder.encode(bytes, 0, length, level, qrCode);
    BitMatrix result = renderResult(qrCode, width, width, quietZone);
    MatrixToImageWriter.writeToFile(result, "png", new File(args[0] + ".png"));
  }
  
  private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
    ByteMatrix input = code.getMatrix();
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = inputWidth + quietZone * 2;
    int qrHeight = inputHeight + quietZone * 2;
    int outputWidth = Math.max(width, qrWidth);
    int outputHeight = Math.max(height, qrHeight);

    int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
    // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
    // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
    // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
    // handle all the padding from 100x100 (the actual QR) up to 200x160.
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    BitMatrix output = new BitMatrix(outputWidth, outputHeight);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the barcode
      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY) == 1) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }

    return output;
  }
}

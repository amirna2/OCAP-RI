 // COPYRIGHT_BEGIN
 //  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 //  
 //  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
 //  
 //  This software is available under multiple licenses: 
 //  
 //  (1) BSD 2-clause 
 //   Redistribution and use in source and binary forms, with or without modification, are
 //   permitted provided that the following conditions are met:
 //        ·Redistributions of source code must retain the above copyright notice, this list 
 //             of conditions and the following disclaimer.
 //        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 //             and the following disclaimer in the documentation and/or other materials provided with the 
 //             distribution.
 //   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 //   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 //   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 //   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 //   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 //   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 //   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 //   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 //   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 //   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 //   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 //  
 //  (2) GPL Version 2
 //   This program is free software; you can redistribute it and/or modify
 //   it under the terms of the GNU General Public License as published by
 //   the Free Software Foundation, version 2. This program is distributed
 //   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 //   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 //   PURPOSE. See the GNU General Public License for more details.
 //  
 //   You should have received a copy of the GNU General Public License along
 //   with this program.If not, see<http:www.gnu.org/licenses/>.
 //  
 //  (3)CableLabs License
 //   If you or the company you represent has a separate agreement with CableLabs
 //   concerning the use of this code, your rights and obligations with respect
 //   to this code shall be as set forth therein. No license is granted hereunder
 //   for any other purpose.
 //  
 //   Please contact CableLabs if you need additional information or 
 //   have any questions.
 //  
 //       CableLabs
 //       858 Coal Creek Cir
 //       Louisville, CO 80027-9750
 //       303 661-9100
 // COPYRIGHT_END
 

package com.tvworks.plateval.vq;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.dvb.dsmcc.DSMCCObject;

import com.tvworks.plateval.TestCaseSet;
import com.tvworks.plateval.framework.TestCase;
import com.tvworks.plateval.util.ResultLog;

import com.tvworks.plateval.format.PlatformInformationRepository;

public class VQDecode implements TestCase
{
   public static void touch()
   {      
   }

   private String vqImageNames[]= 
   { 
       "images/lena.vq",
       "images/Hyundai.vq",
       "images/Shirland.vq",
       "images/ThisWeek.vq"         
   };
      
   public void run()
   {
      long startTime, endTime, runTime;
      int i, imageIdx, result, decodeCount;
      VQImage vqImage[];
      
      vqImage= new VQImage[vqImageNames.length];
      for( imageIdx= 0; imageIdx < vqImageNames.length; ++imageIdx )
      {
         vqImage[imageIdx]= new VQImage( vqImageNames[imageIdx] );
      }
      
      result= 0;
      runTime= 0;
      decodeCount= 0;
      for( i= 0; i < 10; ++i )
      {
         for( imageIdx= 0; imageIdx < vqImageNames.length; ++imageIdx )
         {
            VQImage vq1= vqImage[imageIdx];
            MyImageConsumer c1= new MyImageConsumer(true);

            startTime= System.currentTimeMillis();
            
            vq1.startProduction(c1);

            endTime= System.currentTimeMillis();
            runTime += (endTime-startTime);
 
            result <<= 1;
            if ( (result & 0x80000000) != 0 )
            {
               result ^= 0x800C1003;
            }
            result ^= c1.getHash();
            
            ++decodeCount;
         }
      }

      /*
       * For this set of test images and loop count, the result hash should be 0x3f1a0bf5
       */
      ResultLog.getInstance().add( TestCaseSet.TEST_VQDecode, result, decodeCount, runTime );
   }
   
   public static void format( PlatformInformationRepository pir, StringBuffer sb, int data1, int data2, long data3 )
   {
      sb.append( "  VQ decode time for "+data2+" images: "+data3+" ms, hash= 0x"+Integer.toHexString(data1)+"\r\n" );
   }
   
   private static class VQImage implements ImageProducer
   {
      private static final boolean trace = false;
      private static final boolean traceDecode= false;
      private static final int VQFILEHDR_SIGNATURE = 0x4344;
      private static final int VQFILEHDR_FORMAT_SA = 0x0005;
      private static final int VQIMGHDR_SIGNATURE = 0x56515341;
      private static final int VQTILECOMPRESS_NONE = 0;
      private static final int VQTILECOMPRESS_LZSS = 1;
      private static final int LZSS_WINDOW_SIZE = 4096;
      private static final int LZSS_MinMatchLen = 2;
      private static final int LZSS_MaxMatchLen = 18;
      private Vector consumers;
      private String file;
      private byte vqdata[];
      private byte CLUT[];
      private ColorModel cm;
      private boolean isError;
      private int width;
      private int height;
      private int bitsPerPixel;
      private boolean isIndexed;
      private int numCLUTEntries;
      private int clutOffset;
      private int numberOfTiles;
      private int tileWidth;
      private int tileHeight;
      private int firstTileOffset;
      private int imageDataOffset;
      private int vqFormatVersion;
      private int tileIndexCompression;
      private int numberOfImageBands;
      private int tileRowsPerBand;
      private int bandTileCountsOffset;
      private int horizontalResolution;
      private int verticalResolution;
      private int transparentColor;

      public VQImage(byte vqdata[])
      {
         consumers = new Vector();
         this.file = null;
         this.vqdata = vqdata;
      }

      public VQImage(String file)
      {
         consumers = new Vector();
         this.file = file;
         this.vqdata = readFile();
      }

      public void addConsumer(ImageConsumer c)
      {
         if (trace) System.out.println("VQImage.addConsumer: " + c);
         consumers.addElement(c);
      }

      public boolean isConsumer(ImageConsumer c)
      {
         if (trace) System.out.println("VQImage.isConsumer: " + c + " : " + consumers.contains(c));
         return consumers.contains(c);
      }

      public void removeConsumer(ImageConsumer c)
      {
         if (trace) System.out.println("VQImage.removeConsumer: " + c);
         consumers.removeElement(c);
      }

      public void startProduction(ImageConsumer c)
      {
         if (trace) System.out.println("VQImage.startProduction: " + c);
         
         if ( c == null ) return;

         if ( vqdata == null ) return;
         
         parseData();

         if (isError)
         {
            c.imageComplete(ImageConsumer.IMAGEERROR);
         }
         else
         {
            c.setColorModel(cm);
            c.setDimensions(width, height);
            c.setHints(ImageConsumer.SINGLEPASS | ImageConsumer.TOPDOWNLEFTRIGHT);

            decode(c);
            if (isError)
            {
               c.imageComplete(ImageConsumer.IMAGEERROR);
            }
            else
            {
               c.imageComplete(ImageConsumer.STATICIMAGEDONE);
            }
         }
      }

      public void requestTopDownLeftRightResend(ImageConsumer c)
      {
         if (trace) System.out.println("VQImage.requestTopDownLeftRightResent: " + c);

         startProduction(c);
      }

      private InputStream getFileStream( String name )
      {
         InputStream is= null;
         
         // ---------------------------------------------
         // TVNav / OCAP 
         // ---------------------------------------------
         
         try
         {
            is= new FileInputStream( new File(name) );
         }
         catch( IOException ioe )
         {
            System.out.println("Error: IOException opening VQ file");            
         }
         
         
         // ---------------------------------------------
         // Zodiac
         // ---------------------------------------------
         /*
         try
         {
            is= com.tvworks.plateval.PlatEval.class.getResourceAsStream("/"+name );
         }
         catch( Throwable t )
         {
            System.out.println("Error: Exception accessing VQ file resource");   
            t.printStackTrace();
         }
         */
         
         return is;
      }
      
      private byte[] readFile()
      {
         try
         {
            InputStream is;
            int lenVQ, lenRead, offset;

            is= getFileStream(file);
            if ( is == null ) return null;
            
            lenVQ = (int)80000;
            if (trace) System.out.println("vq file (" + file + "): len " + lenVQ);

            byte[] data = new byte[lenVQ];
            offset= 0;
            for( ; ; )
            {               
               lenRead = is.read(data, offset, (lenVQ-offset));
               if (trace) System.out.println("readFile: offset="+offset+" len req="+(lenVQ-offset)+" len read="+lenRead );
               if ( lenRead > 0 )
               {                  
                  offset += lenRead;
               }
               else
               {
                  break;
               }
            }

            if (offset > 0)
            {
               byte[] datanew= new byte[offset];
               System.arraycopy( data, 0, datanew, 0, offset );
               data= datanew;
               
               return data;
            }
         }
         catch (IOException ioe)
         {
            System.out.println("Error: IOException processing VQ file");
         }

         return null;
      }
      
      private void parseData()
      {
         int parseOffset, value, index;

         // Parse file header
         parseOffset = 0;

         // Check file header signature
         value = getU16(vqdata, parseOffset + 0);
         if (value != VQFILEHDR_SIGNATURE)
         {
            isError = true;
            System.out.println("parseData: bad VQ file header signature");
            return;
         }

         // Check the file header image format
         value = getU16(vqdata, parseOffset + 2);
         if (value != VQFILEHDR_FORMAT_SA)
         {
            isError = true;
            System.out.println("parseData: bad VQ format");
            return;
         }

         // Advance to image header
         parseOffset += 8;

         // Check the image header signature
         value = (int) getU32(vqdata, parseOffset + 0);
         if (value != VQIMGHDR_SIGNATURE)
         {
            isError = true;
            System.out.println("parseData: bad VQ image signature");
            return;
         }

         // Get image width and height
         width = getU16(vqdata, parseOffset + 4);
         height = getU16(vqdata, parseOffset + 6);

         // Get image bits per pixel
         bitsPerPixel = getU16(vqdata, parseOffset + 10);
         switch (bitsPerPixel)
         {
            case 4:
            case 8:
               isIndexed = true;
               break;

            case 16:
            case 24:
               isIndexed = false;
               break;

            default:
               isError = true;
               System.out.println("parseData: bad VQ image bits per pixel");
               return;
         }
         if (trace) System.out.println("parseData: image: " + width + "x" + height + "x" + bitsPerPixel);

         // Read CLUT if necessary
         if (isIndexed)
         {
            numCLUTEntries = getU16(vqdata, parseOffset + 12);
            clutOffset = (int) getU32(vqdata, parseOffset + 14);

            CLUT = new byte[numCLUTEntries * 4];

            System.arraycopy(vqdata, clutOffset + 1, CLUT, 0, numCLUTEntries * 4);

            // Mark all CLUT entries as opaque
            for (index = 0; index < numCLUTEntries; ++index)
            {
               CLUT[index * 4 + 3] = (byte) 0xFF;
            }
         }

         // Create color model
         switch (bitsPerPixel)
         {
            case 4:
               cm = new IndexColorModel(4, 16, CLUT, 0, true, 0);
               break;

            case 8:
               cm = new IndexColorModel(8, 256, CLUT, 0, true, 0);
               break;

            case 16:
               cm = new DirectColorModel(16, 0xF800, 0x07E0, 0x001F);
               break;

            case 24:
               cm = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
               break;
         }

         // Get tile parameters
         numberOfTiles = getU16(vqdata, parseOffset + 18);
         tileWidth = getU16(vqdata, parseOffset + 20);
         tileHeight = getU16(vqdata, parseOffset + 22);
         firstTileOffset = (int) getU32(vqdata, parseOffset + 24);

         // Get image data offset
         imageDataOffset = (int) getU32(vqdata, parseOffset + 28);

         // Get format version
         vqFormatVersion = getU16(vqdata, parseOffset + 32);
         if (vqFormatVersion < 0x0400)
         {
            isError = true;
            System.out.println("parseData: bad VQ format version: " + vqFormatVersion);
            return;
         }

         // Get tile index compression format
         tileIndexCompression = getU16(vqdata, parseOffset + 34);
         switch (tileIndexCompression)
         {
            case VQTILECOMPRESS_NONE:
            case VQTILECOMPRESS_LZSS:
               break;
            default:
               isError = true;
               System.out.println("parseData: bad VQ tile compression: " + tileIndexCompression);
               return;
         }

         // Get number of image bands
         numberOfImageBands = getU16(vqdata, parseOffset + 36);

         // Get the number of tile rows per image band
         tileRowsPerBand = getU16(vqdata, parseOffset + 38);

         // Get offset of the band tile count table
         bandTileCountsOffset = (int) getU32(vqdata, parseOffset + 40);

         // Get image resolution
         horizontalResolution = getU16(vqdata, parseOffset + 44);
         verticalResolution = getU16(vqdata, parseOffset + 46);

         // Get transparent color
         transparentColor = (int) getU32(vqdata, parseOffset + 48);
      }

      private void decode(ImageConsumer c)
      {
         switch (bitsPerPixel)
         {
            case 4:
               decode4(c);
               break;

            case 8:
               decode8(c);
               break;

            case 16:
               decode16(c);
               break;

            case 24:
               decode24(c);
               break;
         }

      }

      private void decode4(ImageConsumer c)
      {
         int row, col;
         int windowMin, windowMax, mask, inhibits, seqLen, seqIndex, winIndex;
         int tilesPerCol, tilesPerRow, bytesPerLineIn, bytesPerLineOut, rowCount, colCount;
         int tileIndexBase, firstRowNextBand, bandIndex, tileRowsLastBand;
         int tileIndex, tileRowIndex, byteIndex;
         int bytesPerTileRow, tileByteWidthIn, tileByteWidthOut, tileByteSize;
         int tileIndexOffset, tileBaseOffset, tileStartOffset;
         int tileOffset, destOffset, pixel;
         int bandTileCountOffset, bmRowTopOffset;
         byte decodedRow[];
         byte LZSSWindow[];

         /*
          * Setup tile index base. This will be used to shift the tile indexes to
          * the proper range as we move from band to band of the encoded image.
          */
         tileIndexBase = 0;
         bandIndex = 0;
         firstRowNextBand = tileRowsPerBand;

         /*
          * If image is banded setup access to the codebook band tile count table
          * and calculate the number of tile rows in the last band
          */
         if (numberOfImageBands > 1)
         {
            tileRowsLastBand = ((height + tileHeight - 1) / tileHeight) - ((numberOfImageBands - 1) * tileRowsPerBand);
         }
         else
         {
            tileRowsLastBand = tileRowsPerBand;
         }

         tilesPerCol = (height + tileHeight - 1) / tileHeight;
         tilesPerRow = (width + tileWidth - 1) / tileWidth;
         bytesPerLineIn = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthIn = ((tileWidth * bitsPerPixel) >> 3);
         bytesPerLineOut = tilesPerRow * tileWidth;
         tileByteWidthOut = tileWidth;

         decodedRow = new byte[bytesPerLineOut * tileHeight];

         rowCount = tilesPerCol;
         colCount = tilesPerRow;
         bytesPerTileRow = bytesPerLineIn * tileHeight;
         tileByteSize = ((tileWidth * tileHeight * bitsPerPixel) >> 3);
         tileIndexOffset = imageDataOffset;
         tileBaseOffset = firstTileOffset;
         bandTileCountOffset = bandTileCountsOffset;
         bmRowTopOffset = 0;

         /*
          * If there is no tile index compression use this faster code
          */
         if (tileIndexCompression == VQTILECOMPRESS_NONE)
         {
            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  tileIndex = (vqdata[tileIndexOffset++] & 0xFF);

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;
                  while (tileRowIndex-- > 0)
                  {
                     byteIndex = tileByteWidthIn;
                     while (byteIndex-- > 0)
                     {
                        pixel = vqdata[tileOffset++];
                        decodedRow[destOffset++] = (byte) ((pixel >> 4) & 0xF);
                        decodedRow[destOffset++] = (byte) ((pixel) & 0xF);
                     }

                     destOffset += bytesPerLineOut - tileByteWidthOut;
                  }
                  tileStartOffset += tileByteWidthOut;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, bytesPerLineOut);
            }
         }
         /*
          * We have tile index compression. Use this code.
          */
         else
         {
            /*
             * Perform tile index decompression setup
             */
            LZSSWindow = new byte[LZSS_WINDOW_SIZE];
            windowMin = 0;
            windowMax = LZSS_WINDOW_SIZE;
            mask = 0x100;
            seqLen = 0;
            seqIndex = 0;
            winIndex = LZSS_WINDOW_SIZE - LZSS_MaxMatchLen;
            inhibits = 0;

            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  if (seqLen > 0)
                  {
                     tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                     if (seqIndex >= windowMax) seqIndex = windowMin;
                     --seqLen;
                  }
                  else
                  {
                     if (mask >= 0x100)
                     {
                        mask = 0x1;
                        inhibits = (vqdata[tileIndexOffset++] & 0xFF);
                     }

                     if ((inhibits & mask) != 0)
                     {
                        tileIndex = (vqdata[tileIndexOffset++] & 0xFF);
                     }
                     else
                     {
                        seqIndex = (vqdata[tileIndexOffset++] & 0xFF);
                        seqIndex |= ((vqdata[tileIndexOffset] & 0xF0) << 4);
                        seqLen = (vqdata[tileIndexOffset++] & 0x0F) + LZSS_MinMatchLen;

                        tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                        if (seqIndex >= windowMax) seqIndex = windowMin;
                     }
                     mask <<= 1;
                  }
                  LZSSWindow[winIndex++] = (byte) tileIndex;
                  if (winIndex >= windowMax) winIndex = windowMin;

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;

                  while (tileRowIndex-- > 0)
                  {
                     byteIndex = tileByteWidthIn;
                     while (byteIndex-- > 0)
                     {
                        pixel = vqdata[tileOffset++];
                        decodedRow[destOffset++] = (byte) ((pixel >> 4) & 0xF);
                        decodedRow[destOffset++] = (byte) ((pixel) & 0xF);
                     }

                     destOffset += (bytesPerLineOut - tileByteWidthOut);
                  }
                  tileStartOffset += tileByteWidthOut;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, bytesPerLineOut);
            }
         }
      }

      private void decode8(ImageConsumer c)
      {
         int row, col;
         int windowMin, windowMax, mask, inhibits, seqLen, seqIndex, winIndex;
         int tilesPerCol, tilesPerRow, bytesPerLineIn, bytesPerLineOut, rowCount, colCount;
         int tileIndexBase, firstRowNextBand, bandIndex, tileRowsLastBand;
         int tileIndex, tileRowIndex, tileColIndex;
         int bytesPerTileRow, tileByteWidthIn, tileByteWidthOut, tileByteSize;
         int tileIndexOffset, tileBaseOffset, tileStartOffset;
         int tileOffset, destOffset, pixel;
         int bandTileCountOffset, bmRowTopOffset;
         byte decodedRow[];
         byte LZSSWindow[];

         /*
          * Setup tile index base. This will be used to shift the tile indexes to
          * the proper range as we move from band to band of the encoded image.
          */
         tileIndexBase = 0;
         bandIndex = 0;
         firstRowNextBand = tileRowsPerBand;

         /*
          * If image is banded setup access to the codebook band tile count table
          * and calculate the number of tile rows in the last band
          */
         if (numberOfImageBands > 1)
         {
            tileRowsLastBand = ((height + tileHeight - 1) / tileHeight) - ((numberOfImageBands - 1) * tileRowsPerBand);
         }
         else
         {
            tileRowsLastBand = tileRowsPerBand;
         }

         tilesPerCol = (height + tileHeight - 1) / tileHeight;
         tilesPerRow = (width + tileWidth - 1) / tileWidth;
         bytesPerLineIn = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthIn = ((tileWidth * bitsPerPixel) >> 3);
         bytesPerLineOut = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthOut = ((tileWidth * bitsPerPixel) >> 3);

         decodedRow = new byte[width * tileHeight];

         rowCount = tilesPerCol;
         colCount = tilesPerRow;
         bytesPerTileRow = bytesPerLineIn * tileHeight;
         tileByteSize = ((tileWidth * tileHeight * bitsPerPixel) >> 3);
         tileIndexOffset = imageDataOffset;
         tileBaseOffset = firstTileOffset;
         bandTileCountOffset = bandTileCountsOffset;
         bmRowTopOffset = 0;

         /*
          * If there is no tile index compression use this faster code
          */
         if (tileIndexCompression == VQTILECOMPRESS_NONE)
         {
            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  tileIndex = (vqdata[tileIndexOffset++] & 0xFF);

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;
                  while (tileRowIndex-- > 0)
                  {
                     tileColIndex = tileWidth;
                     while (tileColIndex-- > 0)
                     {
                        decodedRow[destOffset++] = vqdata[tileOffset++];
                     }

                     destOffset += (width - tileWidth);
                  }
                  tileStartOffset += tileWidth;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, width);
            }
         }
         /*
          * We have tile index compression. Use this code.
          */
         else
         {
            /*
             * Perform tile index decompression setup
             */
            LZSSWindow = new byte[LZSS_WINDOW_SIZE];
            windowMin = 0;
            windowMax = LZSS_WINDOW_SIZE;
            mask = 0x100;
            seqLen = 0;
            seqIndex = 0;
            winIndex = LZSS_WINDOW_SIZE - LZSS_MaxMatchLen;
            inhibits = 0;

            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  if (seqLen > 0)
                  {
                     tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                     if (seqIndex >= windowMax) seqIndex = windowMin;
                     --seqLen;
                  }
                  else
                  {
                     if (mask >= 0x100)
                     {
                        mask = 0x1;
                        inhibits = (vqdata[tileIndexOffset++] & 0xFF);
                     }

                     if ((inhibits & mask) != 0)
                     {
                        tileIndex = (vqdata[tileIndexOffset++] & 0xFF);
                     }
                     else
                     {
                        seqIndex = (vqdata[tileIndexOffset++] & 0xFF);
                        seqIndex |= ((vqdata[tileIndexOffset] & 0xF0) << 4);
                        seqLen = (vqdata[tileIndexOffset++] & 0x0F) + LZSS_MinMatchLen;

                        tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                        if (seqIndex >= windowMax) seqIndex = windowMin;
                     }
                     mask <<= 1;
                  }
                  LZSSWindow[winIndex++] = (byte) tileIndex;
                  if (winIndex >= windowMax) winIndex = windowMin;

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;

                  while (tileRowIndex-- > 0)
                  {
                     tileColIndex = tileWidth;
                     while (tileColIndex-- > 0)
                     {
                        decodedRow[destOffset++] = vqdata[tileOffset++];
                     }

                     destOffset += (width - tileWidth);
                  }
                  tileStartOffset += tileWidth;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, width);
            }
         }
      }

      private void decode16(ImageConsumer c)
      {
         int row, col;
         int windowMin, windowMax, mask, inhibits, seqLen, seqIndex, winIndex;
         int tilesPerCol, tilesPerRow, bytesPerLineIn, bytesPerLineOut, rowCount, colCount;
         int tileIndexBase, firstRowNextBand, bandIndex, tileRowsLastBand;
         int tileIndex, tileRowIndex, tileColIndex;
         int bytesPerTileRow, tileByteWidthIn, tileByteWidthOut, tileByteSize;
         int tileIndexOffset, tileBaseOffset, tileStartOffset;
         int tileOffset, destOffset, pixel;
         int bandTileCountOffset, bmRowTopOffset;
         int decodedRow[];
         byte LZSSWindow[];

         /*
          * Setup tile index base. This will be used to shift the tile indexes to
          * the proper range as we move from band to band of the encoded image.
          */
         tileIndexBase = 0;
         bandIndex = 0;
         firstRowNextBand = tileRowsPerBand;

         /*
          * If image is banded setup access to the codebook band tile count table
          * and calculate the number of tile rows in the last band
          */
         if (numberOfImageBands > 1)
         {
            tileRowsLastBand = ((height + tileHeight - 1) / tileHeight) - ((numberOfImageBands - 1) * tileRowsPerBand);
         }
         else
         {
            tileRowsLastBand = tileRowsPerBand;
         }

         tilesPerCol = (height + tileHeight - 1) / tileHeight;
         tilesPerRow = (width + tileWidth - 1) / tileWidth;
         bytesPerLineIn = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthIn = ((tileWidth * bitsPerPixel) >> 3);
         bytesPerLineOut = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthOut = ((tileWidth * bitsPerPixel) >> 3);

         decodedRow = new int[width * tileHeight];

         rowCount = tilesPerCol;
         colCount = tilesPerRow;
         bytesPerTileRow = bytesPerLineIn * tileHeight;
         tileByteSize = ((tileWidth * tileHeight * bitsPerPixel) >> 3);
         tileIndexOffset = imageDataOffset;
         tileBaseOffset = firstTileOffset;
         bandTileCountOffset = bandTileCountsOffset;
         bmRowTopOffset = 0;

         /*
          * If there is no tile index compression use this faster code
          */
         if (tileIndexCompression == VQTILECOMPRESS_NONE)
         {
            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  tileIndex = (vqdata[tileIndexOffset++] & 0xFF);

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;
                  while (tileRowIndex-- > 0)
                  {
                     tileColIndex = tileWidth;
                     while (tileColIndex-- > 0)
                     {
                        pixel = (vqdata[tileOffset++] << 8);
                        pixel |= ((vqdata[tileOffset++] & 0xFF));
                        decodedRow[destOffset++] = pixel;
                     }

                     destOffset += (width - tileWidth);
                  }
                  tileStartOffset += tileWidth;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, width);
            }
         }
         /*
          * We have tile index compression. Use this code.
          */
         else
         {
            /*
             * Perform tile index decompression setup
             */
            LZSSWindow = new byte[LZSS_WINDOW_SIZE];
            windowMin = 0;
            windowMax = LZSS_WINDOW_SIZE;
            mask = 0x100;
            seqLen = 0;
            seqIndex = 0;
            winIndex = LZSS_WINDOW_SIZE - LZSS_MaxMatchLen;
            inhibits = 0;

            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  if (seqLen > 0)
                  {
                     tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                     if (seqIndex >= windowMax) seqIndex = windowMin;
                     --seqLen;
                  }
                  else
                  {
                     if (mask >= 0x100)
                     {
                        mask = 0x1;
                        inhibits = (vqdata[tileIndexOffset++] & 0xFF);
                     }

                     if ((inhibits & mask) != 0)
                     {
                        tileIndex = (vqdata[tileIndexOffset++] & 0xFF);
                     }
                     else
                     {
                        seqIndex = (vqdata[tileIndexOffset++] & 0xFF);
                        seqIndex |= ((vqdata[tileIndexOffset] & 0xF0) << 4);
                        seqLen = (vqdata[tileIndexOffset++] & 0x0F) + LZSS_MinMatchLen;

                        tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                        if (seqIndex >= windowMax) seqIndex = windowMin;
                     }
                     mask <<= 1;
                  }
                  LZSSWindow[winIndex++] = (byte) tileIndex;
                  if (winIndex >= windowMax) winIndex = windowMin;

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;

                  while (tileRowIndex-- > 0)
                  {
                     tileColIndex = tileWidth;
                     while (tileColIndex-- > 0)
                     {
                        pixel = (vqdata[tileOffset++] << 8);
                        pixel |= ((vqdata[tileOffset++] & 0xFF));
                        decodedRow[destOffset++] = pixel;
                     }

                     destOffset += (width - tileWidth);
                  }
                  tileStartOffset += tileWidth;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, width);
            }
         }
      }

      private void decode24(ImageConsumer c)
      {
         int row, col;
         int windowMin, windowMax, mask, inhibits, seqLen, seqIndex, winIndex;
         int tilesPerCol, tilesPerRow, bytesPerLineIn, bytesPerLineOut, rowCount, colCount;
         int tileIndexBase, firstRowNextBand, bandIndex, tileRowsLastBand;
         int tileIndex, tileRowIndex, tileColIndex;
         int bytesPerTileRow, tileByteWidthIn, tileByteWidthOut, tileByteSize;
         int tileIndexOffset, tileBaseOffset, tileStartOffset;
         int tileOffset, destOffset, pixel;
         int bandTileCountOffset, bmRowTopOffset;
         int decodedRow[];
         byte LZSSWindow[];

         /*
          * Setup tile index base. This will be used to shift the tile indexes to
          * the proper range as we move from band to band of the encoded image.
          */
         tileIndexBase = 0;
         bandIndex = 0;
         firstRowNextBand = tileRowsPerBand;

         /*
          * If image is banded setup access to the codebook band tile count table
          * and calculate the number of tile rows in the last band
          */
         if (numberOfImageBands > 1)
         {
            tileRowsLastBand = ((height + tileHeight - 1) / tileHeight) - ((numberOfImageBands - 1) * tileRowsPerBand);
         }
         else
         {
            tileRowsLastBand = tileRowsPerBand;
         }

         tilesPerCol = (height + tileHeight - 1) / tileHeight;
         tilesPerRow = (width + tileWidth - 1) / tileWidth;
         bytesPerLineIn = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthIn = ((tileWidth * bitsPerPixel) >> 3);
         bytesPerLineOut = ((tilesPerRow * tileWidth * bitsPerPixel + 7) >> 3);
         tileByteWidthOut = ((tileWidth * bitsPerPixel) >> 3);

         decodedRow = new int[width * tileHeight];

         rowCount = tilesPerCol;
         colCount = tilesPerRow;
         bytesPerTileRow = bytesPerLineIn * tileHeight;
         tileByteSize = ((tileWidth * tileHeight * bitsPerPixel) >> 3);
         tileIndexOffset = imageDataOffset;
         tileBaseOffset = firstTileOffset;
         bandTileCountOffset = bandTileCountsOffset;
         bmRowTopOffset = 0;

         /*
          * If there is no tile index compression use this faster code
          */
         if (tileIndexCompression == VQTILECOMPRESS_NONE)
         {
            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  /* Decode next tile index */
                  tileIndex = (vqdata[tileIndexOffset++] & 0xFF);

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;
                  while (tileRowIndex-- > 0)
                  {
                     tileColIndex = tileWidth;
                     while (tileColIndex-- > 0)
                     {
                        pixel = (vqdata[tileOffset++] << 16);
                        pixel |= ((vqdata[tileOffset++] & 0xFF) << 8);
                        pixel |= ((vqdata[tileOffset++] & 0xFF));
                        decodedRow[destOffset++] = pixel;
                     }

                     destOffset += (width - tileWidth);
                  }
                  tileStartOffset += tileWidth;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, width);
            }
         }
         /*
          * We have tile index compression. Use this code.
          */
         else
         {
            /*
             * Perform tile index decompression setup
             */
            LZSSWindow = new byte[LZSS_WINDOW_SIZE];
            windowMin = 0;
            windowMax = LZSS_WINDOW_SIZE;
            mask = 0x100;
            seqLen = 0;
            seqIndex = 0;
            winIndex = LZSS_WINDOW_SIZE - LZSS_MaxMatchLen;
            inhibits = 0;

            for (row = 0; row < rowCount; ++row)
            {
               if ((row == rowCount - 1) && (rowCount * tileHeight != height))
               {
                  tileHeight = height - ((rowCount - 1) * tileHeight);
               }

               if (row == firstRowNextBand)
               {
                  tileIndexBase += getU16(vqdata, bandTileCountOffset + (bandIndex * 2));
                  ++bandIndex;
                  if (bandIndex == (numberOfImageBands - 1))
                  {
                     firstRowNextBand += tileRowsLastBand;
                  }
                  else
                  {
                     firstRowNextBand += tileRowsPerBand;
                  }
               }

               tileStartOffset = bmRowTopOffset;
               for (col = 0; col < colCount; ++col)
               {
                  if ( traceDecode ) System.out.println( "row= "+row+" col="+col );
                  /* Decode next tile index */
                  if (seqLen > 0)
                  {
                     tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                     if ( traceDecode ) System.out.println( "seqLen= "+seqLen+"seqIndex="+seqIndex+" tileIndex="+tileIndex );
                     if (seqIndex >= windowMax) seqIndex = windowMin;
                     --seqLen;
                  }
                  else
                  {
                     if (mask >= 0x100)
                     {
                        mask = 0x1;
                        inhibits = (vqdata[tileIndexOffset++] & 0xFF);
                     }

                     if ((inhibits & mask) != 0)
                     {
                        tileIndex = (vqdata[tileIndexOffset++] & 0xFF);
                        if ( traceDecode ) System.out.println( "mask= "+mask+" inhibits="+inhibits+" tileIndex="+tileIndex );
                     }
                     else
                     {
                        seqIndex = (vqdata[tileIndexOffset++] & 0xFF);
                        seqIndex |= ((vqdata[tileIndexOffset] & 0xF0) << 4);
                        seqLen = (vqdata[tileIndexOffset++] & 0x0F) + LZSS_MinMatchLen;

                        tileIndex = (LZSSWindow[seqIndex++] & 0xFF);
                        if (seqIndex >= windowMax) seqIndex = windowMin;
                        if ( traceDecode ) System.out.println( "mask= "+mask+" inhibits="+inhibits+" seqLen="+seqLen+" seqIndex="+seqIndex+" tileIndex="+tileIndex );
                     }
                     mask <<= 1;
                  }
                  LZSSWindow[winIndex++] = (byte) tileIndex;
                  if (winIndex >= windowMax) winIndex = windowMin;

                  /* Draw block */
                  tileOffset = tileBaseOffset + ((tileIndex + tileIndexBase) * tileByteSize);
                  destOffset = tileStartOffset;
                  tileRowIndex = tileHeight;

                  while (tileRowIndex-- > 0)
                  {
                     tileColIndex = tileWidth;
                     while (tileColIndex-- > 0)
                     {
                        pixel = (vqdata[tileOffset++] << 16);
                        pixel |= ((vqdata[tileOffset++] & 0xFF) << 8);
                        pixel |= ((vqdata[tileOffset++] & 0xFF));
                        decodedRow[destOffset++] = pixel;
                     }

                     destOffset += (width - tileWidth);
                  }
                  tileStartOffset += tileWidth;
               }

               c.setPixels(0, row * tileHeight, width, tileHeight, cm, decodedRow, 0, width);
            }
         }
      }

      static int getU8(byte data[], int i)
      {
         return (data[i] & 0xFF);
      }

      static int getS8(byte data[], int i)
      {
         return (data[i]);
      }

      static int getU16(byte data[], int i)
      {
         return (((data[i++] & 0xFF) << 8) | (data[i] & 0xFF));
      }

      static int getS16(byte data[], int i)
      {
         return (((data[i++]) << 8) | (data[i] & 0xFF));
      }

      static long getU32(byte data[], int i)
      {
         return (((long) (data[i++] & 0xFF) << 24) | (((data[i++] & 0xFF) << 16) | ((data[i++] & 0xFF) << 8) | ((data[i] & 0xFF))));
      }

      static long getS32(byte data[], int i)
      {
         return (((long) (data[i++]) << 24) | (((data[i++]) << 16) | ((data[i++]) << 8) | ((data[i]))));
      }
   }
   
   private static class MyImageConsumer implements ImageConsumer
   {
      private static final boolean trace= false;
      private static final boolean dump= false;
      
      private boolean verify;
      private int status;   
      private int width;
      private int height;
      private int hash;
      
      public MyImageConsumer( boolean verify )
      {
         this.verify= verify;
         status= -1;
      }
      
      public int getStatus()
      {
         return status;
      }
      
      public int getHash()
      {
         if ( trace ) System.out.println("hash="+Integer.toHexString(hash) );
         return hash;
      }
      
      public void imageComplete( int status )
      {
         this.status= status;
      }
      
      public void setColorModel( ColorModel model )
      {      
      }
      
      public void setDimensions(int width, int height )
      {
         this.width= width;
         this.height= height;
         this.hash= 0;
         if ( trace ) System.out.println( "MyImageConsumer: width="+width+" height="+height );
      }
      
      public void setHints( int hintFlags )
      {
      }
      
      public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize)
      {
         if ( trace ) System.out.println( "MyImageConsumer: setPixels x="+x+" y="+y+" off="+off+" scansize="+scansize );
         if ( verify )
         {
            int i, imax;
            
            imax= off+scansize*h;
            for( i= off; i < imax; ++i  )
            {
               hash= (hash << 4) ^ pixels[i];
               if ( (hash & 0x80000000) != 0 )
               {
                  hash ^= 0x800C1003;
               }
            }
         }
      }
      
      public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize)
      {      
         if ( trace ) System.out.println( "MyImageConsumer: setPixels x="+x+" y="+y+" off="+off+" scansize="+scansize );
         if ( verify )
         {         
            int i, imax;
            
            imax= off+scansize*h;
            for( i= off; i < imax; ++i  )
            {
               if ( dump && (y <= 2 )) System.out.println( Integer.toHexString( pixels[i]) );
               hash= (hash << 4) ^ pixels[i];
               if ( (hash & 0x80000000) != 0 )
               {
                  hash ^= 0x800C1003;
               }
            }
         }
      }
      
      public void setProperties(Hashtable props)
      {      
      }
   }
   
   interface ImageProducer
   {
      public void addConsumer( ImageConsumer ic );
      public boolean isConsumer( ImageConsumer ic );
      public void removeConsumer( ImageConsumer ic );
      public void requestTopDownLeftRightResend( ImageConsumer ic );
      public void startProduction( ImageConsumer ic );
   }   
   
   interface ImageConsumer
   {
      public static final int COMPLETESCANLINES= 1;
      public static final int RANDOMPIXELORDER= 2;
      public static final int TOPDOWNLEFTRIGHT= 4;
      public static final int SINGLEPASS= 8;
      public static final int SINGLEFRAME= 16;

      public static final int IMAGEABORTED= 100;
      public static final int IMAGEERROR= 101;
      public static final int SINGLEFRAMEDONE= 102;
      public static final int STATICIMAGEDONE= 103;

      public void imageComplete( int status );
      public void setColorModel( ColorModel model );
      public void setDimensions(int width, int height );
      public void setHints( int hintFlags );
      public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize);
      public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize);
      public void setProperties(Hashtable props);
   }

   private static class DirectColorModel extends ColorModel
   {
      public DirectColorModel( int bits, int rmask, int gmask, int bmask )
      {
         super( bits );
      }
   }
   
   private static class IndexColorModel extends ColorModel
   {
      public IndexColorModel( int bits, int size, byte[] cmap, int start, boolean hasalpha, int trans )
      {
         super( bits );
      }
   }
   
   private static class ColorModel
   {
      protected int bpp;

      public ColorModel( int bitsperpixel )
      {
         bpp= bitsperpixel;
      }

      public int getBpp()
      {
         return bpp;
      }
   }
   
}

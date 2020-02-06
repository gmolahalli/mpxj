/*
 * file:       PrimaveraInputStreamReader.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2011
 * date:       08/08/2011
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.primavera;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * This class creates an InputStream reader with a CharsetDecoder
 * which can be used to silently ignore incorrectly encoded characters,
 * and replace characters which are invalid for XML 1.0 with the
 * Unicode replacement character. 
 */
class PrimaveraInputStreamReader extends InputStreamReader
{
   /**
    * Constructor.
    * 
    * @param is input stream
    * @param charset character set name
    */
   public PrimaveraInputStreamReader(InputStream is, String charset)
   {
      super(is, decoder(charset));
   }

   @Override public int read() throws IOException
   {
      int c = super.read();
      if (!validXmlChar(c))
      {
         c = REPLACEMENT_CHAR;
      }
      return c;
   }

   @Override public int read(char[] cbuf, int offset, int length) throws IOException
   {
      int result = super.read(cbuf, offset, length);
      if (result > 0)
      {
         for (int index = 0; index < result; index++)
         {
            if (!validXmlChar(cbuf[index]))
            {
               cbuf[index] = REPLACEMENT_CHAR;
            }
         }
      }
      return result;
   }

   /**
    * Returns true if this is a valid XML 1.0 character.
    * 
    * @param c character value
    * @return true if this is a valid XML 1.0 character
    */
   private boolean validXmlChar(int c)
   {
      return c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0x10FFFF);
   }

   /**
    * Create a charset decoder which silently ignores 
    * incorrectly encoded characters.
    * 
    * @param charset charset name
    * @return CharsetDecoder instance
    */
   private static CharsetDecoder decoder(String charset)
   {
      CharsetDecoder decoder = Charset.forName(charset).newDecoder();
      decoder.onMalformedInput(CodingErrorAction.REPLACE);
      decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
      return decoder;
   }

   private static final int REPLACEMENT_CHAR = 0xFFFD;
}

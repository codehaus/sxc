/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.envoisolutions.sxc.util.Base64;

public class BinaryUtils {
    public static byte[] decodeAsBytes(XMLStreamReader reader) 
        throws XMLStreamException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        while (reader.getEventType() != XMLStreamReader.END_ELEMENT
               && reader.getEventType() != XMLStreamReader.CHARACTERS) 
            reader.next();
        
        if (reader.isEndElement())
            return new byte[0];
        
        int length = reader.getTextLength();
        
        char[] myBuffer = new char[length];
        for (int sourceStart = 0;; sourceStart += length)
        {
            int nCopied = reader.getTextCharacters(sourceStart, myBuffer, 0, length);
            
            try {
                Base64.decode(myBuffer, 0, nCopied, bos);
            } catch (IOException e) {
                throw new XMLStreamException("Could not decode base64 text.", e);
            }
            
            if (nCopied < length)
                break;
        }
        
        while (!reader.isEndElement()) reader.next();
        
        try {
            bos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return bos.toByteArray();
    }
    
    public static void encodeBytes(XMLStreamWriter writer, byte[] bytes) throws XMLStreamException {
        // TODO: Could be more efficient if we didn't make a String
        writer.writeCharacters( Base64.encode(bytes) );
    }
    
    public static DataHandler decodeAsDataHandler(XMLStreamReader reader, AttachmentUnmarshaller au) {
        return null;
    }
}

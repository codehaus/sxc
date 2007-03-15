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

package com.envoisolutions.sxc.builder;

import javax.xml.namespace.QName;

import com.envoisolutions.sxc.Context;
import com.envoisolutions.sxc.util.XoXMLStreamReader;

public interface GeneratedReader {
    public Object read(XoXMLStreamReader reader, 
                       Context context) throws Exception;
    
    public Object read(XoXMLStreamReader reader, 
                       Context context,
                       QName type) throws Exception;
}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.01.29 at 04:15:32 PM GMT
//

package net.sf.mpxj.phoenix.schema;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import net.sf.mpxj.Day;

public class Adapter6 extends XmlAdapter<String, Day>
{

   @Override public Day unmarshal(String value)
   {
      return (net.sf.mpxj.phoenix.DatatypeConverter.parseDaye(value));
   }

   @Override public String marshal(Day value)
   {
      return (net.sf.mpxj.phoenix.DatatypeConverter.printDay(value));
   }

}

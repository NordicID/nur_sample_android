### Tag Data Translation library for manipulate GS1 coded tags.
See the <a href="https://www.gs1.org/standards/epc-rfid/tag-data-translation" target="_blank">Tag Data Translation Standard</a>

#### Example: Translating EPC code to GS1 scheme (giai-202)
````
EPCTagEngine eng = new EPCTagEngine("383559CCA5AB56EC183072E583060E5CB76EE1B346CD9A800000");
  
  TDTScheme sc = eng.getScheme();
  System.out.println(sc.getName() + " (" + sc.getFriendlyName()+")");
  System.out.println("PureURI=" + eng.buildPureIdentityURI());
  System.out.println("TagURI=" + eng.buildTagURI());
  System.out.println(eng.getSegmentName(0) + ":" + eng.getSegment(0).toString());
  System.out.println(eng.getSegmentName(1) + ":" + eng.getSegment(1).toString());
````
 
 Output:
 ````
 giai-202 (Global Individual Asset Identifier)
 PureURI=urn:epc:id:giai:5665577.557000990009977864665
 TagURI=urn:epc:tag:giai-202:1.5665577.557000990009977864665
 Company Prefix:5665577
 Individual Asset Reference:557000990009977864665
 ````
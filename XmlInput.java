import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


public class XmlInputTransformer implements Iterator<StoreXml> {

    private XPath xmlPath;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'Z'");
    private static final DateTimeFormatter ALT_DATE_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy");
    private static final LocalDate DATE_CONSIDERED_UNKNOWN = LocalDate.parse("9999-01-31Z", DATE_FORMAT);

    private static Pattern TIME_FRAME_PATTERN = Pattern.compile("\\A(\\d{2}:\\d{2})-(\\d{2}:\\d{2})\\z");
    private static Pattern DAY_NAME_PATTERN = Pattern.compile("\\ACOH_(\\w+)\\z");
    private static Pattern SUN_BROWSE_PATTERN = Pattern.compile("\\ASUN_(\\w+)\\z");
    private static Pattern SUN_BROWSE_TIME_PATTERN = Pattern.compile("\\A(\\d{2}:\\d{2})\\z");

    private Transformer transformer;
    private XMLStreamReader xmlStreamReader;

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlInputTransformer.class);

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public XmlInputTransformer(InputStream input) {
        try {
            final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            this.xmlStreamReader = new StreamReaderDelegate(inputFactory.createXMLStreamReader(input)) {
                @Override
                public String getVersion() {
                    return "1.0";
                }
            };
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // to be compliant, prohibit the use of all protocols by external entities:
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            this.transformer = transformerFactory.newTransformer();
            this.xmlPath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            throw new XmlTransformationException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            while (!isStoreData()) {
                xmlStreamReader.next();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StoreXml next() {
        try {
            while (!isStoreData()) {
                xmlStreamReader.next();
            }

            DOMResult result = new DOMResult();
            transformer.transform(new StAXSource(xmlStreamReader), result);
            Node domNode = result.getNode();
            return transformNode(domNode);
        } catch (Exception e) {
            throw new XmlTransformationException(e);
        }
    }

    private boolean isStoreData() {
        return xmlStreamReader.isStartElement() && xmlStreamReader.getLocalName().equals("StoreData");
    }

    private StoreXml transformNode(Node node) {
        StoreXml.StoreXmlBuilder builder = new StoreXml.StoreXmlBuilder();

        try {
            // Parse the ID and then make it available to logging
            Integer storeId = getInteger("//SAPStoreId", node);
            MDC.put("storeId", String.valueOf(storeId));
            builder.setId(storeId);

            builder.setName(getString("//StoreName", node));
            builder.setAlias(getString("//StoreAliasName", node));
            builder.setLanguage(getString("//Language", node));
            builder.setStoreType(StoreType.fromString(getString("//StoreType", node)));
            builder.setOpeningDate(getDate("//StoreOpeningDate", node));
            builder.setClosingDate(getDate("//StoreClosingDate", node));
            builder.setCoordinates(
                    new Coordinates(
                            getDouble("//Latitude", node),
                            getDouble("//Longitude", node)
                    )
            );
            builder.setIsChristmasOrderAccepted(getBoolean("//IsChristmasOrderAccepted", node));
            builder.setIsGmCollectionSupported(getBoolean("//IsGMCollectionSupported", node));
            builder.setIsFoodCollectionSupported(getBoolean("//IsFoodCollectionSupported", node));
            builder.setLocationTypeCode(getInteger("//LocationTypeCode", node));
            builder.setLocationTypeName(getString("//LocationTypeName", node));
            builder.setVatRegNumber(getString("//VATRegNumber", node));
            builder.setShowStoreInfo(getBoolean("//ShowStoreInfo", node));
            builder.setStoreStatus(StoreStatus.fromString(getString("//StoreStatus", node)));
            builder.setCoreOpeningHours(getCoreOpeningHours(node, getSundayBrowsingHours(node)));
            //builder.setSundayBrowsingHours(getSundayBrowsingHours(node));
            builder.setSpecialOpeningHours(getSpecialOpeningHours(node));
            builder.setAddress(
                    new Address(
                            getString("//StoreAddress/AddressLine1", node),
                            getString("//StoreAddress/AddressLine2", node),
                            getString("//StoreAddress/City", node),
                            getString("//StoreAddress/County", node),
                            getString("//StoreAddress/ISOTwoCountryCode", node),
                            getString("//StoreAddress/Country", node),
                            getString("//StoreAddress/PostalCode", node)
                    )
            );
            builder.setPhone(getString("//StoreAddress/Phone", node));
            builder.setFacilities(getFacilities("//Facilities", node));
            builder.setDepartments(getPipeDelimeteredDepartments("//Departments", node));
            builder.setServices(getPipeDelimeteredServices("//Services", node));

            StoreXml store = builder.build();

            MDC.remove("store");
            return store;
        } catch (Exception e) {
            LOGGER.error("Failed to convert Store XML", e);
            MDC.remove("store");
            return builder.build();
        }
    }

    private String getString(String xmlPathExpression, Node node) {
        try {
            return (String) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private Integer getInteger(String xmlPathExpression, Node node) {
        try {
            Number number = (Number) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.NUMBER);
            return number.intValue();
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private Double getDouble(String xmlPathExpression, Node node) {
        try {
            Number number = (Number) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.NUMBER);
            return number.doubleValue();
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private Boolean getBoolean(String xmlPathExpression, Node node) {
        try {
            String result = (String) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.STRING);
            result = result.trim();
            return (result.equals("1") || result.equals("true"));
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private LocalDate getDate(String xmlPathExpression, Node node) {
        try {
            String date = (String) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.STRING);
            LocalDate localDate = LocalDate.parse(date, DATE_FORMAT);

            // If the date is unknown then return null
            return localDate.compareTo(DATE_CONSIDERED_UNKNOWN) >= 0 ? null : localDate;
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private StoreTime getSundayBrowsingHours(Node node) {
        try {

            NodeList nodes = (NodeList) xmlPath.evaluate("//CoreOpeningHours/*[self::SUN_BROWSE]",
                    node, XPathConstants.NODESET);
            return IntStream.range(0, nodes.getLength())
                    .boxed()
                    .map(nodes::item) // Locate the relevant node
                    .map(this::convertNodeToSunBrowseTime)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
                    // Convert the node to a DayOpenPeriod

        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private List<DayOpenPeriod> getCoreOpeningHours(Node node, StoreTime browseTime) {
        try {
            NodeList nodes = (NodeList) xmlPath.evaluate("//CoreOpeningHours/*[self::COH_Monday "
                    + "or self::COH_Tuesday or self::COH_Wednesday " + "or self::COH_Thursday "
                    + "or self::COH_Friday or self::COH_Saturday or self::COH_Sunday]", node, XPathConstants.NODESET);

            return IntStream.range(0, nodes.getLength())
                    .mapToObj(i -> covertNodeToDayOpenPeriod(nodes.item(i), browseTime))
                    .collect(Collectors.toList()); // Collect them into a list
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private List<SpecialOpenPeriod> getSpecialOpeningHours(Node node) {
        try {
            NodeList nodes = (NodeList) xmlPath.evaluate("//SpecialOpeningHours/*", node, XPathConstants.NODESET);
            return IntStream.range(0, nodes.getLength())
                    .boxed()
                    .map(nodes::item) // Locate the relevant node
                    .map(this::covertNodeToSpecialOpenPeriod) // Convert the node to a SpecialOpenPeriod
                    .filter(Objects::nonNull) // Remove the many blanks
                    .collect(Collectors.toList()); // Collect them into a list
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private DayOpenPeriod covertNodeToDayOpenPeriod(Node node, StoreTime browseTime) {
        String timeRangeString = node.getTextContent();
        StoreTime browse = null;

        Matcher timeMatcher = TIME_FRAME_PATTERN.matcher(timeRangeString);
        Matcher dayMatcher = DAY_NAME_PATTERN.matcher(node.getNodeName());
        dayMatcher.matches();

        if (!timeMatcher.matches()) {
            return new DayOpenPeriod(
                    DayOfWeek.valueOf(dayMatcher.group(1).toUpperCase()),
                    new StoreTime(0, 0),
                    new StoreTime(0, 0),
                    browse
            );
        }

        if ("SUNDAY".equalsIgnoreCase(dayMatcher.group(1))) {
            browse = browseTime;
        }

        return new DayOpenPeriod(
                DayOfWeek.valueOf(dayMatcher.group(1).toUpperCase()),
                convertToStoreTime(timeMatcher.group(1)),
                convertToStoreTime(timeMatcher.group(2)),
                browse
        );
    }

    private StoreTime convertNodeToSunBrowseTime(Node node) {
        String timeRangeString = node.getTextContent();
        Matcher timeMatcher = SUN_BROWSE_TIME_PATTERN.matcher(timeRangeString);
        Matcher sunBrowseMatcher = SUN_BROWSE_PATTERN.matcher(node.getNodeName());
        sunBrowseMatcher.matches();

        if (!timeMatcher.matches()) {
            return null;
        }

        return convertToStoreTime(timeMatcher.group(1));
    }

    private SpecialOpenPeriod covertNodeToSpecialOpenPeriod(Node node) {
        String[] parts = node.getTextContent().split("\\|", 4);

        if (parts.length < 4) {
            return null;
        }

        Matcher timeMatcher = TIME_FRAME_PATTERN.matcher(parts[2]);
        String browse = parts[3];

        // TODO So there are some very strange ways of representing data in the XML feed 0 is closed but what is null?
        // I'm assuming null is closed
        if (!timeMatcher.matches()) {
            return new SpecialOpenPeriod(
                    parts[0],
                    LocalDate.parse(parts[1], ALT_DATE_FORMAT),
                    new StoreTime(0, 0),
                    new StoreTime(0, 0),null,
                    null
            );
        }

        return new SpecialOpenPeriod(
                parts[0],
                LocalDate.parse(parts[1], ALT_DATE_FORMAT),
                convertToStoreTime(timeMatcher.group(1)),
                convertToStoreTime(timeMatcher.group(2)),null,
                !browse.isEmpty() ? convertToStoreTime(browse) : null
        );
    }

    private StoreTime convertToStoreTime(String time) {
        String[] parts = time.split(":");

        return new StoreTime(
                Integer.valueOf(parts[0]),
                Integer.valueOf(parts[1])
        );
    }

    private List<Facility> getFacilities(String xmlPathExpression, Node node) {
        try {
            String facilitiesString = (String) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.STRING);
            String[] facilities = facilitiesString.split("\\|");
            return Arrays.asList(facilities).stream()
                    .filter(fs -> !fs.isEmpty())
                    .map(fs -> fs.split(",", 2)) // Split the string
                    .filter(fs -> fs[1].equals("true")) // Filter only the facilities set to true
                    .map(fs -> fs[0].trim()) // Now only continue with the first value (the facility id)
                    .map(Facility::fromString) // Convert it to a full facility value
                    .collect(Collectors.toList()); // Collect the results into a list
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private List<Service> getPipeDelimeteredServices(String xmlPathExpression, Node node) {
        try {
            String valueString = (String) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.STRING);
            String[] ids = valueString.split("\\|");
            return Arrays.asList(ids).stream()
                    .filter(id -> !id.isEmpty())
                    .map(String::trim)
                    .map(Service::fromString)
                    .collect(Collectors.toList());
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }

    private List<Department> getPipeDelimeteredDepartments(String xmlPathExpression, Node node) {
        try {
            String valueString = (String) xmlPath.evaluate(xmlPathExpression, node, XPathConstants.STRING);
            String[] ids = valueString.split("\\|");
            return Arrays.asList(ids).stream()
                    .filter(id -> !id.isEmpty())
                    .map(String::trim)
                    .map(Department::fromString)
                    .collect(Collectors.toList());
        } catch (XPathExpressionException e) {
            throw new XmlTransformationException(e);
        }
    }
}

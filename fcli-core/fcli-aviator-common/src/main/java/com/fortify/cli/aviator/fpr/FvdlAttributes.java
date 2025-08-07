package com.fortify.cli.aviator.fpr;

public final class FvdlAttributes {
    // From ClassInfo
    public static final String CLASS_ID = "ClassID";
    public static final String KINGDOM = "Kingdom";
    public static final String ANALYZER_NAME = "AnalyzerName";
    public static final String TYPE = "Type";
    public static final String SUBTYPE = "Subtype";
    public static final String DEFAULT_SEVERITY = "DefaultSeverity";

    // From InstanceInfo
    public static final String INSTANCE_ID = "InstanceID";
    public static final String INSTANCE_SEVERITY = "InstanceSeverity";
    public static final String CONFIDENCE = "Confidence";
    public static final String INSTANCE_DESCRIPTION = "InstanceDescription";

    // From AnalysisInfo (general)
    public static final String SOURCE_LOCATION_PATH = "SourceLocationPath";
    public static final String SOURCE_LOCATION_LINE = "SourceLocationLine";
    public static final String SOURCE_LOCATION_LINE_END = "SourceLocationLineEnd";
    public static final String SOURCE_LOCATION_COL_START = "SourceLocationColStart";
    public static final String SOURCE_LOCATION_COL_END = "SourceLocationColEnd";
    public static final String SOURCE_LOCATION_SNIPPET = "Snippet";
    public static final String SOURCE_LOCATION_CONTEXT_ID = "ContextId";

    // Trace-related (calculated/aggregated)
    public static final String TAINT_FLAGS = "TaintFlags"; // Aggregated list
    public static final String KNOWLEDGE = "Knowledge"; // Aggregated map

    // AuxiliaryData
    public static final String AUXILIARY_DATA = "AuxiliaryData"; // List of maps

    // ExternalEntries
    public static final String EXTERNAL_ENTRIES = "ExternalEntries"; // List of Entry

    // ExternalID
    public static final String EXTERNAL_ID = "ExternalID"; // Map name->value

    // Calculated/Meta
    public static final String ACCURACY = "Accuracy";
    public static final String IMPACT = "Impact";
    public static final String PROBABILITY = "Probability";
    public static final String AUDIENCE = "Audience";
    public static final String LIKELIHOOD = "Likelihood";
    public static final String PRIORITY = "Priority";
    public static final String CATEGORY = "Category"; // Type + Subtype

    // Request-related (from aux/external if present)
    public static final String REQUEST_HEADERS = "RequestHeaders";
    public static final String REQUEST_PARAMETERS = "RequestParameters";
    public static final String REQUEST_BODY = "RequestBody";
    public static final String REQUEST_METHOD = "RequestMethod";
    public static final String REQUEST_COOKIES = "RequestCookies";
    public static final String REQUEST_HTTP_VERSION = "RequestHttpVersion";
    public static final String ATTACK_PAYLOAD = "AttackPayload";
    public static final String ATTACK_TYPE = "AttackType";
    public static final String RESPONSE = "Response";
    public static final String TRIGGER = "Trigger";
    public static final String MAPPED_CATEGORY = "MappedCategory";
    public static final String SECONDARY_REQUESTS = "SecondaryRequests";
    public static final String VULNERABLE_PARAMETER = "VulnerableParameter";

    // Other (from library/calculated)
    public static final String BUILD_ID = "BuildId";
    public static final String UUID = "Uuid";
    public static final String FILE_TYPE = "FileType";

    private FvdlAttributes() {} // Prevent instantiation
}
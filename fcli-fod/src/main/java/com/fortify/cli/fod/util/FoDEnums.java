package com.fortify.cli.fod.util;

// TODO Any good reason for having one big class defining many enums? Was this somehow generated from FoD?
// TODO Are all these enums actually used?
// TODO Maybe better to have top-level enums in the entity packages that use them, if applicable. 
//      For example, most of the scan-related enums only seem to be used by the scan-related entities.
// TODO I'm not an FoD expert, and maybe its related to how FoD API's are structured, but there seems
//      to be some overlap between different enums; maybe we should review in more detail and restructure
//      if needed.
public class FoDEnums {
    public enum RemediationScanPreferenceType {
        RemediationScanIfAvailable(0),
        RemediationScanOnly(1),
        NonRemediationScanOnly(2);

        private final int _val;

        RemediationScanPreferenceType(int val) {
            this._val = val;
        }

        public int getValue() {
            return this._val;
        }

        public String toString() {
            switch (this._val) {
                case 0:
                    return "RemediationScanIfAvailable";
                case 1:
                    return "RemediationScanOnly";
                case 2:
                default:
                    return "NonRemediationScanOnly";
            }
        }

        public static RemediationScanPreferenceType fromInt(int val) {
            switch (val) {
                case 1:
                    return RemediationScanOnly;
                case 0:
                    return RemediationScanIfAvailable;
                case 2:
                default:
                    return NonRemediationScanOnly;
            }
        }
    }

    public enum AuditPreferenceTypes {
        Manual(1),
        Automated(2);

        private final int _val;

        AuditPreferenceTypes(int val) {
            this._val = val;
        }

        public int getValue() {
            return this._val;
        }

        public String toString() {
            switch (this._val) {
                case 1:
                    return "Manual";
                case 2:
                default:
                    return "Automated";
            }
        }

        public static AuditPreferenceTypes fromInt(int val) {
            switch (val) {

                case 1:
                    return Manual;
                case 2:
                default:
                    return Automated;
            }
        }
    }

    public enum EntitlementPreferenceType {
        SingleScanOnly(1),
        SubscriptionOnly(2),
        SingleScanFirstThenSubscription(3),
        SubscriptionFirstThenSingleScan(4) ;

        private final int _val;

        EntitlementPreferenceType(int val) {
            this._val = val;
        }
        public int getValue() {
            return this._val;
        }

        public String toString() {
            switch (this._val) {
                case 1:
                    return "SingleScanOnly";
                case 2:
                    return "SubscriptionOnly";
                case 3:
                    return "SingleScanFirstThenSubscription";
                case 4:
                default:
                    return "SubscriptionFirstThenSingleScan";
            }
        }

        public static EntitlementPreferenceType fromInt(int val) {
            switch (val) {
                case 1:
                    return SingleScanOnly;
                case 2:
                    return SubscriptionOnly;
                case 3:
                    return SingleScanFirstThenSubscription ;
                case 4:
                    return SubscriptionFirstThenSingleScan;
                default:
                    return null;
            }
        }
    }

    public enum InProgressScanActionType {
        DoNotStartScan(0),
        CancelScanInProgress(1),
        Queue(2);

        private final int _val;

        InProgressScanActionType(int val) {
            this._val = val;
        }

        public int getValue() {
            return this._val;
        }

        public String toString() {
            switch (this._val) {
                case 1:
                    return "CancelInProgressScan";
                case 2:
                    return "Queue";
                case 0:
                default:
                    return "DoNotStartScan";
            }
        }

        public static InProgressScanActionType fromInt(int val) {
            switch (val) {
                case 2:
                    return Queue;
                case 1:
                    return CancelScanInProgress;
                case 0:
                default:
                    return DoNotStartScan;
            }
        }
    }

    public enum EntitlementFrequencyTypes {
        SingleScan(1),
        Subscription(2);

        private final int _val;

        EntitlementFrequencyTypes(int val) {
            this._val = val;
        }

        public int getValue() {
            return this._val;
        }

        public String toString() {
            switch (this._val) {
                case 1:
                    return "SingleScan";
                case 2:
                default:
                    return "Subscription";
            }
        }

        public static EntitlementFrequencyTypes fromInt(int val) {
            switch (val) {

                case 1:
                    return SingleScan;
                case 2:
                default:
                    return Subscription;
            }
        }
    }

    public enum DynamicScanEnvironmentFacingType {
        Internal,
        External
    }

    public enum RepeatScheduleType {
        NoRepeat,
        Biweekly,
        Monthly,
        Bimonthly,
        Quarterly,
        Triannually,
        Semiannually,
        Annually
    }

    public enum WebServiceType {
        SOAP,
        REST,
        PostmanCollectionFile,
        PostmanCollectionURL,
        OpenApiFile,
        OpenApiUrl
    }

    public enum UserAgentType {
        Desktop, Mobile
    }

    public enum ConcurrentRequestThreadsType {
        Standard, Limited
    }
    public enum Day {
        Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    }

    public enum TimeZones {
        Dateline("Dateline Standard Time"),
        UTCMinus11("UTC-11"),
        Aleutian("Aleutian Standard Time"),
        Hawaiian("Hawaiian Standard Time"),
        Marquesas("Marquesas Standard Time"),
        Alaskan("Alaskan Standard Time"),
        UTCMinus9("UTC-09"),
        PacificMexico("Pacific Standard Time (Mexico)"),
        UTCMinus8("UTC-08"),
        Pacific("Pacific Standard Time"),
        USMountain("US Mountain Standard Time"),
        MountainMexico("Mountain Standard Time (Mexico)"),
        Mountain("Mountain Standard Time"),
        Yukon("Yukon Standard Time"),
        CentralAmerica("Central America Standard Time"),
        Central("Central Standard Time"),
        EastIsland("Easter Island Standard Time"),
        CentralMexico("Central Standard Time (Mexico)"),
        CentralCanda("Canada Central Standard Time"),
        SAPacific("SA Pacific Standard Time"),
        EasternMexico("Eastern Standard Time (Mexico)"),
        Eastern("Eastern Standard Time"),
        Haiti("Haiti Standard Time"),
        Cuba("Cuba Standard Time"),
        USEastern("US Eastern Standard Time"),
        TurksAndCaicos("Turks And Caicos Standard Time"),
        Paraguay("Paraguay Standard Time"),
        Atlantic("Atlantic Standard Time"),
        Venezuela("Venezuela Standard Time"),
        CentralBrazilian("Central Brazilian Standard Time"),
        SAWestern("SA Western Standard Time"),
        PacificSA("Pacific SA Standard Time"),
        Newfoundland("Newfoundland Standard Time"),
        Tocantins("Tocantins Standard Time"),
        EastSouthAmerica("E. South America Standard Time"),
        SAEastern("SA Eastern Standard Time"),
        Argentina("Argentina Standard Time"),
        Greenland("Greenland Standard Time"),
        Montevideo("Montevideo Standard Time"),
        Magallanes("Magallanes Standard Time"),
        SaintPierre("Saint Pierre Standard Time"),
        Bahia("Bahia Standard Time"),
        UTCMinus2("UTC-02"),
        MidAtlantic("Mid-Atlantic Standard Time"),
        Azores("Azores Standard Time"),
        CapeVerde("Cape Verde Standard Time"),
        UTC("UTC"),
        GMT("GMT Standard Time"),
        Greenwich("Greenwich Standard Time"),
        SaoTome("Sao Tome Standard Time"),
        Morocco("Morocco Standard Time"),
        WestEurope("W. Europe Standard Time"),
        CentralEurope("Central Europe Standard Time"),
        Romance("Romance Standard Time"),
        CentralEuropean("Central European Standard Time"),
        WestCentralAfrica("W. Central Africa Standard Time"),
        Jordan("Jordan Standard Time"),
        GTB("GTB Standard Time"),
        MiddleEast("Middle East Standard Time"),
        Egypt("Egypt Standard Time"),
        EastEurope("E. Europe Standard Time"),
        Syria("Syria Standard Time"),
        WestBank("West Bank Standard Time"),
        SouthAfrica("South Africa Standard Time"),
        FLE("FLE Standard Time"),
        Israel("Israel Standard Time"),
        SouthSudan("South Sudan Standard Time"),
        Kaliningrad("Kaliningrad Standard Time"),
        Sudan("Sudan Standard Time"),
        Libya("Libya Standard Time"),
        Namibia("Namibia Standard Time"),
        Arabic("Arabic Standard Time"),
        Turkey("Turkey Standard Time"),
        Arab("Arab Standard Time"),
        Belarus("Belarus Standard Time"),
        Russian("Russian Standard Time"),
        EastAfrica("E. Africa Standard Time"),
        Volgograd("Volgograd Standard Time"),
        Iran("Iran Standard Time"),
        Arabian("Arabian Standard Time"),
        Astrakhan("Astrakhan Standard Time"),
        Azerbaijan("Azerbaijan Standard Time"),
        Russia3("Russia Time Zone 3"),
        Mauritius("Mauritius Standard Time"),
        Saratov("Saratov Standard Time"),
        Georgian("Georgian Standard Time"),
        Caucasus("Caucasus Standard Time"),
        Afghanistan("Afghanistan Standard Time"),
        WestAsia("West Asia Standard Time"),
        Ekaterinburg("Ekaterinburg Standard Time"),
        Pakistan("Pakistan Standard Time"),
        Qyzylorda("Qyzylorda Standard Time"),
        India("India Standard Time"),
        SriLanka("Sri Lanka Standard Time"),
        Nepal("Nepal Standard Time"),
        CentralAsia("Central Asia Standard Time"),
        Bangladesh("Bangladesh Standard Time"),
        Omsk("Omsk Standard Time"),
        Myanmar("Myanmar Standard Time"),
        SEAsia("SE Asia Standard Time"),
        Altai("Altai Standard Time"),
        WestMongolia("W. Mongolia Standard Time"),
        NorthAsia("North Asia Standard Time"),
        NorthCentralAsia("N. Central Asia Standard Time"),
        Tomsk("Tomsk Standard Time"),
        China("China Standard Time"),
        NortAsiaEast("North Asia East Standard Time"),
        Singapore("Singapore Standard Time"),
        WestAustralia("W. Australia Standard Time"),
        Taipei("Taipei Standard Time"),
        Ulaanbaatar("Ulaanbaatar Standard Time"),
        AustraliaCentralWest("Aus Central W. Standard Time"),
        Transbaikal("Transbaikal Standard Time"),
        Tokyo("Tokyo Standard Time"),
        NorthKorea("North Korea Standard Time"),
        Korea("Korea Standard Time"),
        Yakutsk("Yakutsk Standard Time"),
        CentralAustralia("Cen. Australia Standard Time"),
        AustraliaCentral("AUS Central Standard Time"),
        EastAustralia("E. Australia Standard Time"),
        AustrialiaEasten("AUS Eastern Standard Time"),
        WestPacific("West Pacific Standard Time"),
        Tasmania("Tasmania Standard Time"),
        Vladivostok("Vladivostok Standard Time"),
        LordHowe("Lord Howe Standard Time"),
        Bougainville("Bougainville Standard Time"),
        Russia10("Russia Time Zone 10"),
        Magadan("Magadan Standard Time"),
        Norfolk("Norfolk Standard Time"),
        Sakhalin("Sakhalin Standard Time"),
        CentralPacific("Central Pacific Standard Time"),
        Russia11("Russia Time Zone 11"),
        NewZealand("New Zealand Standard Time"),
        UTCPlus12("UTC+12"),
        Fiji("Fiji Standard Time"),
        Kamchatka("Kamchatka Standard Time"),
        ChathamIslands("Chatham Islands Standard Time"),
        UTCPlus13("UTC+13"),
        Tonga("Tonga Standard Time"),
        Samoa("Samoa Standard Time"),
        LineIslands("Line Islands Standard Time");

        public final String value;
        TimeZones(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

    }

    public enum UserApplicationAccessAction {
        Add, Remove
    }

    public enum UserGroupApplicationAccessAction {
        Add, Remove
    }

    public enum UserGroupMembershipAction {
        Add, Remove
    }




}

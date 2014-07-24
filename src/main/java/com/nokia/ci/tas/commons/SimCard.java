package com.nokia.ci.tas.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * Incapsulates all information about the SIM card used in the product supported by the Testing Automation Service.
 *
 * The XML format used for representing the 1st SIM card in a dual-SIM product:

    <sim1>
        <!-- Phone number (or MSISDN) assigned to this SIM card. Empty phone number means that SIM card is not used -->
        <phone-number>+3585550000001</phone-number>

        <!-- The 1st and the 2nd Personal Identification Number codes -->
        <pin1>1234</pin1>
        <pin2>1234</pin2>

        <!-- The 1st and the 2nd PIN Unblocking Key codes -->
        <puk1>12345</puk1>
        <puk2>12345</puk2>

        <!-- A security code assigned to the SIM card -->
        <security-code>12345</security-code>

        <!-- International Mobile Subscription Identity number, which identificates this SIM card -->
        <imsi>244070103300372</imsi>

        <!-- Service dialling number associated with the SIM card -->
        <service-dialling-number>+3585550000010</service-dialling-number>

        <!-- Number of a voice mailbox -->
        <voice-mailbox-number>+3585550000011</voice-mailbox-number>
    </sim1>

 * The XML format used for representing the 2nd SIM card in a dual-SIM products:

    <sim2>
        <phone-number>+3585550000002</phone-number>
        <pin1>1234</pin1>
        <pin2>1234</pin2>
        <puk1>12345</puk1>
        <puk2>12345</puk2>
        <security-code>12345</security-code>
        <imsi>244070103300373</imsi>
        <service-dialling-number>+3585550000020</service-dialling-number>
        <voice-mailbox-number>+3585550000021</voice-mailbox-number>
    </sim2>
 */
public class SimCard {

    /**
     * XML tag indicating the block of parameters related to the 1st (or only) SIM card in a dual-SIM product.
     */
    public static final String XML_ELEMENT_SIM_CARD_1 = "sim1";

    /**
     * XML tag indicating the block of parameters related to the 2nd SIM card in a dual-SIM product.
     */
    public static final String XML_ELEMENT_SIM_CARD_2 = "sim2";

    /**
     * XML tag indicating the phone number (MSISDN) associated with the SIM card.
     */
    public static final String XML_ELEMENT_PHONE_NUMBER = "phone-number";

    /**
     * XML tag indicating the operator associated with the SIM card.
     */
    public static final String XML_ELEMENT_OPERATOR = "operator";
    
    /**
     * XML tag indicating the operator code associated with the SIM card.
     */
    public static final String XML_ELEMENT_OPERATOR_CODE = "operator-code";
    
    /**
     * XML tag indicating the operator country associated with the SIM card.
     */
    public static final String XML_ELEMENT_OPERATOR_COUNTRY = "operator-country";
    
    /**
     * XML tag indicating the RF signal associated with the SIM card.
     */
    public static final String XML_ELEMENT_SIGNAL = "signal";
    
    /**
     * XML tag indicating the 1st PIN code associated with the SIM card.
     */
    public static final String XML_ELEMENT_PIN_1_CODE = "pin1";

    /**
     * XML tag indicating the 2nd PIN code associated with the SIM card.
     */
    public static final String XML_ELEMENT_PIN_2_CODE = "pin2";

    /**
     * XML tag indicating the 1st PUK code associated with the SIM card.
     */
    public static final String XML_ELEMENT_PUK_1_CODE = "puk1";

    /**
     * XML tag indicating the 2nd PUK code associated with the SIM card.
     */
    public static final String XML_ELEMENT_PUK_2_CODE = "puk2";

    /**
     * XML tag indicating the security code assigned for the SIM card.
     */
    public static final String XML_ELEMENT_SECURITY_CODE = "security-code";

    /**
     * XML tag indicating the International Mobile Subscription Identity number (IMSI) assigned for the SIM card.
     */
    public static final String XML_ELEMENT_IMSI = "imsi";

    /**
     * XML tag indicating the service dialling number assigned to the SIM card.
     */
    public static final String XML_ELEMENT_SERVICE_DIALLING_NUMBER = "service-dialling-number";

    /**
     * XML tag indicating the voice mailbox number assigned to the SIM card.
     */
    public static final String XML_ELEMENT_VOICE_MAILBOX_NUMBER = "voice-mailbox-number";

    /**
     * JSON tag indicating the block of parameters related to the 1st (or only) SIM card in a dual-SIM product.
     */
    public static final String JSON_ELEMENT_SIM_CARD_1 = "SIM1";

    /**
     * JSON tag indicating the block of parameters related to the 2nd SIM card in a dual-SIM product.
     */
    public static final String JSON_ELEMENT_SIM_CARD_2 = "SIM2";

    /**
     * JSON tag indicating the phone number (MSISDN) associated with the SIM card.
     */
    public static final String JSON_ELEMENT_PHONE_NUMBER = "PhoneNumber";

    /**
     * JSON tag indicating the 1st PIN code associated with the SIM card.
     */
    public static final String JSON_ELEMENT_PIN_1_CODE = "Pin1Code";

    /**
     * JSON tag indicating the 2nd PIN code associated with the SIM card.
     */
    public static final String JSON_ELEMENT_PIN_2_CODE = "Pin2Code";

    /**
     * JSON tag indicating the 1st PUK code associated with the SIM card.
     */
    public static final String JSON_ELEMENT_PUK_1_CODE = "Puk1Code";

    /**
     * JSON tag indicating the 2nd PUK code associated with the SIM card.
     */
    public static final String JSON_ELEMENT_PUK_2_CODE = "Puk2Code";

    /**
     * JSON tag indicating the security code assigned for the SIM card.
     */
    public static final String JSON_ELEMENT_SECURITY_CODE = "SecurityCode";

    /**
     * JSON tag indicating the International Mobile Subscription Identity number (IMSI) assigned for the SIM card.
     */
    public static final String JSON_ELEMENT_IMSI = "Imsi";

    /**
     * JSON tag indicating the service number assigned to the SIM card.
     */
    public static final String JSON_ELEMENT_SERVICE_NUMBER = "ServiceNumber";

    /**
     * JSON tag indicating the voice mail number assigned to the SIM card.
     */
    public static final String JSON_ELEMENT_VOICE_MAIL_NUMBER = "VoiceMailNumber";

    /**
     * JSON tag indicating the operator of this SIM card.
     */
    public static final String JSON_ELEMENT_OPERATOR = "Operator";
    
    /**
     * JSON tag indicating the operator code of this SIM card.
     */
    public static final String JSON_ELEMENT_OPERATOR_CODE = "OperatorCode";
    
    /**
     * JSON tag indicating the operator country of this SIM card.
     */
    public static final String JSON_ELEMENT_OPERATOR_COUNTRY = "OperatorCountry";
    
    /**
     * JSON tag indicating the signal of this SIM card.
     */
    public static final String JSON_ELEMENT_SIGNAL = "Signal";
    // Deprecated JSON elements

    /**
     * @deprecated
     * JSON tag indicating the phone number (MSISDN) associated with the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PHONE_NUMBER = "phoneNumber";

    /**
     * @deprecated
     * JSON tag indicating the 1st PIN code associated with the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PIN_1_CODE = "pin1";

    /**
     * @deprecated
     * JSON tag indicating the 2nd PIN code associated with the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PIN_2_CODE = "pin2";

    /**
     * @deprecated
     * JSON tag indicating the 1st PUK code associated with the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PUK_1_CODE = "puk1";

    /**
     * @deprecated
     * JSON tag indicating the 2nd PUK code associated with the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PUK_2_CODE = "puk2";

    /**
     * @deprecated
     * JSON tag indicating the security code assigned for the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_SECURITY_CODE = "securityCode";

    /**
     * @deprecated
     * JSON tag indicating the International Mobile Subscription Identity number (IMSI) assigned for the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_IMSI = "imsi";

    /**
     * @deprecated
     * JSON tag indicating the service dialling number assigned to the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_SERVICE_DIALLING_NUMBER = "serviceDiallingNumber";

    /**
     * @deprecated
     * JSON tag indicating the voice mailbox number assigned to the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_VOICE_MAILBOX_NUMBER = "voiceMailboxNumber";
    
    /**
     * @deprecated
     * JSON tag indicating the voice operator assigned to the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_OPERATOR = "operator";
    
    /**
     * @deprecated
     * JSON tag indicating the operator assigned to the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_OPERATOR_CODE = "operatorCode";
    
    /**
     * @deprecated
     * JSON tag indicating the operator country assigned to the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_OPERATOR_COUNTRY = "operatorCountry";
    
    /**
     * @deprecated
     * JSON tag indicating the signal assigned to the SIM card.
     */
    public static final String DEPRECATED_JSON_ELEMENT_SIGNAL = "signal";

    /**
     * Identificator of the SIM card.
     * Identificator of the SIM card can be only XML_ELEMENT_SIM_CARD_1 or XML_ELEMENT_SIM_CARD_2.
     */
    private String identificator = XML_ELEMENT_SIM_CARD_1;

    /**
     * Phone number (MSISDN) associated with the SIM card.
     */
    private String phoneNumber = "";

    /**
     * The 1st PIN code associated with the SIM card.
     * The value "1234" is specified here as default value for the Nokia SIM cards.
     */
    private String pin1Code = "1234";

    /**
     * The 2nd PIN code associated with the SIM card.
     * The value "2222" is specified here as default value for the Nokia SIM cards.
     */
    private String pin2Code = "2222";

    /**
     * The 1st PUK code associated with the SIM card.
     * The value "11111111" is specified here as default value for the Nokia SIM cards.
     */
    private String puk1Code = "11111111";

    /**
     * The 2nd PUK code associated with the SIM card.
     * The value "22222222" is specified here as default value for the Nokia SIM cards.
     */
    private String puk2Code = "22222222";

    /**
     * The security code associated with the SIM card.
     * The value "12345" is specified here as default value for the Nokia SIM cards.
     */
    private String securityCode = "12345";

    /**
     * The IMSI number associated with the SIM card.
     */
    private String imsi = "";

    /**
     * Service dialling number assigned to the SIM card.
     */
    private String serviceDiallingNumber = "";

    /**
     * Voice mailbox number assigned to the SIM card.
     */
    private String voiceMailboxNumber = "";

    /**
     * Operator Name.
     */
    private String operator;
    
    /**
     * Operator code, e.g.: NokiaBTN 46013
     */
    private String operatorCode;
    
    /**
     * Operator Country, e.g.:cn
     */
    private String operatorCountry;
    
    /**
     * RF Signal. normally it should be dBm value, but sometimes it will be aus.
     * dBm =-113+2*asu.  Normal Scope, dBM: -90 ~ -51, bigger is better. When dBm<=-100 , almost out of signal. 
     * e.g.: 71dBm or 23asu.
     */
    private String signal;
    
    /**
     * Parametrized constructor.
     * Identificator of the SIM card can be only one of the following values:
     * XML_ELEMENT_SIM_CARD_1 or XML_ELEMENT_SIM_CARD_2.
     *
     * @param identificator Identificator of the SIM card
     */
    public SimCard(String identificator) {
        if (identificator != null && !identificator.isEmpty()) {
            this.identificator = identificator;
        }
    }

    /**
     * Returns identificator of this SIM card.
     *
     * @return Identificator of this SIM card
     */
    public String getIdentificator() {
        return identificator;
    }

    /**
     * Sets the phone number (MSISDN) associated with this SIM card.
     *
     * @param phoneNumber Phone number (MSISDN) associated with this SIM card
     */
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    /**
     * Returns the phone number (MSISDN) associated with this SIM card.
     *
     * @return Phone number (MSISDN) associated with this SIM card
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the 1st PIN code assigned to this SIM card.
     *
     * @param pin1Code The 1st PIN code assigned to this SIM card
     */
    public void setPin1Code(String pin1Code) {
        if (pin1Code != null) {
            this.pin1Code = pin1Code;
        }
    }

    /**
     * Returns the 1st PIN code assigned to this SIM card.
     *
     * @return The 1st PIN code assigned to this SIM card
     */
    public String getPin1Code() {
        return pin1Code;
    }

    /**
     * Sets the 2nd PIN code assigned to this SIM card.
     *
     * @param pin2Code The 2nd PIN code assigned to this SIM card
     */
    public void setPin2Code(String pin2Code) {
        if (pin2Code != null) {
            this.pin2Code = pin2Code;
        }
    }

    /**
     * Returns the 2nd PIN code assigned to this SIM card.
     *
     * @return The 2nd PIN code assigned to this SIM card
     */
    public String getPin2Code() {
        return pin2Code;
    }

    /**
     * Sets the 1st PUK code assigned to this SIM card.
     *
     * @param puk1Code The 1st PUK code assigned to this SIM card
     */
    public void setPuk1Code(String puk1Code) {
        if (puk1Code != null) {
            this.puk1Code = puk1Code;
        }
    }

    /**
     * Returns the 1st PUK code assigned to this SIM card.
     *
     * @return The 1st PUK code assigned to this SIM card
     */
    public String getPuk1Code() {
        return puk1Code;
    }

    /**
     * Sets the 2nd PUK code assigned to this SIM card.
     *
     * @param puk2Code The 2nd PUK code assigned to this SIM card
     */
    public void setPuk2Code(String puk2Code) {
        if (puk2Code != null) {
            this.puk2Code = puk2Code;
        }
    }

    /**
     * Returns the 2nd PUK code assigned to this SIM card.
     *
     * @return The 2nd PUK code assigned to this SIM card
     */
    public String getPuk2Code() {
        return puk2Code;
    }

    /**
     * Sets security code assigned to this SIM card.
     *
     * @param securityCode Security code assigned to this SIM card
     */
    public void setSecurityCode(String securityCode) {
        if (securityCode != null) {
            this.securityCode = securityCode;
        }
    }

    /**
     * Returns security code assigned to this SIM card.
     *
     * @return Security code assigned to this SIM card
     */
    public String getSecurityCode() {
        return securityCode;
    }

    /**
     * Sets IMSI assigned to this SIM card.
     *
     * @param imsi IMSI assigned to this SIM card
     */
    public void setIMSI(String imsi) {
        if (imsi != null) {
            this.imsi = imsi;
        }
    }

    /**
     * Returns IMSI assigned to this SIM card.
     *
     * @return IMSI assigned to this SIM card
     */
    public String getIMSI() {
        return imsi;
    }

    /**
     * Sets service dialling number assigned to the SIM card.
     *
     * @param serviceDiallingNumber Service dialling number assigned to the SIM card
     */
    public void setServiceDiallingNumber(String serviceDiallingNumber) {
        if (serviceDiallingNumber != null) {
            this.serviceDiallingNumber = serviceDiallingNumber;
        }
    }

    /**
     * Returns service dialling number assigned to the SIM card.
     *
     * @return Service dialling number assigned to the SIM card
     */
    public String getServiceDiallingNumber() {
        return serviceDiallingNumber;
    }

    /**
     * Sets voice mailbox number assigned to the SIM card.
     *
     * @param voiceMailboxNumber Voice mailbox number assigned to the SIM card
     */
    public void setVoiceMailboxNumber(String voiceMailboxNumber) {
        if (voiceMailboxNumber != null) {
            this.voiceMailboxNumber = voiceMailboxNumber;
        }
    }

    /**
     * Returns voice mailbox number assigned to the SIM card.
     *
     * @return Voice mailbox number assigned to the SIM card
     */
    public String getVoiceMailboxNumber() {
        return voiceMailboxNumber;
    }

    public String getOperator() {
		return operator;
	}

	public void setOperator( String operator ) {
		this.operator = operator;
	}

	public String getOperatorCode() {
		return operatorCode;
	}

	public void setOperatorCode( String operatorCode ) {
		this.operatorCode = operatorCode;
	}

	public String getOperatorCountry() {
		return operatorCountry;
	}

	public void setOperatorCountry( String operatorCountry ) {
		this.operatorCountry = operatorCountry;
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal( String signalInfo ) {
		this.signal = signalInfo;
	}

	/**
     * Comparse this SIM card to a specified one.
     * Returns true if both SIM cards contain the same data, or false otherwise.
     *
     * @param simCard SIM card to be compared against
     * @return True if both SIM cards contain the same data, or false otherwise
     */
    public boolean equals(SimCard simCard) {

        if (!phoneNumber.equals(simCard.getPhoneNumber())) {
            return false;
        }

        if (!pin1Code.equals(simCard.getPin1Code())) {
            return false;
        }

        if (!pin2Code.equals(simCard.getPin2Code())) {
            return false;
        }

        if (!puk1Code.equals(simCard.getPuk1Code())) {
            return false;
        }

        if (!puk2Code.equals(simCard.getPuk2Code())) {
            return false;
        }

        if (!securityCode.equals(simCard.getSecurityCode())) {
            return false;
        }

        if (!imsi.equals(simCard.getIMSI())) {
            return false;
        }

        if (!serviceDiallingNumber.equals(simCard.getServiceDiallingNumber())) {
            return false;
        }

        if (!voiceMailboxNumber.equals(simCard.getVoiceMailboxNumber())) {
            return false;
        }

        return true;
    }

    /**
     * Checks if this SIM card is valid or not.
     *
     * @return Returns false if any of SIM card parameters is empty.
     */
    public boolean isValid() {
        if (phoneNumber.isEmpty()) {
            // If phone number is not specified,
            // the SIM card is considered as not a used one
            return true;
        }

        if (pin1Code.isEmpty()) {
            return false;
        }

        if (pin2Code.isEmpty()) {
            return false;
        }

        if (puk1Code.isEmpty()) {
            return false;
        }

        if (puk2Code.isEmpty()) {
            return false;
        }

        if (securityCode.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Returns a textual representation of the object.
     *
     * @return A textual representation of the object
     */
    @Override
    public String toString() {

        StringBuilder string = new StringBuilder();

        string.append(" SIM card \"" + identificator + "\":");

        string.append("\n\t Phone number:   " + phoneNumber);
        string.append("\n\t Operator  :     " + operator);
        string.append("\n\t Operator code:  " + operatorCode);
        string.append("\n\t Operatorcountry:" + operatorCountry);
        string.append("\n\t Signal          :"+ signal);
        string.append("\n\t PIN 1 code:     " + pin1Code);
        string.append("\n\t PIN 2 code:     " + pin2Code);
        string.append("\n\t PUK 1 code:     " + puk1Code);
        string.append("\n\t PUK 2 code:     " + puk2Code);
        string.append("\n\t Security code:  " + securityCode);
        string.append("\n\t IMSI:           " + imsi);
        string.append("\n\t Service number: " + serviceDiallingNumber);
        string.append("\n\t Voice mailbox:  " + voiceMailboxNumber);

        return string.toString();
    }

    /**
     * Returns XML representation of the SIM card.
     *
     * @return XML representation of the SIM card
     */
    public String toXML() {
        return toXML("");
    }

    /**
     * Returns JSON representation of the SIM card.
     *
     * @return JSON representation of the SIM card
     */
    public String toJSON() {
        return toJSON("");
    }

    /**
     * @deprecated
     * Returns JSON representation of the SIM card.
     *
     * @return JSON representation of the SIM card
     */
    public String toDeprecatedJSON() {
        return toDeprecatedJSON("");
    }

    /**
     * Returns XML representation of the SIM card with specified indentation.
     *
     * @param indentation Indentation to be used in XML outputs
     * @return XML representation of the SIM card with specified indentation
     */
    public String toXML(String indentation) {
        StringBuilder xml = new StringBuilder();

        xml.append(indentation + "<" + identificator + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_PHONE_NUMBER + ">" + phoneNumber + "</" + XML_ELEMENT_PHONE_NUMBER + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_OPERATOR + ">" + operator + "</" + XML_ELEMENT_OPERATOR + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_OPERATOR_CODE + ">" + operatorCode + "</" + XML_ELEMENT_OPERATOR_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_OPERATOR_COUNTRY + ">" + operatorCountry + "</" + XML_ELEMENT_OPERATOR_COUNTRY + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_SIGNAL + ">" + signal + "</" + XML_ELEMENT_SIGNAL + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_PIN_1_CODE + ">" + pin1Code + "</" + XML_ELEMENT_PIN_1_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_PIN_2_CODE + ">" + pin2Code + "</" + XML_ELEMENT_PIN_2_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_PUK_1_CODE + ">" + puk1Code + "</" + XML_ELEMENT_PUK_1_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_PUK_2_CODE + ">" + puk2Code + "</" + XML_ELEMENT_PUK_2_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_SECURITY_CODE + ">" + securityCode + "</" + XML_ELEMENT_SECURITY_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_IMSI + ">" + imsi + "</" + XML_ELEMENT_IMSI + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_SERVICE_DIALLING_NUMBER + ">" + serviceDiallingNumber + "</" + XML_ELEMENT_SERVICE_DIALLING_NUMBER + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_VOICE_MAILBOX_NUMBER + ">" + voiceMailboxNumber + "</" + XML_ELEMENT_VOICE_MAILBOX_NUMBER + ">\n");

        xml.append(indentation + "</" + identificator + ">");

        return xml.toString();
    }

    /**
     * Returns JSON representation of the SIM card with specified indentation.
     *
     * @param indentation Indentation to be used in JSON outputs
     * @return JSON representation of the SIM card with specified indentation
     */
    public String toJSON(String indentation) {
        StringBuilder json = new StringBuilder();

        String id = JSON_ELEMENT_SIM_CARD_1;

        if (identificator.equalsIgnoreCase(XML_ELEMENT_SIM_CARD_2)) {
            id = JSON_ELEMENT_SIM_CARD_2;
        }

        json.append(indentation + "\t\"" + id + JSON_ELEMENT_PHONE_NUMBER + "\":\"" + phoneNumber + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_OPERATOR + "\":\"" + operator + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_OPERATOR_CODE + "\":\"" + operatorCode + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_OPERATOR_COUNTRY + "\":\"" + operatorCountry + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_SIGNAL + "\":\"" + signal + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_PIN_1_CODE + "\":\"" + pin1Code + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_PIN_2_CODE + "\":\"" + pin2Code + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_PUK_1_CODE + "\":\"" + puk1Code + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_PUK_2_CODE + "\":\"" + puk2Code + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_SECURITY_CODE + "\":\"" + securityCode + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_IMSI + "\":\"" + imsi + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_SERVICE_NUMBER + "\":\"" + serviceDiallingNumber + "\",\n");
        json.append(indentation + "\t\"" + id + JSON_ELEMENT_VOICE_MAIL_NUMBER + "\":\"" + voiceMailboxNumber + "\"");

        return json.toString();
    }

    /**
     * Returns a list of parameter names used to build name-value pairs.
     * Each parameter name will have the Constant.NAME_VALUE_SEPARATOR as a suffix
     * for sufficient separation of similar look a like parameter names,
     * like "status" and "status details", "reservation-time" or "reservation-timeout", etc.
     *
     * @return A list of parameter names used to build name-value pairs
     */
    public List<String> getParameterNames() {
        List<String> nameTokens = new ArrayList<String>(0);

        nameTokens.add(identificator + "-" + XML_ELEMENT_PHONE_NUMBER + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_OPERATOR + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_OPERATOR_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_OPERATOR_COUNTRY + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_PIN_1_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_PIN_2_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_PUK_1_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_PUK_2_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_SECURITY_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_IMSI + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_SERVICE_DIALLING_NUMBER + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(identificator + "-" + XML_ELEMENT_VOICE_MAILBOX_NUMBER + Constant.NAME_VALUE_SEPARATOR);

        return nameTokens;
    }

    /**
     * Returns a string containing representation of this SIM card in form of "parameter name"-"parameter value" pairs.
     * Each "parameter name" is separated from the "parameter value" with the Constant.NAME_VALUE_SEPARATOR symbol,
     * while "parameter name"-"parameter value" pairs are separated from each other with the Constant.NAME_VALUE_PAIR_SEPARATOR symbol.
     *
     * @return A string containing representation of this SIM card in form of "parameter name"-"parameter value" pairs
     */
    public String getNameValuePairsRepresentation() {
        StringBuilder representation = new StringBuilder();

        // Here each "name" is extdended with the SIM card identificator: either "sim1" or "sim2"

        representation.append(identificator + "-" + XML_ELEMENT_PHONE_NUMBER + Constant.NAME_VALUE_SEPARATOR + phoneNumber + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_OPERATOR + Constant.NAME_VALUE_SEPARATOR + operator + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_OPERATOR_CODE + Constant.NAME_VALUE_SEPARATOR + operatorCode + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_OPERATOR_COUNTRY + Constant.NAME_VALUE_SEPARATOR + operatorCountry + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_PIN_1_CODE + Constant.NAME_VALUE_SEPARATOR + pin1Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_PIN_2_CODE + Constant.NAME_VALUE_SEPARATOR + pin2Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_PUK_1_CODE + Constant.NAME_VALUE_SEPARATOR + puk1Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_PUK_2_CODE + Constant.NAME_VALUE_SEPARATOR + puk2Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_SECURITY_CODE + Constant.NAME_VALUE_SEPARATOR + securityCode + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_IMSI + Constant.NAME_VALUE_SEPARATOR + imsi + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_SERVICE_DIALLING_NUMBER + Constant.NAME_VALUE_SEPARATOR + serviceDiallingNumber + Constant.NAME_VALUE_PAIR_SEPARATOR);
        representation.append(identificator + "-" + XML_ELEMENT_VOICE_MAILBOX_NUMBER + Constant.NAME_VALUE_SEPARATOR + voiceMailboxNumber + Constant.NAME_VALUE_PAIR_SEPARATOR);

        return representation.toString();
    }

    /**
     * Returns a string containing representation of this SIM card in form of "parameter name"-"parameter value" pairs
     * suitable for search of matching SIM cards out of testing farm.
     * Each "parameter name" is separated from the "parameter value" with the Constant.NAME_VALUE_SEPARATOR symbol,
     * while "parameter name"-"parameter value" pairs are separated from each other with the Constant.NAME_VALUE_PAIR_SEPARATOR symbol.
     *
     * @return A string containing representation of this SIM card in form of "parameter name"-"parameter value" pairs
     * suitable for search of matching SIM cards out of testing farm
     */
    public String getNameValuePairs() {
        StringBuilder representation = new StringBuilder();

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            representation.append(identificator + "-" + XML_ELEMENT_PHONE_NUMBER + Constant.NAME_VALUE_SEPARATOR + phoneNumber + Constant.NAME_VALUE_PAIR_SEPARATOR);
            
            if (operator != null && !operator.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_OPERATOR + Constant.NAME_VALUE_SEPARATOR + operator + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }
            
            if (operatorCode != null && !operatorCode.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_OPERATOR_CODE + Constant.NAME_VALUE_SEPARATOR + operatorCode + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }
            
            if (operatorCountry != null && !operatorCountry.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_OPERATOR_COUNTRY + Constant.NAME_VALUE_SEPARATOR + operatorCountry + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }
            
            if (signal != null && !signal.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_SIGNAL + Constant.NAME_VALUE_SEPARATOR + signal + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }
            
            if (pin1Code != null && !pin1Code.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_PIN_1_CODE + Constant.NAME_VALUE_SEPARATOR + pin1Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }

            if (pin2Code != null && !pin2Code.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_PIN_2_CODE + Constant.NAME_VALUE_SEPARATOR + pin2Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }

            if (puk1Code != null && !puk1Code.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_PUK_1_CODE + Constant.NAME_VALUE_SEPARATOR + puk1Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }

            if (puk2Code != null && !puk2Code.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_PUK_2_CODE + Constant.NAME_VALUE_SEPARATOR + puk2Code + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }

            if (securityCode != null && !securityCode.isEmpty()) {
                representation.append(identificator + "-" + XML_ELEMENT_SECURITY_CODE + Constant.NAME_VALUE_SEPARATOR + securityCode + Constant.NAME_VALUE_PAIR_SEPARATOR);
            }
        }

        if (imsi != null && !imsi.isEmpty()) {
            representation.append(identificator + "-" + XML_ELEMENT_IMSI + Constant.NAME_VALUE_SEPARATOR + imsi + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (serviceDiallingNumber != null && !serviceDiallingNumber.isEmpty()) {
            representation.append(identificator + "-" + XML_ELEMENT_SERVICE_DIALLING_NUMBER + Constant.NAME_VALUE_SEPARATOR + serviceDiallingNumber + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (voiceMailboxNumber != null && !voiceMailboxNumber.isEmpty()) {
            representation.append(identificator + "-" + XML_ELEMENT_VOICE_MAILBOX_NUMBER + Constant.NAME_VALUE_SEPARATOR + voiceMailboxNumber + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        return representation.toString();
    }

    /**
     * @deprecated
     * Returns JSON representation of the SIM card with specified indentation.
     *
     * @param indentation Indentation to be used in JSON outputs
     * @return JSON representation of the SIM card with specified indentation
     */
    public String toDeprecatedJSON(String indentation) {
        StringBuilder json = new StringBuilder();

        json.append(indentation + "\"" + identificator + "\":{\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PHONE_NUMBER + "\":\"" + phoneNumber + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_OPERATOR + "\":\"" + operator + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_OPERATOR_CODE + "\":\"" + operatorCode + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_OPERATOR_COUNTRY + "\":\"" + operatorCountry + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_SIGNAL + "\":\"" + signal + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PIN_1_CODE + "\":\"" + pin1Code + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PIN_2_CODE + "\":\"" + pin2Code + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PUK_1_CODE + "\":\"" + puk1Code + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PUK_2_CODE + "\":\"" + puk2Code + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_SECURITY_CODE + "\":\"" + securityCode + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_IMSI + "\":\"" + imsi + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_SERVICE_DIALLING_NUMBER + "\":\"" + serviceDiallingNumber + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_VOICE_MAILBOX_NUMBER + "\":\"" + voiceMailboxNumber + "\"\n");
        json.append(indentation + "\t}");

        return json.toString();
    }
}


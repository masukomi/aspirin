/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.mailet;

import java.util.Locale;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.ParseException;

/**
 * A representation of an email address.
 * <p>This class encapsulates functionalities to access to different
 * parts of an email address without dealing with its parsing.</p>
 * 
 * <p>A MailAddress is an address specified in the MAIL FROM and
 * RCPT TO commands in SMTP sessions.  These are either passed by
 * an external server to the mailet-compliant SMTP server, or they
 * are created programmatically by the mailet-compliant server to
 * send to another (external) SMTP server.  Mailets and matchers
 * use the MailAddress for the purpose of evaluating the sender
 * and recipient(s) of a message.</p>
 * 
 * <p>MailAddress parses an email address as defined in RFC 821
 * (SMTP) p. 30 and 31 where addresses are defined in BNF convention.
 * As the mailet API does not support the aged "SMTP-relayed mail"
 * addressing protocol, this leaves all addresses to be a <mailbox>,
 * as per the spec.  The MailAddress's "user" is the <local-part> of
 * the <mailbox> and "host" is the <domain> of the mailbox.</p>
 * 
 * <p>This class is a good way to validate email addresses as there are
 * some valid addresses which would fail with a simpler approach
 * to parsing address.  It also removes parsing burden from
 * mailets and matchers that might not realize the flexibility of an
 * SMTP address.  For instance, "serge@home"@lokitech.com is a valid
 * SMTP address (the quoted text serge@home is the user and
 * lokitech.com is the host).  This means all current parsing to date
 * is incorrect as we just find the first @ and use that to separate
 * user from host.</p>
 * 
 * <p>This parses an address as per the BNF specification for <mailbox>
 * from RFC 821 on page 30 and 31, section 4.1.2. COMMAND SYNTAX.
 * http://www.freesoft.org/CIE/RFC/821/15.htm</p>
 *
 * @version 1.0
 * @author Roberto Lo Giacco <rlogiacco@mail.com>
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Gabriel Bucher <gabriel.bucher@razor.ch>
 * @author Stuart Roebuck <stuart.roebuck@adolos.com>
 */
public class MailAddress implements java.io.Serializable {
    //We hardcode the serialVersionUID so that from James 1.2 on,
    //  MailAddress will be deserializable (so your mail doesn't get lost)
    public static final long serialVersionUID = 2779163542539434916L;

    private final static char[] SPECIAL =
    {'<', '>', '(', ')', '[', ']', '\\', '.', ',', ';', ':', '@', '\"'};

    private String user = null;
    private String host = null;
    //Used for parsing
    private int pos = 0;

    /**
     * <p>Construct a MailAddress parsing the provided <code>String</code> object.</p>
     *
     * <p>The <code>personal</code> variable is left empty.</p>
     *
     * @param   address the email address compliant to the RFC822 format
     * @throws  ParseException    if the parse failed
     */
    public MailAddress(String address) throws ParseException {
        address = address.trim();
        StringBuffer userSB = new StringBuffer();
        StringBuffer hostSB = new StringBuffer();
        //Begin parsing
        //<mailbox> ::= <local-part> "@" <domain>

        try {
            //parse local-part
            //<local-part> ::= <dot-string> | <quoted-string>
            if (address.charAt(pos) == '\"') {
                userSB.append(parseQuotedLocalPart(address));
            } else {
                userSB.append(parseUnquotedLocalPart(address));
            }
            if (userSB.toString().length() == 0) {
                throw new ParseException("No local-part (user account) found at position " + (pos + 1));
            }

            //find @
            if (address.charAt(pos) != '@') {
                throw new ParseException("Did not find @ between local-part and domain at position " + (pos + 1));
            }
            pos++;

            //parse domain
            //<domain> ::=  <element> | <element> "." <domain>
            //<element> ::= <name> | "#" <number> | "[" <dotnum> "]"
            while (true) {
                if (address.charAt(pos) == '#') {
                    hostSB.append(parseNumber(address));
                } else if (address.charAt(pos) == '[') {
                    hostSB.append(parseDotNum(address));
                } else {
                    hostSB.append(parseDomainName(address));
                }
                if (pos >= address.length()) {
                    break;
                }
                if (address.charAt(pos) == '.') {
                    hostSB.append('.');
                    pos++;
                    continue;
                }
                break;
            }

            if (hostSB.toString().length() == 0) {
                throw new ParseException("No domain found at position " + (pos + 1));
            }
        } catch (IndexOutOfBoundsException ioobe) {
            throw new ParseException("Out of data at position " + (pos + 1));
        }

        user = userSB.toString();
        host = hostSB.toString();
    }

    /**
     * Construct a MailAddress with the provided personal name and email
     * address.
     *
     * @param   user        the username or account name on the mail server
     * @param   host        the server that should accept messages for this user
     * @throws  ParseException    if the parse failed
     */
    public MailAddress(String newUser, String newHost) throws ParseException {
        /* NEEDS TO BE REWORKED TO VALIDATE EACH CHAR */
        user = newUser;
        host = newHost;
    }

    /**
     * Constructs a MailAddress from a JavaMail InternetAddress, using only the
     * email address portion, discarding the personal name.
     */
    public MailAddress(InternetAddress address) throws ParseException {
        this(address.getAddress());
    }

    /**
     * Return the host part.
     *
     * @return  a <code>String</code> object representing the host part
     *          of this email address. If the host is of the dotNum form
     *          (e.g. [yyy.yyy.yyy.yyy]) then strip the braces first.
     */
    public String getHost() {
        if (!(host.startsWith("[") && host.endsWith("]"))) {
            return host;
        } else {
            return host.substring(1, host.length() -1);
        }
    }

    /**
     * Return the user part.
     *
     * @return  a <code>String</code> object representing the user part
     *          of this email address.
     * @throws  AddressException    if the parse failed
     */
    public String getUser() {
        return user;
    }

    public String toString() {
        StringBuffer addressBuffer = 
            new StringBuffer(128)
                    .append(user)
                    .append("@")
                    .append(host);
        return addressBuffer.toString();
    }

    public InternetAddress toInternetAddress() {
        try {
            return new InternetAddress(toString());
        } catch (javax.mail.internet.AddressException ae) {
            //impossible really
            return null;
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof String) {
            String theString = (String)obj;
            return toString().equalsIgnoreCase(theString);
        } else if (obj instanceof MailAddress) {
            MailAddress addr = (MailAddress)obj;
            return getUser().equalsIgnoreCase(addr.getUser()) && getHost().equalsIgnoreCase(addr.getHost());
        }
        return false;
    }

    /**
     * Return a hashCode for this object which should be identical for addresses
     * which are equivalent.  This is implemented by obtaining the default
     * hashcode of the String representation of the MailAddress.  Without this
     * explicit definition, the default hashCode will create different hashcodes
     * for separate object instances.
     *
     * @return the hashcode.
     */
    public int hashCode() {
        return toString().toLowerCase(Locale.US).hashCode();
    }

    private String parseQuotedLocalPart(String address) throws ParseException {
        StringBuffer resultSB = new StringBuffer();
        resultSB.append('\"');
        pos++;
        //<quoted-string> ::=  """ <qtext> """
        //<qtext> ::=  "\" <x> | "\" <x> <qtext> | <q> | <q> <qtext>
        while (true) {
            if (address.charAt(pos) == '\"') {
                resultSB.append('\"');
                //end of quoted string... move forward
                pos++;
                break;
            }
            if (address.charAt(pos) == '\\') {
                resultSB.append('\\');
                pos++;
                //<x> ::= any one of the 128 ASCII characters (no exceptions)
                char x = address.charAt(pos);
                if (x < 0 || x > 128) {
                    throw new ParseException("Invalid \\ syntaxed character at position " + (pos + 1));
                }
                resultSB.append(x);
                pos++;
            } else {
                //<q> ::= any one of the 128 ASCII characters except <CR>,
                //<LF>, quote ("), or backslash (\)
                char q = address.charAt(pos);
                if (q <= 0 || q == '\n' || q == '\r' || q == '\"' || q == '\\') {
                    throw new ParseException("Unquoted local-part (user account) must be one of the 128 ASCI characters exception <CR>, <LF>, quote (\"), or backslash (\\) at position " + (pos + 1));
                }
                resultSB.append(q);
                pos++;
            }
        }
        return resultSB.toString();
    }

    private String parseUnquotedLocalPart(String address) throws ParseException {
        StringBuffer resultSB = new StringBuffer();
        //<dot-string> ::= <string> | <string> "." <dot-string>
        boolean lastCharDot = false;
        while (true) {
            //<string> ::= <char> | <char> <string>
            //<char> ::= <c> | "\" <x>
            if (address.charAt(pos) == '\\') {
                resultSB.append('\\');
                pos++;
                //<x> ::= any one of the 128 ASCII characters (no exceptions)
                char x = address.charAt(pos);
                if (x < 0 || x > 128) {
                    throw new ParseException("Invalid \\ syntaxed character at position " + (pos + 1));
                }
                resultSB.append(x);
                pos++;
                lastCharDot = false;
            } else if (address.charAt(pos) == '.') {
                resultSB.append('.');
                pos++;
                lastCharDot = true;
            } else if (address.charAt(pos) == '@') {
                //End of local-part
                break;
            } else {
                //<c> ::= any one of the 128 ASCII characters, but not any
                //    <special> or <SP>
                //<special> ::= "<" | ">" | "(" | ")" | "[" | "]" | "\" | "."
                //    | "," | ";" | ":" | "@"  """ | the control
                //    characters (ASCII codes 0 through 31 inclusive and
                //    127)
                //<SP> ::= the space character (ASCII code 32)
                char c = address.charAt(pos);
                if (c <= 31 || c == 127 || c == ' ') {
                    throw new ParseException("Invalid character in local-part (user account) at position " + (pos + 1));
                }
                for (int i = 0; i < SPECIAL.length; i++) {
                    if (c == SPECIAL[i]) {
                        throw new ParseException("Invalid character in local-part (user account) at position " + (pos + 1));
                    }
                }
                resultSB.append(c);
                pos++;
                lastCharDot = false;
            }
        }
        if (lastCharDot) {
            throw new ParseException("local-part (user account) ended with a \".\", which is invalid.");
        }
        return resultSB.toString();
    }

    private String parseNumber(String address) throws ParseException {
        //<number> ::= <d> | <d> <number>

        StringBuffer resultSB = new StringBuffer();
        //We keep the position from the class level pos field
        while (true) {
            if (pos >= address.length()) {
                break;
            }
            //<d> ::= any one of the ten digits 0 through 9
            char d = address.charAt(pos);
            if (d == '.') {
                break;
            }
            if (d < '0' || d > '9') {
                throw new ParseException("In domain, did not find a number in # address at position " + (pos + 1));
            }
            resultSB.append(d);
            pos++;
        }
        return resultSB.toString();
    }

    private String parseDotNum(String address) throws ParseException {
        //throw away all irrelevant '\' they're not necessary for escaping of '.' or digits, and are illegal as part of the domain-literal
        while(address.indexOf("\\")>-1){
             address= address.substring(0,address.indexOf("\\")) + address.substring(address.indexOf("\\")+1);
        }
        StringBuffer resultSB = new StringBuffer();
        //we were passed the string with pos pointing the the [ char.
        // take the first char ([), put it in the result buffer and increment pos
        resultSB.append(address.charAt(pos));
        pos++;

        //<dotnum> ::= <snum> "." <snum> "." <snum> "." <snum>
        for (int octet = 0; octet < 4; octet++) {
            //<snum> ::= one, two, or three digits representing a decimal
            //                      integer value in the range 0 through 255
            //<d> ::= any one of the ten digits 0 through 9
            StringBuffer snumSB = new StringBuffer();
            for (int digits = 0; digits < 3; digits++) {
                char d = address.charAt(pos);
                if (d == '.') {
                    break;
                }
                if (d == ']') {
                    break;
                }
                if (d < '0' || d > '9') {
                    throw new ParseException("Invalid number at position " + (pos + 1));
                }
                snumSB.append(d);
                pos++;
            }
            if (snumSB.toString().length() == 0) {
                throw new ParseException("Number not found at position " + (pos + 1));
            }
            try {
                int snum = Integer.parseInt(snumSB.toString());
                if (snum > 255) {
                    throw new ParseException("Invalid number at position " + (pos + 1));
                }
            } catch (NumberFormatException nfe) {
                throw new ParseException("Invalid number at position " + (pos + 1));
            }
            resultSB.append(snumSB.toString());
            if (address.charAt(pos) == ']') {
                if (octet < 3) {
                    throw new ParseException("End of number reached too quickly at " + (pos + 1));
                } else {
                    break;
                }
            }
            if (address.charAt(pos) == '.') {
                resultSB.append('.');
                pos++;
            }
        }
        if (address.charAt(pos) != ']') {
            throw new ParseException("Did not find closing bracket \"]\" in domain at position " + (pos + 1));
        }
        resultSB.append(']');
        pos++;
        return resultSB.toString();
    }

    private String parseDomainName(String address) throws ParseException {
        StringBuffer resultSB = new StringBuffer();
        //<name> ::= <a> <ldh-str> <let-dig>
        //<ldh-str> ::= <let-dig-hyp> | <let-dig-hyp> <ldh-str>
        //<let-dig> ::= <a> | <d>
        //<let-dig-hyp> ::= <a> | <d> | "-"
        //<a> ::= any one of the 52 alphabetic characters A through Z
        //  in upper case and a through z in lower case
        //<d> ::= any one of the ten digits 0 through 9

        // basically, this is a series of letters, digits, and hyphens,
        // but it can't start with a digit or hypthen
        // and can't end with a hyphen

        // in practice though, we should relax this as domain names can start
        // with digits as well as letters.  So only check that doesn't start
        // or end with hyphen.
        while (true) {
            if (pos >= address.length()) {
                break;
            }
            char ch = address.charAt(pos);
            if ((ch >= '0' && ch <= '9') || 
                (ch >= 'a' && ch <= 'z') || 
                (ch >= 'A' && ch <= 'Z') ||
                (ch == '-')) {
                resultSB.append(ch);
                pos++;
                continue;
            }
            if (ch == '.') {
                break;
            }
            throw new ParseException("Invalid character at " + pos);
        }
        String result = resultSB.toString();
        if (result.startsWith("-") || result.endsWith("-")) {
            throw new ParseException("Domain name cannot begin or end with a hyphen \"-\" at position " + (pos + 1));
        }
        return result;
    }
}

package com.github.tsutomunakamura.tokenha.element;

public class TokenElement {

    private String token;
    private long timeMillis;

    public TokenElement(String token, long timeMillis) {
        this.token = token;
        this.timeMillis = timeMillis;
    }

    public long getTimeMillis() {
        return timeMillis;
    }
    public String getToken() {
        return token;
    }
    
    /**
     * Serialize TokenElement to simple JSON.
     * @return JSON string representation
     */
    public String toJson() {
        return "{\"token\":\"" + escapeJsonString(token) + "\",\"timeMillis\":" + timeMillis + "}";
    }
    
    /**
     * Escape special characters for JSON string values.
     * @param str string to escape
     * @return escaped string
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
}

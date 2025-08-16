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
}

package com.miapp.mediastreaming.model;

public class MediaServerRequest {
    private String serverName;
    private String ipAddress;
    private Integer port; // Opcional, el cliente puede enviarlo o dejarlo null

    // Constructores
    public MediaServerRequest() {}
    public MediaServerRequest(String serverName, String ipAddress, Integer port) {
        this.serverName = serverName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    // Getters y setters
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
}

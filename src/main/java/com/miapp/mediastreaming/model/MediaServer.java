package com.miapp.mediastreaming.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "media_servers")
public class MediaServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ipAddress;

    private int port; // Puerto para streaming
    private String streamKey; // Clave única para el stream
    private String status; // ONLINE, OFFLINE

    @ManyToOne
    @JsonBackReference
    private User user;

    // Constructores
    public MediaServer() {}
    public MediaServer(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.status = "OFFLINE"; // Por defecto
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
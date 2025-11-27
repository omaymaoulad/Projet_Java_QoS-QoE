package com.ensah.qoe.Models;

import java.time.LocalDateTime;


public class Feedback {

    private int id;
    private String clientName;
    private String service;
    private Double score;
    private String commentaire;
    private LocalDateTime date;

    public Feedback() {}

    // getters & setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}

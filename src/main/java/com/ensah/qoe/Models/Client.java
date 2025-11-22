package com.ensah.qoe.Models;

public class Client {
    private int idClient;
    private String nom;
    private String genre;
    private String telephone;
    private String localisationZone;
    private int estUserApp;

    public Client(int idClient, String nom, String genre, String telephone, String localisationZone, int estUserApp) {
        this.idClient = idClient;
        this.nom = nom;
        this.genre = genre;
        this.telephone = telephone;
        this.localisationZone = localisationZone;
        this.estUserApp = estUserApp;
    }
    public int getIdClient() {return idClient;}
    public String getNom() {return nom;}
    public String getGenre() {return genre;}
    public String getTelephone() {return telephone;}
    public String getLocalisationZone() {return localisationZone;}
    public int getEstUserApp() {return estUserApp;}

    public void setIdClient(int idClient) {this.idClient = idClient;}
    public void setNom(String nom) {this.nom = nom;}
    public void setGenre(String genre) {this.genre = genre;}
    public void setTelephone(String telephone) {this.telephone = telephone;}
    public void setLocalisationZone(String localisationZone) {this.localisationZone = localisationZone;}
    public void setEstUserApp(int estUserApp) {this.estUserApp = estUserApp;}
}

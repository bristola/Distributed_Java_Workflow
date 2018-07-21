package com.web;

public class Machine {

    private String name;
    private String id;
    private String ip;
    private boolean inUse = false;

    public Machine() {
        name = "";
        id = "";
        ip = "";
    }

    public Machine(String name) {
        this.name = name;
    }

    public Machine(String name, String id, String ip) {
        this.name = name;
        this.id = id;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return id;
    }

    public String getIP() {
        return ip;
    }

    public boolean getInUse() {
        return inUse;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    @Override
    public String toString() {
        return "(" +name+" | " +id+ " | " +ip+ ")";
    }

    @Override
    public boolean equals(Object obj) {
        Machine m = (Machine)obj;
        return m.getName().equals(name);
    }

}

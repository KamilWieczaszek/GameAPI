package dev.kavu.gameapi;

public class Rule<E extends Enum<E>> {

    private final String name;
    private E status;

    public Rule(String name, E status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public E getStatus(){
        return status;
    }

    public boolean check(E status) {
        return this.status.equals(status);
    }

    public void setStatus(E status) {
        this.status = status;
    }

    public enum Basic {
        ON,
        OFF;
    }
}

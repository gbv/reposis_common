package de.gbv.reposis.config;

public class Property {

    private String old;
    private String _new;
    private String component;
    private String property;

    public Property(
        String component,
        String property,
        String old,
        String _new) {

        this.old = old;
        this._new = _new;
        this.component = component;
        this.property = property;
    }

    public String getOld() {
        return old;
    }

    public void setOld(String old) {
        this.old = old;
    }

    public String get_new() {
        return _new;
    }

    public void set_new(String _new) {
        this._new = _new;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}

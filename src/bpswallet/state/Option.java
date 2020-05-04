package bpswallet.state;

public abstract class Option<T extends Scene> {

    protected String key, value;
    protected T context;

    public Option(T context, String key, String value) {
        this.key = key;
        this.value = value;
        this.context = context;
    }

    public abstract void trigger();

    public boolean isAnyInput() {
        return key.isEmpty();
    }
}

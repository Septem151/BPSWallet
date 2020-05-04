package bpswallet.state;

import java.util.ArrayList;

public abstract class Scene {

    protected State state;
    protected String title;
    protected ArrayList<Option> options;
    protected boolean autoclear;

    public Scene(String title) {
        this.title = title;
        this.state = State.getInstance();
        options = new ArrayList<>();
        autoclear = true;
    }

    public abstract void display();

    public boolean processInput(String input) {
        for (Option option : options) {
            if (option.isAnyInput() || option.key.equalsIgnoreCase(input)) {
                option.trigger();
                return true;
            }
        }
        return false;
    }

    public boolean autoclears() {
        return autoclear;
    }
}

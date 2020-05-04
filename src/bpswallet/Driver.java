package bpswallet;

import bpswallet.state.State;

public class Driver {

    public static void main(String[] args) {
        State state = State.getInstance();
        state.start();
    }

}

package bpswallet;

import bpswallet.state.State;
import bpswallet.tests.*;

public class Driver {

    public static void main(String[] args) {
        BIP39Test bip39test = new BIP39Test();
        BIP32Test bip32test = new BIP32Test();
        if (bip39test.runAllTests() && bip32test.runAllTests()) {
            State state = State.getInstance();
            state.start();
        }else System.out.println("One or more tests failed.");
    }

}

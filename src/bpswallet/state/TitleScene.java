package bpswallet.state;

import bpswallet.util.FileUtil;

public class TitleScene extends Scene {

    public TitleScene() {
        super("");
        addOptions();
    }

    private void addOptions() {
        options.add(new Option<TitleScene>(this, "N", "New Wallet") {
            @Override
            public void trigger() {
                state.setScene(new NewWalletScene());
            }
        });
        options.add(new Option<TitleScene>(this, "L", "Load Wallet") {
            @Override
            public void trigger() {
                if(FileUtil.listWalletFileNames().length != 0) {
                    state.setScene(new LoadWalletScene());
                }else {
                    System.out.println("No wallet files exist.");
                    state.pause(1000);
                }
            }
        });
        options.add(new Option<TitleScene>(this, "R", "Recover Wallet") {
            @Override
            public void trigger() {
                state.setScene(new RecoverWalletScene());
            }
        });
        options.add(new Option<TitleScene>(this, "Q", "Exit BPSWallet") {
            @Override
            public void trigger() {
                state.stop();
            }
        });
    }

    @Override
    public void display() {
        for (Option option : options) {
            System.out.println("[" + option.key + "] - " + option.value);
        }
    }
}

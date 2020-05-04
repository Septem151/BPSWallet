package bpswallet.state;

public abstract class FlowScene extends Scene {

    private int step;
    private int prevStep;

    public FlowScene(String title) {
        super(title);
        step = 0;
        prevStep = -1;
    }

    protected abstract void updateOptions();

    public int getStep() {
        return step;
    }

    public void setStep(int value) {
        prevStep = step;
        step = value;
    }

    public boolean stepChanged() {
        return step != prevStep;
    }

    public void incrementStep() {
        prevStep = step;
        step++;
    }

    public void decrementStep() {
        prevStep = step;
        step--;
    }
}

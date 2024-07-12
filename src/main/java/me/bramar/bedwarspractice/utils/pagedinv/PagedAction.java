package me.bramar.bedwarspractice.utils.pagedinv;

public enum PagedAction {
    PREVIOUS(-1), NEXT(1), NONE(0);

    public final int value;
    PagedAction(int value) {
        this.value = value;
    }
}

package rege.rege.silt.parser.test;

import org.jetbrains.annotations.Contract;

public enum Nil {
    INSTANCE;

    @Contract(pure = true)
    public String toString() {
        return "nil";
    }
}

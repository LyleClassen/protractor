package com.lyleclassen.protractor.core;

/**
 * A Run of consecutive blocks along the Line's longest axis. {@code stepBefore} names the Step that
 * leads into this Run (empty for the first Run), e.g. "↑", "→z" or "↓→x".
 */
public record Run(int length, String stepBefore) {}

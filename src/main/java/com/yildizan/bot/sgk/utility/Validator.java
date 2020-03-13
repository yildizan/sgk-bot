package com.yildizan.bot.sgk.utility;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Validator {

    private Validator() {}

    public static boolean validateCommand(String text) {
        return text != null && text.startsWith("!sgk");
    }

    public static boolean validateParameter(List<String> tokens) {
        return Arrays.asList(Constants.PARAMETERS).containsAll(tokens.stream().filter(t -> t.matches("[a-zA-Z]+")).collect(Collectors.toList()));
    }

    public static boolean validateTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour() > Constants.START_HOUR &&
                now.getHour() < Constants.END_HOUR &&
                now.getDayOfWeek() != DayOfWeek.SATURDAY &&
                now.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}

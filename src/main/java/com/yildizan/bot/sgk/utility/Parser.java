package com.yildizan.bot.sgk.utility;

import com.yildizan.bot.sgk.model.User;

import java.util.List;

public class Parser {

    private Parser() {}

    public static int extractStatus(List<String> tokens) {
        try {
            switch (tokens.get(tokens.indexOf("durum") + 1)) {
                case "ben":
                    return User.SELF;
                case "ilk":
                    return User.WINNER;
                case "son":
                    return User.LOSER;
                default:
                    throw new IllegalArgumentException();
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

}

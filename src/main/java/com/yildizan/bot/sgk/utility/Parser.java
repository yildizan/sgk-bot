package com.yildizan.bot.sgk.utility;

import com.yildizan.bot.sgk.model.User;

import java.util.List;

public final class Parser {

    private Parser() {}

    public static int extractStatus(List<String> tokens) {
        try {
            final String token = tokens.get(tokens.indexOf("durum") + 1);
            if(token.equals("ben")) {
                return User.SELF;
            }
            else if(token.equals("ilk")) {
                return User.WINNER;
            }
            else if(token.equals("son")) {
                return User.LOSER;
            }
            else if(token.startsWith("<@!")) {
                return User.TAG;
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

    public static long extractId(String command) {
        try {
            return Long.parseLong(command.substring(command.indexOf('<') + 3, command.indexOf('>')));
        }
        catch (Exception e) {
            return 0;
        }
    }

}

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
            if(command.contains("<@!")) {
                return Long.parseLong(command.substring(command.lastIndexOf('!') + 1, command.indexOf('>')));
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

    public static String extractMention(String command) {
        try {
            if(command.contains("<")) {
                return command.substring(command.indexOf('<'));
            }
            else if(command.contains("@")) {
                return command.substring(command.indexOf('@'));
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        catch (Exception e) {
            return "";
        }
    }

}

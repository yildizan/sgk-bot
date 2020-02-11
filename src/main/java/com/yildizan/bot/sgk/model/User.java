package com.yildizan.bot.sgk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.OnlineStatus;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class User implements Comparable<User> {

    private String tag;
    private OnlineStatus status;
    private boolean isSaluted;
    private long timestamp;
    // in seconds
    private int totalDuration;

    public static final int SELF = 1;
    public static final int WINNER = 2;
    public static final int LOSER = 3;
    public static final int ALL = 4;

    public String toString(String emoji, int index, int total) {
        return toString(emoji, index, total, false);
    }

    public String toString(String emoji, int index, int total, boolean inline) {
        return (inline ? "" : "```") + emoji + " " + tag + ", #" + index + " of " + total + "\n" +
                "⏲: " + totalDuration / 3600 + "h " + (totalDuration % 3600) / 60 + "m " + totalDuration % 60 + "s" + (inline ? "" : "```");
    }

    @Override
    public int compareTo(@NotNull User u) {
        return u.getTotalDuration() - totalDuration;
    }
}

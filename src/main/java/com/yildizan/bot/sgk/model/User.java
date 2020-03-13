package com.yildizan.bot.sgk.model;

import com.yildizan.bot.sgk.utility.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.OnlineStatus;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@AllArgsConstructor
public class User implements Comparable<User>, Serializable {

    private String tag;
    private OnlineStatus status;
    private boolean isSaluted;
    private long timestamp;
    private long startedAt;
    // in seconds
    private int totalDuration;
    private int balance;

    public static final int SELF = 1;
    public static final int WINNER = 2;
    public static final int LOSER = 3;
    public static final int TAG = 4;
    public static final int ALL = 5;

    public void reset() {
        status = OnlineStatus.OFFLINE;
        isSaluted = false;
        timestamp = 0;
        startedAt = 0;
        totalDuration = 0;
    }

    public String toString(String emoji, int index, int total) {
        return toString(emoji, index, total, false);
    }

    public String toString(String emoji, int index, int total, boolean inline) {
        return (inline ? "" : "```") + (tag.equals(Constants.OWNER) ? "\uD83D\uDC51" : emoji) + " " + tag + ", #" + index + " of " + total + "\n" +
                "⏲: " + totalDuration / 3600 + "h " + (totalDuration % 3600) / 60 + "m " + totalDuration % 60 + "s" +
                (startedAt != 0 ? ", \uD83C\uDF1E: " + new SimpleDateFormat("HH:mm").format(new Date(startedAt)) : "") +
                ", \uD83D\uDCB2: " + balance + (inline ? "" : "```");
    }

    @Override
    public int compareTo(@Nonnull User u) {
        return u.getTotalDuration() - totalDuration;
    }
}

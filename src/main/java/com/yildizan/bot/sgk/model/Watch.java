package com.yildizan.bot.sgk.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Map;

@AllArgsConstructor
public class Watch {

    @Getter private TextChannel channel;
    @Getter private Map<Long, User> members;

}

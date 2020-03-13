package com.yildizan.bot.sgk.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

@AllArgsConstructor
public class Watch implements Serializable {

    @Getter private long channelId;
    @Getter private Map<Long, User> members;

}

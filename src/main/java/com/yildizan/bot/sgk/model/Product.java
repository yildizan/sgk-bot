package com.yildizan.bot.sgk.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Product {

    private String name;
    private String emoji;
    private String text;
    private int price;

}

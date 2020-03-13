package com.yildizan.bot.sgk.utility;

import com.yildizan.bot.sgk.model.Product;

public final class Waiter {

    private static final Product[] products = new Product[]{
            new Product("dürüm", "\uD83C\uDF2E", "şifâ olsun.", 3),
            new Product("ayran", "\uD83E\uDD5B", "can olsun.", 1)
    };

    private Waiter() {}

    public static String showMenu() {
        String menu = "```";
        for(Product product : products) {
            menu += product.getEmoji() + ' ' + product.getName() + ": \uD83D\uDCB2" + product.getPrice() + '\n';
        }
        return menu + "```";
    }

    public static Product order(int product) {
        return products[product];
    }

}

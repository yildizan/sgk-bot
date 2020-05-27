package com.yildizan.bot.sgk;

import com.yildizan.bot.sgk.model.User;
import com.yildizan.bot.sgk.model.Watch;
import com.yildizan.bot.sgk.utility.SortBy;
import com.yildizan.bot.sgk.utility.*;
import com.yildizan.bot.sgk.model.Product;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Core extends ListenerAdapter {

    private ScheduledFuture<?> future;
    private boolean isFlushed = false;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private static final List<Watch> watches = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        JDABuilder.createDefault(Constants.TOKEN)
                .addEventListeners(new Core())
                .setActivity(Activity.listening("type !sgk help"))
                .build();
        // backup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Backup.write(watches);
            }
            catch (Exception ignored) {}
        }));
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        System.out.println(Constants.SELFNAME + " is online!");
        try {
            // continue with previous watches
            watches.addAll(Backup.read());
            if(!watches.isEmpty()) {
                future = executor.scheduleAtFixedRate(() -> watch(event.getJDA()), 0, Constants.WATCH_PERIOD, TimeUnit.MINUTES);
                System.out.println("Loaded.");
            }
        }
        catch (Exception e) {
            watches.clear();
            System.out.println("Loading failed:");
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if(event.getAuthor().isBot() || !Validator.validateCommand(event.getMessage().getContentRaw()) || event.getChannelType() != ChannelType.TEXT) {
            return;
        }
        Message message = event.getMessage();
        TextChannel channel = event.getTextChannel();
        String command = message.getContentRaw().trim().toLowerCase();

        // uncomment on test
        /*
        if(!event.getAuthor().getAsTag().equals(Constants.OWNER)) {
            send(channel, "\uD83E\uDDEA testteyim.");
            return;
        }
         */

        List<String> tokens = Arrays.asList(command.split("\\s+"));
        if(!Validator.validateParameter(tokens)) {
            send(channel, ":relieved: bilmediğim parametreler giriyorsun...");
        }
        else if(tokens.size() == 2) {
            switch(tokens.get(1)) {
                case "help" -> showHelp(channel);
                case "clear" -> clear(channel);
                case "durum" -> display(event, User.ALL, SortBy.DURATION);
                case "forbes" -> display(event, User.ALL, SortBy.BALANCE);
                case "menü" -> send(channel, Waiter.showMenu());
                case "ölç" -> startWatching(channel);
                case "sal" -> {
                    if(Validator.validateTime()) {
                        send(channel, ":man_technologist: mesai bitsin de öyle salayım.");
                    }
                    else {
                        stopWatching(channel);
                    }
                }
                default -> send(channel, ":face_with_monocle: komutu yanlış/eksik girmiş olabilir misin?");
            }
        }
        else if(tokens.size() == 3) {
            switch(tokens.get(1)) {
                case "durum" -> {
                    int position = Parser.extractStatus(tokens);
                    if(position > 0) {
                        display(event, position, SortBy.DURATION);
                    }
                    else {
                        send(channel, ":wolf: ya düzgün parametre gir ya terk et...");
                    }
                }
                case "dürüm" -> {
                    if(buy(channel, event.getAuthor().getIdLong(), Parser.extractMention(command), Waiter.order(0))) {
                        message.delete().queue();
                    }
                }
                case "ayran" -> {
                    if(buy(channel, event.getAuthor().getIdLong(), Parser.extractMention(command), Waiter.order(1))) {
                        message.delete().queue();
                    }
                }
                default -> send(channel, ":alien: böyle bir parametre yok, böyle bir parametreye gerek yok.");
            }
        }
        else {
            send(channel, ":pensive: geçersiz parametre girdin, niye öyle oldu?");
        }
    }

    private void send(@Nonnull MessageChannel channel, String message) {
        channel.sendMessage(message).queue();
    }

    private void showHelp(TextChannel channel) {
        String commands = "`!sgk ölç`: " + Constants.START_HOUR + "-" + Constants.END_HOUR + " saatleri arasında " + Constants.WATCH_PERIOD + " dakikada bir süreleri ölçer.\n" +
                            "`!sgk sal`: süre ölçmeyi bırakır.\n" +
                            "`!sgk forbes`: bakiyeye göre sıralamayı gösterir.\n" +
                            "`!sgk durum`: süreye göre sıralamayı gösterir.\n" +
                            "`!sgk durum [ilk|son|ben|@kullanıcı]`: belirtilen sıradaki veya etiketlenen kişiyi gösterir.\n" +
                            "`!sgk [dürüm|ayran] @kullanıcı`: etiketlenen kişiye dürüm veya ayran ısmarlanır.\n" +
                            "`!sgk menü`: dürüm ve ayran fiyatlarını gösterir.\n" +
                            "`!sgk clear`: bot tarafından gönderilen mesajları ve komutları temizler.";
        send(channel, commands);
    }

    private void clear(@Nonnull TextChannel channel) {
        OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(2, ChronoUnit.WEEKS);
        final List<Message> messages = new ArrayList<>();
        for(Message m : channel.getIterableHistory()) {
            if(m.getTimeCreated().isBefore(twoWeeksAgo)) {
                break;
            }
            // delete bot's messages (except orders) and commands
            else if((m.getAuthor().isBot() && m.getAuthor().getName().equals(Constants.SELFNAME) && !m.getContentRaw().contains(":arrow_right:")) || Validator.validateCommand(m.getContentRaw())) {
                messages.add(m);
            }
        }
        // jda api restriction: deleting count should be between 2 and 100
        if(messages.size() > 1) {
            channel.deleteMessages(messages.stream().limit(Math.min(messages.size(), 100)).collect(Collectors.toList()))
                    .delay(1, TimeUnit.SECONDS)
                    .flatMap(report -> channel.sendMessage(":wastebasket: bugün de temizlendik çok şükür. (" + Math.min(messages.size(), 100) + " mesaj)"))
                    .delay(3, TimeUnit.SECONDS)
                    .flatMap(Message::delete)
                    .queue();
        }
    }

    private void watch(JDA jda) {
        // watch only in working hours and weekdays
        if(!Validator.validateTime()) {
            if(!isFlushed) {
                watches.forEach(w -> w.getMembers().values().forEach(User::reset));
                isFlushed = true;
            }
            return;
        }
        else {
            isFlushed = false;
        }

        long timestamp = System.currentTimeMillis();
        for(Watch watch : watches) {
            TextChannel channel = jda.getTextChannelById(watch.getChannelId());
            if(channel == null) {
                watches.remove(watch);
                continue;
            }

            List<Member> channelMembers = channel.getMembers()
                                                .stream()
                                                .filter(m -> !m.getUser().isBot())
                                                .collect(Collectors.toList());
            Map<Long, User> localMembers = watch.getMembers();
            for(Member channelMember : channelMembers) {
                long id = channelMember.getIdLong();
                if(localMembers.containsKey(id)) {
                    User localMember = localMembers.get(id);
                    // salute
                    if(!localMember.isSaluted() && isOnline(channelMember)) {
                        String salutationText = new Random().nextInt(2) == 0 ? ":dragon_face: günaydın kerkenez " : ":chipmunk: hoşgeldin sincap ";
                        send(channel, salutationText + channelMember.getAsMention());
                        localMember.setSaluted(true);
                        localMember.setStartedAt(timestamp);
                    }
                    // update
                    int duration = localMember.getStatus() == OnlineStatus.ONLINE && isOnline(channelMember) ? (int) (timestamp - localMember.getTimestamp()) / 1000 : 0;
                    if(localMember.getTotalDuration() / 3600 != (localMember.getTotalDuration() + duration) / 3600) {
                        localMember.setBalance(localMember.getBalance() + 1);
                    }
                    localMember.setTotalDuration(localMember.getTotalDuration() + duration);
                    localMember.setStatus(channelMember.getOnlineStatus());
                    localMember.setTimestamp(timestamp);
                }
                // add
                else {
                    localMembers.put(id, new User(channelMember.getUser().getAsTag(), channelMember.getOnlineStatus(), false, timestamp, 0, 0, 0));
                }
            }
        }
    }

    private void startWatching(TextChannel channel) {
        if(!isWatching(channel)) {
            long timestamp = System.currentTimeMillis();
            Map<Long, User> members = new HashMap<>();
            for(Member member : channel.getMembers()) {
                if(!member.getUser().isBot()) {
                    members.put(member.getIdLong(), new User(member.getUser().getAsTag(), member.getOnlineStatus(), false, timestamp, 0, 0, 0));
                }
            }
            // start batch job if not running
            if(watches.isEmpty()) {
                future = executor.scheduleAtFixedRate(() -> watch(channel.getJDA()), 0, Constants.WATCH_PERIOD, TimeUnit.MINUTES);
            }
            watches.add(new Watch(channel.getIdLong(), members));
        }
        send(channel, ":detective: izlemedeyim.");
    }

    private void stopWatching(TextChannel channel) {
        if(isWatching(channel)) {
            watches.removeIf(w -> w.getChannelId() == channel.getIdLong());
            // terminate batch job if no watch exists
            if(watches.isEmpty()) {
                future.cancel(true);
            }
        }
        send(channel, ":hand_splayed: saldım.");
    }

    private Optional<Watch> getWatch(TextChannel channel) {
        return watches.stream().filter(w -> w.getChannelId() == channel.getIdLong()).findAny();
    }

    private boolean isWatching(TextChannel channel) {
        return getWatch(channel).isPresent();
    }

    private boolean isOnline(@Nonnull Member member) {
        return member.getOnlineStatus(ClientType.DESKTOP) == OnlineStatus.ONLINE || member.getOnlineStatus(ClientType.WEB) == OnlineStatus.ONLINE;
    }

    private boolean buy(TextChannel channel, long buyerId, String mention, Product product) {
        Optional<Watch> watch = getWatch(channel);
        if(watch.isPresent() && !mention.isEmpty()) {
            User buyer = watch.get().getMembers().get(buyerId);
            int quantity;
            String boost = "";
            // @everyone or @here
            if(mention.startsWith("@")) {
                quantity = (int) channel.getMembers()
                                        .stream()
                                        .filter(m -> isOnline(m) && watch.get().getMembers().containsKey(m.getIdLong()))
                                        .count();
                boost = ":boom: ";
            }
            // role
            else if(mention.startsWith("<@&")) {
                long roleId = Long.parseLong(mention.substring(mention.indexOf('&') + 1, mention.indexOf('>')));
                quantity = (int) channel.getMembers()
                                        .stream()
                                        .filter(m -> isOnline(m) && watch.get().getMembers().containsKey(m.getIdLong()) && m.getRoles().stream().anyMatch(r -> r.getIdLong() == roleId))
                                        .count();
            }
            // member
            else {
                quantity = 1;
            }
            int price = quantity * product.getPrice();
            if(buyer.getBalance() - price >= 0) {
                buyer.setBalance(buyer.getBalance() - price);

                // order
                String orderText = boost + "<@!" + buyerId + "> :arrow_right: " + mention + ": " + product.getEmoji() + ' ' + product.getText();
                send(channel, orderText);

                // invoice
                String invoiceText =  "`\uD83E\uDDFE tutar: " + quantity + " x " + product.getEmoji() + " = \uD83D\uDCB2" + quantity * product.getPrice() +", kalan: \uD83D\uDCB0" + buyer.getBalance() + '`';
                channel.getMembers()
                        .stream()
                        .filter(m -> m.getIdLong() == buyerId)
                        .findFirst()
                        .ifPresent(m -> m.getUser().openPrivateChannel().queue(privateChannel -> send(privateChannel, invoiceText)));
                return true;
            }
            // insufficient balance
            else {
                send(channel, "¯\\_(ツ)_/¯ para yok.");
            }
        }
        return false;
    }

    private void display(@Nonnull MessageReceivedEvent event, int position, int sortBy) {
        TextChannel channel = event.getTextChannel();
        Optional<Watch> watch = getWatch(channel);
        if(watch.isEmpty() || watch.get().getMembers().isEmpty()) {
            send(channel, ":saxophone: olmayan şeyi nasıl yazayım zurna?");
            return;
        }

        // sort members
        List<User> members = new ArrayList<>(watch.get().getMembers().values());
        members.removeIf(m -> m.getTotalDuration() == 0);
        if(members.isEmpty()) {
            send(channel, ":saxophone: olmayan şeyi nasıl yazayım zurna?");
            return;
        }

        final int size = members.size();
        if(sortBy == SortBy.DURATION) {
            members.sort(Comparator.comparingInt(User::getTotalDuration).reversed());
        }
        else {
            members.sort(Comparator.comparingInt(User::getBalance).reversed());
        }

        switch(position) {
            case User.SELF -> {
                User self = watch.get().getMembers().get(event.getAuthor().getIdLong());
                if(self != null) {
                    send(channel, self.toString("\uD83D\uDC68", members.indexOf(self) + 1, size));
                }
            }
            case User.WINNER -> send(channel, members.get(0).toString("\uD83E\uDD47", 1, size));
            case User.LOSER -> send(channel, members.get(members.size() - 1).toString("\uD83D\uDCA9", size, size));
            case User.TAG -> {
                String message = event.getMessage().getContentRaw();
                User user = watch.get().getMembers().get(Parser.extractId(message));
                if (user != null) {
                    send(channel, user.toString("\uD83D\uDC68", members.indexOf(user) + 1, size));
                }
            }
            case User.ALL -> {
                StringBuilder leaderboard = new StringBuilder("```");
                for(int i = 0; i < size; i++) {
                    String emoji;
                    if (i == 0) {
                        emoji = "\uD83E\uDD47"; // first place
                    } else if (i == 1) {
                        emoji = "\uD83E\uDD48"; // second place
                    } else if (i == 2) {
                        emoji = "\uD83E\uDD49"; // third place
                    } else if (i == size - 1) {
                        emoji = "\uD83D\uDCA9"; // last place
                    } else {
                        emoji = "\uD83D\uDC68"; // ordinary
                    }
                    leaderboard.append(members.get(i).toString(emoji, i + 1, size, true)).append(i < size - 1 ? "\n\n" : "```");
                }
                send(channel, leaderboard.toString());
            }
            default -> throw new IllegalArgumentException();
        }
    }

}

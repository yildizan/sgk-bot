package com.yildizan.bot.sgk;

import com.yildizan.bot.sgk.model.User;
import com.yildizan.bot.sgk.model.Watch;
import com.yildizan.bot.sgk.utility.Constants;
import com.yildizan.bot.sgk.utility.Parser;
import com.yildizan.bot.sgk.utility.Validator;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Core extends ListenerAdapter {

    private ScheduledFuture<?> future;
    private boolean isFlushed = false;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final List<Watch> watches = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new JDABuilder(Constants.TOKEN)
            .addEventListeners(new Core())
            .setActivity(Activity.listening("type !sgk help"))
            .build();
        System.out.println(Constants.SELFNAME + " is online!");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot() || !Validator.validateCommand(event.getMessage().getContentRaw()) || event.getChannelType() != ChannelType.TEXT) {
            return;
        }
        Message message = event.getMessage();
        TextChannel channel = event.getTextChannel();
        String command = message.getContentRaw().toLowerCase();

        List<String> tokens = Arrays.asList(command.split("\\s+"));
        if(!Validator.validateParameter(tokens)) {
            send(channel, ":relieved: bilmediğim parametreler giriyorsun...");
        }
        else if(tokens.size() == 2) {
            switch (tokens.get(1)) {
                case "help":
                    showHelp(channel);
                    break;
                case "clear":
                    clear(channel);
                    break;
                case "ölç":
                    startWatching(channel);
                    break;
                case "sal":
                    if(Validator.validateTime()) {
                        send(channel, ":man_technologist: mesai bitsin de öyle salayım.");
                    }
                    else {
                        stopWatching(channel);
                    }
                    break;
                case "durum":
                    display(event, User.ALL);
                    break;
                default:
                    send(channel, ":face_with_monocle: komutu yanlış/eksik girmiş olabilir misin?");
                    break;
            }
        }
        else if(tokens.size() == 3) {
            int position = Parser.extractStatus(tokens);
            if(position > 0) {
                display(event, position);
            }
            else {
                send(channel, ":wolf: ya düzgün parametre gir ya terk et...");
            }
        }
        else {
            send(channel, ":pensive: geçersiz parametre girdin, niye öyle oldu?");
        }
    }

    private void send(TextChannel channel, String message) {
        channel.sendMessage(message).queue();
    }

    private void showHelp(TextChannel channel) {
        send(channel, "`!sgk ölç`: " + Constants.START_HOUR + "-" + Constants.END_HOUR + " saatleri arasında " + Constants.WATCH_PERIOD + " dakikada bir süreleri ölçer.");
        send(channel, "`!sgk sal`: süre ölçmeyi bırakır.");
        send(channel, "`!sgk durum`: tüm süreleri gösterir.");
        send(channel, "`!sgk durum [ilk|son|ben]`: istenen sıradaki kişiyi gösterir.");
        send(channel, "`!sgk clear`: bot tarafından gönderilen mesajları ve komutları temizler.");
    }

    private void clear(TextChannel channel) {
        OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(2, ChronoUnit.WEEKS);
        final List<Message> messages = new ArrayList<>();
        for(Message m : channel.getIterableHistory()) {
            if(m.getTimeCreated().isBefore(twoWeeksAgo)) {
                break;
            }
            else if((m.getAuthor().isBot() && m.getAuthor().getName().equals(Constants.SELFNAME)) ||
                    Validator.validateCommand(m.getContentRaw())) {
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

    private void watch() {
        // watch only in working hours and weekdays
        if(!Validator.validateTime()) {
            if(!isFlushed) {
                watches.forEach(w -> w.getMembers().clear());
                isFlushed = true;
            }
            return;
        }
        else {
            isFlushed = false;
        }

        long timestamp = System.currentTimeMillis();
        for(Watch watch : watches) {
            TextChannel channel = watch.getChannel();
            List<Member> channelMembers = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());
            Map<Long, User> localMembers = watch.getMembers();
            for(Member channelMember : channelMembers) {
                long id = channelMember.getIdLong();
                if(localMembers.containsKey(id)) {
                    User localMember = localMembers.get(id);
                    // salute
                    if(!localMember.isSaluted() && localMember.getStatus() != OnlineStatus.ONLINE && channelMember.getOnlineStatus() == OnlineStatus.ONLINE) {
                        String salutationText = new Random().nextInt(2) == 0 ? ":dragon_face: günaydın kerkenez " : ":chipmunk: hoşgeldin sincap ";
                        send(channel, salutationText + channelMember.getAsMention());
                        localMember.setSaluted(true);
                        localMember.setStartedAt(timestamp);
                    }
                    // update
                    int duration = localMember.getStatus() == OnlineStatus.ONLINE && channelMember.getOnlineStatus() == OnlineStatus.ONLINE ? (int) (timestamp - localMember.getTimestamp()) / 1000 : 0;
                    localMember.setTotalDuration(localMember.getTotalDuration() + duration);
                    localMember.setStatus(channelMember.getOnlineStatus());
                    localMember.setTimestamp(timestamp);
                }
                // add
                else {
                    localMembers.put(id, new User(channelMember.getUser().getAsTag(), channelMember.getOnlineStatus(), false, timestamp, 0, 0));
                }
            }
        }
    }

    private void startWatching(TextChannel channel) {
        if(!isWatching(channel)) {
            // start batch job if not running
            if(watches.isEmpty()) {
                future = executor.scheduleAtFixedRate(this::watch, 0, Constants.WATCH_PERIOD, TimeUnit.MINUTES);
            }
            long timestamp = System.currentTimeMillis();
            Map<Long, User> members = new HashMap<>();
            for(Member member : channel.getMembers()) {
                if(!member.getUser().isBot()) {
                    members.put(member.getIdLong(), new User(member.getUser().getAsTag(), member.getOnlineStatus(), false, timestamp, 0, 0));
                }
            }
            watches.add(new Watch(channel, members));
        }
        send(channel, ":detective: izlemedeyim.");
    }

    private void stopWatching(TextChannel channel) {
        if(isWatching(channel)) {
            watches.removeIf(c -> c.getChannel().getGuild().getName().equals(channel.getGuild().getName()));
            // terminate batch job if no watch exists
            if(watches.isEmpty()) {
                future.cancel(true);
            }
        }
        send(channel, ":hand_splayed: saldım.");
    }

    private Optional<Watch> getWatch(TextChannel channel) {
        return watches.stream().filter(w -> w.getChannel().getGuild().getName().equals(channel.getGuild().getName())).findFirst();
    }

    private boolean isWatching(TextChannel channel) {
        return getWatch(channel).isPresent();
    }

    private void display(MessageReceivedEvent event, int position) {
        TextChannel channel = event.getTextChannel();
        Optional<Watch> watch = getWatch(channel);
        if(!watch.isPresent() || watch.get().getMembers().isEmpty()) {
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
        Collections.sort(members);

        switch (position) {
            case User.SELF:
                User self = watch.get().getMembers().get(event.getAuthor().getIdLong());
                if(Objects.nonNull(self)) {
                    send(channel, self.toString("\uD83D\uDC68", members.indexOf(self) + 1, size));
                }
                break;
            case User.WINNER:
                send(channel, members.get(0).toString("\uD83E\uDD47", 1, size));
                break;
            case User.LOSER:
                send(channel, members.get(members.size() - 1).toString("\uD83D\uDCA9", size, size));
                break;
            case User.ALL:
                StringBuilder leaderboard = new StringBuilder("```");
                for(int i = 0; i < size; i++) {
                    String emoji;
                    if(i == 0) {
                        emoji = "\uD83E\uDD47"; // first place
                    }
                    else if(i == 1) {
                        emoji = "\uD83E\uDD48"; // second place
                    }
                    else if(i == 2) {
                        emoji = "\uD83E\uDD49"; // third place
                    }
                    else if(i == size - 1) {
                        emoji = "\uD83D\uDCA9"; // last place
                    }
                    else {
                        emoji = "\uD83D\uDC68"; // ordinary
                    }
                    leaderboard.append(members.get(i).toString(emoji, i + 1, size, true)).append(i < size - 1 ? "\n\n" : "```");
                }
                send(channel, leaderboard.toString());
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

}

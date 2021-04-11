package dev.gigafyde.arizoomii.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.gigafyde.arizoomii.utils.Emotes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateRegionEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ServerLogListener extends ListenerAdapter {
    private final Cache<Long, Message> modlogCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(2_500).build();
    private final Cache<Long, User> userCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(1_000_000).build();
    private final Cache<Long, Message> messageCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(5_000_000).build();
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    public static TextChannel serverlog;

    private String getTime() {
        return dateFormat.format(new Date());
    }

    private String logTime() {
        return String.format("**`%s UTC`**", getTime());
    }

    private String haste(String message) {
        if (message.length() > 500) {
            return "Message too long ";
        }
        return message;
    }

    private String getUser(Message message) {
        return getTag(message.getAuthor());
    }

    private String getUser(Member member) {
        if (member == null) return "Unknown Member";
        return getUser(member.getUser());
    }

    private String getUser(User user) {
        if (user == null) return "Unknown user";
        return "**" + user.getName().replace("@", "@\u200b").replace("`", "") + "#" + user.getDiscriminator() + "`(" + user.getId() + ")`**";
    }

    public static String getTag(Member member) {
        return member.getUser().getName() + "#" + member.getUser().getDiscriminator();
    }

    public static String getTag(User user) {
        return user.getName() + "#" + user.getDiscriminator() + " (" + user.getId() + ")";
    }

    private void log(String message) {
        serverlog.sendMessage(message).queue(msg -> modlogCache.put(msg.getIdLong(), msg));
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        messageCache.put(event.getMessage().getIdLong(), event.getMessage());
        userCache.put(event.getMessage().getAuthor().getIdLong(), event.getAuthor());
    }


    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        Message message = modlogCache.getIfPresent(event.getMessageIdLong());
        if (message != null) {
            serverlog.sendMessage(message).queue(msg -> modlogCache.put(msg.getIdLong(), msg));
            return;
        }

        message = messageCache.getIfPresent(event.getMessageIdLong());
        if (message == null) return;


        String content = message.getContentStripped();
        if (content.isEmpty()) {
            return;
        }
        log(String.format(Emotes.EDIT + " %s | %s %s's message has been deleted | Content: `%s`",
                logTime(),
                event.getChannel().getAsMention().replace("`", ""),
                getUser(message),
                message));
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            return;
        }
        Message after = event.getMessage();
        Message before = messageCache.getIfPresent(after.getIdLong());

        if (before == null) return;
        if (after.getContentStripped().isEmpty()) return;
        if (after.getContentStripped().equals(before.getContentStripped())) return;
        messageCache.put(event.getMessage().getIdLong(), event.getMessage());
        log(String.format(Emotes.EDIT + " %s | %s %s's message (`%s`) has been edited\nBefore: `%s`\nAfter: `%s`",
                logTime(),
                event.getChannel().getAsMention().replace("`", ""),
                getUser(before),
                after.getId(),
                haste(before.getContentStripped().replace("`", "")),
                haste(after.getContentStripped().replace("`", ""))));
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        long days = Duration.between(event.getMember().getUser().getTimeCreated(), OffsetDateTime.now()).toDays();
        String created = days > 10 ? String.format("**Created %s days ago.**", days) : String.format(Emotes.WARN + " **New User - joined %s days ago.**", days);

        log(String.format(Emotes.SUCCESS + " %s | %s joined the server.",
                logTime(),
                getUser(event.getMember()),
                created));
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            log(String.format(Emotes.LEAVE + " %s | %s has left or was kicked from the server.",
                    logTime(),
                    getUser(event.getUser())));
            return;
        }

        List<AuditLogEntry> kicks = event.getGuild().retrieveAuditLogs().type(ActionType.KICK).complete();
        if (!kicks.isEmpty() && Instant.now().getEpochSecond() - kicks.get(0).getTimeCreated().toInstant().getEpochSecond() <= 2 && kicks.get(0).getTargetIdLong() == Objects.requireNonNull(event.getMember()).getUser().getIdLong()) {
            log(String.format(Emotes.KICK + "%s | %s was kicked.",
                    logTime(),
                    getUser(event.getUser())));
        } else {
            log(String.format(Emotes.LEAVE + " %s | %s has left the server.",
                    logTime(),
                    getUser(event.getUser())));
        }
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        log(String.format(Emotes.BAN + " %s | **%s** (`%s`) has been banned.",
                logTime(),
                getTag(event.getUser()),
                event.getUser().getId()));
    }


    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        log(String.format(Emotes.WARN + " %s | **%s** (`%s`) has been unbanned",
                logTime(),
                getTag(event.getUser()).replace("`", ""),
                event.getUser().getId()));
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            List<AuditLogEntry> memberupdates = event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).complete();
            if (!memberupdates.isEmpty() && Instant.now().getEpochSecond() - memberupdates.get(0).getTimeCreated().toInstant().getEpochSecond() <= 2 && memberupdates.get(0).getTargetIdLong() == event.getMember().getUser().getIdLong()) {
                User target = event.getMember().getUser();
                User author = memberupdates.get(0).getUser();
                String NewNick = event.getNewNickname();
                String PrevNick = event.getOldNickname();
                if (event.getNewNickname() == null) NewNick = event.getMember().getUser().getName();
                if (event.getOldNickname() == null) PrevNick = event.getMember().getUser().getName();
                if (target == author)
                    log(String.format(Emotes.EDIT + "%s | %s changed their nickname | Old: `%s` ⇨ New: `%s`", logTime(), getUser(event.getMember()), event.getOldNickname() == null ? event.getMember().getUser().getName() : event.getOldNickname(), event.getNewNickname() == null ? event.getMember().getUser().getName() : event.getNewNickname()));
                else
                    log(String.format(Emotes.EDIT + "%s | %s updated the nickname of %s | Old: `%s` ⇨ New: `%s`", logTime(), getUser(author), getUser(target), PrevNick, NewNick));
            }
        } else
            log(String.format(Emotes.EDIT + "%s | %s changed their nickname | Old: `%s` ⇨ New: `%s`", logTime(), getUser(event.getMember()), event.getOldNickname() == null ? event.getMember().getUser().getName() : event.getOldNickname(), event.getNewNickname() == null ? event.getMember().getUser().getName() : event.getNewNickname()));
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        log(String.format(Emotes.INFO + " %s | Role **added** to %s - `%s`",
                logTime(),
                getUser(event.getMember()),
                event.getRoles().stream().map(Role::getName).map(s -> s.replace("`", "")).collect(Collectors.joining(", "))));
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        log(String.format(Emotes.INFO + " %s | Role **taken** from %s - `%s`",
                logTime(),
                getUser(event.getMember()),
                event.getRoles().stream().map(Role::getName).map(s -> s.replace("`", "")).collect(Collectors.joining(", "))));
    }

    @Override
    public void onRoleUpdateName(RoleUpdateNameEvent event) {
        log(String.format(Emotes.INFO + " %s | Role **%s** had it's name changed from **%s** to **%s**",
                logTime(),
                event.getOldName().replace("`", ""),
                event.getOldName().replace("`", ""),
                event.getNewName().replace("`", "")));
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        List<Guild> userGuilds = event.getJDA().getGuilds().stream().filter(g -> g.getMember(event.getUser()) != null).collect(Collectors.toList());
        for (Guild guild : userGuilds) {
            if (serverlog == null) return;

            log(String.format(Emotes.CHANGE + " %s | %s Changed their username `%s` ➥ `%s`",
                    logTime(),
                    event.getOldName().replace("`", "") + "#" + event.getUser().getDiscriminator() + " (" + event.getUser().getId() + ")",
                    event.getOldName().replace("`", ""),
                    event.getNewName().replace("`", "")));
        }
    }

    @Override
    public void onTextChannelCreate(TextChannelCreateEvent event) {
        log(String.format(Emotes.INFO + " %s | Textchannel **%s** - %s has been created",
                logTime(),
                event.getChannel().getName().replace("`", ""),
                event.getChannel().getAsMention()).replace("`", ""));

    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        log(String.format(Emotes.INFO + " %s | Textchannel **%s** - has been deleted.",
                logTime(),
                event.getChannel().getName().replace("`", "")));
    }

    @Override
    public void onTextChannelUpdateName(TextChannelUpdateNameEvent event) {
        log(String.format(Emotes.INFO + " %s | %s had it's name changed from **%s** to **%s**",
                logTime(),
                event.getChannel().getAsMention(),
                event.getOldName().replace("`", ""),
                event.getNewName().replace("`", "")));
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        log(String.format(Emotes.WARN + " %s | Role **%s** has been created.",
                logTime(),
                event.getRole().getName().replace("`", "")));
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        log(String.format(Emotes.WARN + " %s | Role **%s** has been deleted.",
                logTime(),
                event.getRole().getName().replace("`", "")));
    }

    @Override
    public void onGuildUpdateOwner(GuildUpdateOwnerEvent event) {
        String before = getTag(Objects.requireNonNull(event.getOldOwner()));
        String after = getTag(Objects.requireNonNull(event.getNewOwner()));

        log(String.format(Emotes.WARN + " %s | **%s** transferred owner ship to **%s**",
                logTime(),
                before.replace("`", ""),
                after.replace("`", "")));

    }

    @Override
    public void onGuildUpdateRegion(GuildUpdateRegionEvent event) {
        String before = event.getOldRegion().toString();
        String after = event.getNewRegion().toString();

        log(String.format(Emotes.REGION + " %s | Server region changed from **%s** to **%s**",
                logTime(),
                before.replace("`", ""),
                after.replace("`", "")));
    }

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        String before = event.getOldName();
        String after = event.getNewName();

        log(String.format(Emotes.REGION + " %s | Server name changed from **%s** to **%s**",
                logTime(),
                before.replace("`", ""),
                after.replace("`", "")));
    }
}

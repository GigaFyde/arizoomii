package dev.gigafyde.arizoomii;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ReactionListener extends ListenerAdapter {
    public static Role youtubePing;
    public static Role twitchPing;
    public static Role announcementPing;

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (!event.getChannel().getId().equals("752523144853585980")) return;
        if (event.getReactionEmote().getName().equals("youtube"))
            event.getGuild().addRoleToMember(event.getMember(), youtubePing).reason("YT Reaction Role").queue();
        if (event.getReactionEmote().getName().equals("twitch"))
            event.getGuild().addRoleToMember(event.getMember(), twitchPing).reason("Twitch Reaction Role").queue();
        if (event.getReactionEmote().getName().equals("neongoogletada"))
            event.getGuild().addRoleToMember(event.getMember(), announcementPing).reason("Announcement Reaction Role").queue();
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        Member member = event.getGuild().retrieveMemberById(event.getUserId()).complete();
        if (!event.getChannel().getId().equals("752523144853585980")) return;
        if (event.getReactionEmote().getName().equals("youtube"))
            event.getGuild().removeRoleFromMember(member, youtubePing).reason("YT Reaction Role").queue();
        if (event.getReactionEmote().getName().equals("twitch"))
            event.getGuild().removeRoleFromMember(member, twitchPing).reason("Twitch Reaction Role").queue();
        if (event.getReactionEmote().getName().equals("neongoogletada"))
            event.getGuild().removeRoleFromMember(member, announcementPing).reason("Announcement Reaction Role").queue();
    }
}

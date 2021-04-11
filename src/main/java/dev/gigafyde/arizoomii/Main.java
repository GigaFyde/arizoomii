package dev.gigafyde.arizoomii;

import dev.gigafyde.arizoomii.listeners.ReactionListener;
import dev.gigafyde.arizoomii.listeners.ServerLogListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static JDA jda;
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        jda = JDABuilder.createDefault(System.getenv("TOKEN"))
                .addEventListeners(new ReactionListener(), new ServerLogListener())
                .build();
        jda.awaitReady();
        ServerLogListener.serverlog = jda.getTextChannelById("752590321992466432");
        ReactionListener.youtubePing = jda.getRoleById("766970231498866699");
        ReactionListener.twitchPing = jda.getRoleById("766970181447581696");
        ReactionListener.announcementPing = jda.getRoleById("766970037352398861");
        log.info("Starting arizoomii");
    }
}

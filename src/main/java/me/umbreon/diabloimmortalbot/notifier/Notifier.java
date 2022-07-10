package me.umbreon.diabloimmortalbot.notifier;

import me.umbreon.diabloimmortalbot.database.DatabaseRequests;
import me.umbreon.diabloimmortalbot.gameevents.*;
import me.umbreon.diabloimmortalbot.utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Umbreon Majora
 */
public class Notifier {

    private final AncientArea ancientArea;
    private final AncientNightMare ancientNightMare;
    private final Battleground battleground;
    private final DemonGates demonGates;
    private final HauntedCarriage hauntedCarriage;
    private final Vault vault;
    private final ClientCache clientCache;
    private final Assembly assembly;
    private final ShadowLottery shadowLottery;
    private final InformationNotifier informationNotifier;

    public Notifier(DatabaseRequests databaseRequests, ClientConfig clientConfig, ClientCache clientCache) {
        this.clientCache = clientCache;
        this.assembly = new Assembly(databaseRequests, clientConfig);
        this.shadowLottery = new ShadowLottery(databaseRequests, clientConfig, clientCache);
        this.ancientArea = new AncientArea(databaseRequests, clientConfig);
        this.ancientNightMare = new AncientNightMare(databaseRequests, clientConfig);
        this.battleground = new Battleground(databaseRequests, clientConfig);
        this.demonGates = new DemonGates(databaseRequests, clientConfig);
        this.hauntedCarriage = new HauntedCarriage(databaseRequests, clientConfig);
        this.vault = new Vault(databaseRequests, clientConfig);
        this.informationNotifier = new InformationNotifier(clientCache);
    }

    public void runNotifierScheduler(JDA jda) {

        new Timer().schedule(new TimerTask() {
            public void run() {

                StringBuilder infoNotificationBuilder = new StringBuilder();
                StringBuilder aliveChecker = new StringBuilder();

                for (String channel : clientCache.getListWithNotificationChannels().keySet()) {
                    try {
                        TextChannel textChannel = jda.getTextChannelById(channel);
                        String timezone = clientCache.getTimezone(channel);

                        if (textChannel != null) {

                            if (Time.isTimeZoneUTC(timezone)) {
                                // UTC+5 or UTC-9 or smt is't supported or idk?
                                // so we r going to replace that.
                                // UTC-2 -> GMT-2
                                timezone = Time.replaceUtcTimeZone(timezone);
                            }

                            //If the textChannel is null, the channel got deleted or bot isn't on that server anymore.

                            StringBuilder notificationMessageBuilder = new StringBuilder();
                            aliveChecker.append(channel).append(Time.getFullTime(timezone)).append(timezone).append("\n");

                            switch (clientCache.getStatus(textChannel.getId())) {
                                case 0: //ALL MESSAGES
                                    notificationMessageBuilder.append(vault.checkVault(timezone));
                                    notificationMessageBuilder.append(battleground.checkBattleground(timezone));
                                    notificationMessageBuilder.append(ancientNightMare.checkAncientNightMare(timezone));
                                    notificationMessageBuilder.append(demonGates.checkDemonGates(timezone));
                                    notificationMessageBuilder.append(hauntedCarriage.checkHauntedCarriage(timezone));
                                    notificationMessageBuilder.append(ancientArea.checkAncientArea(timezone));
                                    notificationMessageBuilder.append(assembly.checkAssembly(timezone));
                                    notificationMessageBuilder.append(shadowLottery.checkShadowLottery(timezone));
                                    break;
                                case 1: //ONLY OVERWORLD
                                    notificationMessageBuilder.append(ancientNightMare.checkAncientNightMare(timezone));
                                    notificationMessageBuilder.append(demonGates.checkDemonGates(timezone));
                                    notificationMessageBuilder.append(hauntedCarriage.checkHauntedCarriage(timezone));
                                    notificationMessageBuilder.append(ancientArea.checkAncientArea(timezone));
                                    notificationMessageBuilder.append(battleground.checkBattleground(timezone));
                                    break;
                                case 2: //ONLY IMMPORTAL
                                    notificationMessageBuilder.append(vault.checkVault(timezone));
                                    notificationMessageBuilder.append(battleground.checkBattleground(timezone));
                                    break;
                                case 3: //ONLY SHADOW
                                    notificationMessageBuilder.append(vault.checkVault(timezone));
                                    notificationMessageBuilder.append(assembly.checkAssembly(timezone));
                                    notificationMessageBuilder.append(shadowLottery.checkShadowLottery(timezone));
                                    notificationMessageBuilder.append(battleground.checkBattleground(timezone));
                                    break;
                                case 4: //IMMORTAL WITH OVERWORLD
                                    notificationMessageBuilder.append(ancientNightMare.checkAncientNightMare(timezone));
                                    notificationMessageBuilder.append(demonGates.checkDemonGates(timezone));
                                    notificationMessageBuilder.append(hauntedCarriage.checkHauntedCarriage(timezone));
                                    notificationMessageBuilder.append(ancientArea.checkAncientArea(timezone));
                                    notificationMessageBuilder.append(battleground.checkBattleground(timezone));
                                    notificationMessageBuilder.append(vault.checkVault(timezone));
                                    break;
                                case 5: //SHADOW WITH OVERWORLD
                                    notificationMessageBuilder.append(ancientNightMare.checkAncientNightMare(timezone));
                                    notificationMessageBuilder.append(demonGates.checkDemonGates(timezone));
                                    notificationMessageBuilder.append(hauntedCarriage.checkHauntedCarriage(timezone));
                                    notificationMessageBuilder.append(ancientArea.checkAncientArea(timezone));
                                    notificationMessageBuilder.append(battleground.checkBattleground(timezone));
                                    notificationMessageBuilder.append(vault.checkVault(timezone));
                                    notificationMessageBuilder.append(assembly.checkAssembly(timezone));
                                    notificationMessageBuilder.append(shadowLottery.checkShadowLottery(timezone));
                                    break;
                                case 128:
                                    notificationMessageBuilder.append("Time: ").append(Time.getFullTime(timezone))
                                            .append(" TimeZone: ").append(timezone);
                            }

                            if (notificationMessageBuilder.length() > 0) {
                                notificationMessageBuilder.append(prepareMention(channel, textChannel.getGuild()));
                                addDebugMessageIfInMode(channel, timezone, notificationMessageBuilder);
                                textChannel.sendMessage(notificationMessageBuilder.toString()).queue();
                                String logInfoMessage = "Sended Message to " + textChannel.getGuild().getName() + ".\n" +
                                        notificationMessageBuilder + "\nID: " + textChannel.getGuild().getId() + "\n" +
                                        "Timezone: " + timezone + ", Time: " + Time.getFullTime(timezone);
                                ClientLogger.createNewLogEntry(logInfoMessage);
                                informationNotifier.sendInformationMessage(infoNotificationBuilder.toString(), jda);
                            }

                        }

                    } catch (Exception e) {
                        ClientLogger.createNewLogEntry(e.getMessage());
                    }

                }

            }
        }, 0, 60 * 1000); //every minute
    }

    private String prepareMention(String channelId, Guild guild) {
        String roleId = clientCache.getMentionRoleID(channelId);
        String mention = "@everyone";

        try {
            if (!roleId.isBlank()) {
                mention = guild.getRoleById(roleId).getAsMention();
            }
        } catch (NullPointerException ignore) {
            //If this exception happens, no role was set.
        }

        return mention;
    }

    private void addDebugMessageIfInMode(String channel, String timezone, StringBuilder notificationMessageBuilder) {
        if (clientCache.isChannelInDebugMode(channel)) {
            String message =
                    "\n\nCT: " + Time.getFullTime(timezone) + "\n" +
                            "ACT: " + Time.getFullTime("GMT+2") + " \n" +
                            "TZ: " + timezone;
            notificationMessageBuilder.append(message);
        }
    }

}
package com.gmail.nossr50.commands.general;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.nossr50.Users;
import com.gmail.nossr50.m;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcPermissions;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.locale.mcLocale;
import com.gmail.nossr50.skills.Skills;

public class InspectCommand implements CommandExecutor {
    private final mcMMO plugin;

    public InspectCommand(mcMMO instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (player != null  && !mcPermissions.getInstance().whois(player)) {
            sender.sendMessage(ChatColor.YELLOW + "[mcMMO] " + ChatColor.DARK_RED + mcLocale.getString("mcPlayerListener.NoPermission"));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Proper usage is /whois <playername>");
            return true;
        }
        // if split[1] is a player
        if (plugin.getServer().getPlayer(args[0]) != null) 
        {
            Player target = plugin.getServer().getPlayer(args[0]);
            PlayerProfile PPt = Users.getProfile(target);
            
            //If they are not an Op they have to be close
            if(sender instanceof Player && !player.isOp() && !m.isNear(player.getLocation(), target.getLocation(), 5))
            {
                sender.sendMessage("You are too far away to inspect that player!");
            }
            
            sender.sendMessage(ChatColor.GREEN + "mcMMO Stats for " + ChatColor.YELLOW + target.getName());

            sender.sendMessage(ChatColor.GOLD + "-=GATHERING SKILLS=-");
            if (mcPermissions.getInstance().excavation(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.ExcavationSkill"), PPt.getSkillLevel(SkillType.EXCAVATION), PPt.getSkillXpLevel(SkillType.EXCAVATION), PPt.getXpToLevel(SkillType.EXCAVATION)));
            if (mcPermissions.getInstance().fishing(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.FishingSkill"), PPt.getSkillLevel(SkillType.FISHING), PPt.getSkillXpLevel(SkillType.FISHING), PPt.getXpToLevel(SkillType.FISHING)));
            if (mcPermissions.getInstance().herbalism(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.HerbalismSkill"), PPt.getSkillLevel(SkillType.HERBALISM), PPt.getSkillXpLevel(SkillType.HERBALISM), PPt.getXpToLevel(SkillType.HERBALISM)));
            if (mcPermissions.getInstance().mining(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.MiningSkill"), PPt.getSkillLevel(SkillType.MINING), PPt.getSkillXpLevel(SkillType.MINING), PPt.getXpToLevel(SkillType.MINING)));
            if (mcPermissions.getInstance().woodcutting(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.WoodcuttingSkill"), PPt.getSkillLevel(SkillType.WOODCUTTING), PPt.getSkillXpLevel(SkillType.WOODCUTTING), PPt.getXpToLevel(SkillType.WOODCUTTING)));

            sender.sendMessage(ChatColor.GOLD + "-=COMBAT SKILLS=-");
            if (mcPermissions.getInstance().axes(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.AxesSkill"), PPt.getSkillLevel(SkillType.AXES), PPt.getSkillXpLevel(SkillType.AXES), PPt.getXpToLevel(SkillType.AXES)));
            if (mcPermissions.getInstance().archery(player))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.ArcherySkill"), PPt.getSkillLevel(SkillType.ARCHERY), PPt.getSkillXpLevel(SkillType.ARCHERY), PPt.getXpToLevel(SkillType.ARCHERY)));
            if (mcPermissions.getInstance().swords(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.SwordsSkill"), PPt.getSkillLevel(SkillType.SWORDS), PPt.getSkillXpLevel(SkillType.SWORDS), PPt.getXpToLevel(SkillType.SWORDS)));
            if (mcPermissions.getInstance().taming(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.TamingSkill"), PPt.getSkillLevel(SkillType.TAMING), PPt.getSkillXpLevel(SkillType.TAMING), PPt.getXpToLevel(SkillType.TAMING)));
            if (mcPermissions.getInstance().unarmed(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.UnarmedSkill"), PPt.getSkillLevel(SkillType.UNARMED), PPt.getSkillXpLevel(SkillType.UNARMED), PPt.getXpToLevel(SkillType.UNARMED)));

            sender.sendMessage(ChatColor.GOLD + "-=MISC SKILLS=-");
            if (mcPermissions.getInstance().acrobatics(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.AcrobaticsSkill"), PPt.getSkillLevel(SkillType.ACROBATICS), PPt.getSkillXpLevel(SkillType.ACROBATICS), PPt.getXpToLevel(SkillType.ACROBATICS)));
            if (mcPermissions.getInstance().repair(target))
                sender.sendMessage(Skills.getSkillStats(mcLocale.getString("mcPlayerListener.RepairSkill"), PPt.getSkillLevel(SkillType.REPAIR), PPt.getSkillXpLevel(SkillType.REPAIR), PPt.getXpToLevel(SkillType.REPAIR)));

            sender.sendMessage(mcLocale.getString("mcPlayerListener.PowerLevel") + ChatColor.GREEN + (m.getPowerLevel(target)));
        } else {
            sender.sendMessage("That is not a valid player!");
        }

        return true;
    }
}
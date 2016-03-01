package ShowInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ShowInfo.command.ShowInfoCommand;
import ShowInfo.listener.CommandListener;
import ShowInfo.task.ShowInfoTask;
import cn.nukkit.Player;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

public class ShowInfo extends PluginBase{
	private String format;
	private Config offPlayersConfig;
	private Config settingConfig;
	private TaskHandler showinfoTask;
	private int moneyStatus = ShowInfo.ECONOMY_NONE;
	public static final int ECONOMY_NONE = 0;
	public static final int ECONOMY_LEET = 1;
	public static final int ECONOMY_API = 2;

	@Override
	public void onEnable(){
		this.getLogger().info("Finding economy plugin...");
		if(this.getServer().getPluginManager().getPlugin("Economy-LEET") != null){
			this.moneyStatus = ShowInfo.ECONOMY_LEET;
			this.getLogger().info(TextFormat.AQUA + "Found : Economy-LEET");
		}else if(this.getServer().getPluginManager().getPlugin("EconomyAPI") != null){
			this.moneyStatus = ShowInfo.ECONOMY_API;
			this.getLogger().info(TextFormat.AQUA + "Found : Economy-API");
		}else{
			this.getLogger().info(TextFormat.DARK_RED + "Not found economy plugin...");
		}
		this.loadData();
		this.getServer().getCommandMap().register("showinfo", new ShowInfoCommand(this));
		this.getServer().getPluginManager().registerEvents(new CommandListener(this), this);
	}

	@Override
	public void onDisable(){
		this.taskStop();
		this.saveData();
	}

	public void onTaskRun(){
		LinkedHashMap<String, Double> allMoney = this.getAllMoneys();
		LinkedList<String> playerNames = new LinkedList<String>();
		for(String playerName : allMoney.keySet()){
			playerNames.add(playerName);
		}
		Collections.sort(playerNames, new Comparator<String>(){
			@Override
			public int compare(String playerName1, String playerName2){
				Double money1 = allMoney.get(playerName1);
				Double money2 = allMoney.get(playerName2);
				return money1 > money2 ? -1 : (money1 == money2 ? 0 : 1);
			}
		});
		LinkedHashMap<String, Integer> sortRank = new LinkedHashMap<String, Integer>();
		Integer rank = 0;
		for(String playerName : playerNames){
			if(this.getSettingConfig().getBoolean("OpInRank") == false && this.getServer().isOp(playerName) == true){
				sortRank.put(playerName, -1);
			}else{
				rank++;
				sortRank.put(playerName, rank);
			}
		}
		StringBuilder formatBuilder;
		Integer push = this.getSettingConfig().getInt("PushLevel");
		if(push == 0){
			formatBuilder = new StringBuilder(this.format.toString());
		}else{
			StringBuilder pushBuilder = new StringBuilder("");
			for(int i = 0; i < Math.abs(push); i++){
				pushBuilder.append(" ");
			}
			if(push > 0){
				formatBuilder = new StringBuilder(pushBuilder.toString()).append(this.format.replace("\n", "\n" + pushBuilder.toString()));
			}else{
				formatBuilder = new StringBuilder(this.format.replace("\n", pushBuilder.toString() + "\n")).append(pushBuilder.toString());
			}
		}
		String format = formatBuilder.toString();
		Pattern pattern = Pattern.compile("\\{([a-zA-Z]+)\\}");
		Matcher matcher = pattern.matcher(format);
		while(matcher.find() == true){
			switch(matcher.group(1).toString().toLowerCase()){
				case "players":
					format = format.replace(matcher.group(0), String.valueOf(this.getServer().getOnlinePlayers().size()));
					break;
				case "maxplayers":
					format = format.replace(matcher.group(0), String.valueOf(this.getServer().getMaxPlayers()));
					break;
			}
		}
		String playerFormat, replace;
		for(Player player : new ArrayList<>(this.getServer().getOnlinePlayers().values())){
			String playerName = player.getName();
			if(this.getOffPlayersConfig().exists(playerName, true)){
				continue;
			}
			String iplayerName = playerName.toLowerCase();
			playerFormat = format;
			matcher = pattern.matcher(playerFormat);
			while(matcher.find() == true){
				switch(matcher.group(1).toLowerCase()){
					case "player":
						replace = playerName;
						break;
					case "iplayer":
						replace = iplayerName;
						break;
					case "displayName":
						replace = player.getDisplayName();
						break;
					case "nametag":
						replace = player.getNameTag();
						break;
					case "health":
						replace = String.valueOf(player.getHealth());
						break;
					case "maxhealth":
						replace = String.valueOf(player.getMaxHealth());
						break;
					case "healthpercentage":
						replace = String.valueOf((int) (((double) player.getHealth()) / ((double) player.getMaxHealth()) * 100));
						break;
					case "x":
						replace = String.valueOf(Math.ceil(player.x * 10) / 10);
						break;
					case "y":
						replace = String.valueOf(Math.ceil(player.y * 10) / 10);
						break;
					case "z":
						replace = String.valueOf(Math.ceil(player.z * 10) / 10);
						break;
					case "world":
						replace = player.level.getName();
						break;
					case "worldfolder":
						replace = player.level.getFolderName();
						break;
					case "itemid":
						replace = String.valueOf(player.getInventory().getItemInHand().getId());
						break;
					case "itemdamage":
						replace = String.valueOf(player.getInventory().getItemInHand().getDamage());
						break;
					case "itemname":
						replace = player.getInventory().getItemInHand().getName();
						break;
					case "money":
						if(allMoney.containsKey(iplayerName)){
							replace = allMoney.get(iplayerName).toString();
						}else{
							replace = "-";
						}
						break;
					case "rank":
						if(sortRank.containsKey(iplayerName)){
							replace = sortRank.get(iplayerName) == -1 ? "OP" : sortRank.get(iplayerName).toString();
						}else{
							replace = "-";
						}
						break;
					/*
					 * case "": replace = ; break;
					 */
					default:
						continue;
				}
				playerFormat = playerFormat.replace(matcher.group(0), replace);
			}
			player.sendTip(playerFormat);
		}
	}

	public Boolean isTaskStop(){
		return (this.showinfoTask == null || this.showinfoTask.isCancelled());
	}

	public void taskStart(){
		if(this.isTaskStop() != true){
			this.taskStop();
		}
		this.showinfoTask = this.getServer().getScheduler().scheduleRepeatingTask(new ShowInfoTask(this), this.getSettingConfig().getInt("Tick"));
	}

	public void setTaskPeriod(Integer period){
		if(this.isTaskStop() != true){
			this.showinfoTask.setPeriod(period);
		}
	}

	public void taskStop(){
		if(this.isTaskStop() != true){
			this.getServer().getScheduler().cancelTask(this.showinfoTask.getTaskId());
		}
	}

	public void loadData(){
		this.saveDefaultData(false);
		this.offPlayersConfig = new Config(new File(this.getDataFolder(), "/OffPlayers.yml"));
		this.settingConfig = new Config(new File(this.getDataFolder(), "/Setting.yml"), Config.YAML, new LinkedHashMap<String, Object>(){
			private static final long serialVersionUID = 1L;
			{
				put("Enable", true);
				put("OpInRank", false);
				put("PushLevel", 0);
				put("Tick", 20);
			}
		});
		try{
			this.format = Utils.readFile(new File(this.getDataFolder(), "Format.txt"));
		}catch(IOException e){
			this.getServer().getLogger().logException(e);
		}
		if(this.getSettingConfig().getBoolean("Enable")){
			this.getLogger().info("ShowInfo is running!");
			this.taskStart();
		}else{
			this.getLogger().info("ShowInfo is not run...");
		}
	}

	public void saveData(){
		this.offPlayersConfig.save();
		this.settingConfig.save();
	}

	public void saveDefaultData(Boolean replace){
		this.saveResource("resource/Format.txt", "Format.txt", replace);
		this.saveResource("resource/OffPlayers.yml", "OffPlayers.yml", replace);
		this.saveResource("resource/Setting.yml", "Setting.yml", replace);
	}

	public String getFomat(){
		return this.format;
	}

	public Config getOffPlayersConfig(){
		return this.offPlayersConfig;
	}

	public Config getSettingConfig(){
		return this.settingConfig;
	}

	public LinkedHashMap<String, Double> getAllMoneys(){
		switch(this.moneyStatus){
			case ShowInfo.ECONOMY_LEET:
				return new LinkedHashMap<String, Double>(cc.leet.economy.Economy.getPlugin().getAPI().getBalanceAll());
			case ShowInfo.ECONOMY_API:
				return me.onebone.economyapi.EconomyAPI.getInstance().getAllMoney();
			default:
				return new LinkedHashMap<String, Double>();
		}
	}
}

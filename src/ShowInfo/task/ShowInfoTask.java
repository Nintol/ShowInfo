package ShowInfo.task;

import ShowInfo.ShowInfo;
import cn.nukkit.scheduler.PluginTask;

public class ShowInfoTask extends PluginTask<ShowInfo>{
	public ShowInfoTask(ShowInfo owner){
		super(owner);
	}

	@Override
	public void onRun(int currentTick){
		ShowInfo plugin = this.getOwner();
		new Thread(new Runnable(){
			@Override
			public void run(){
				plugin.onTaskRun();
			}
		}).start();
	}
}

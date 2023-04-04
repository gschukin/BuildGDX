package ru.m210projects.Build.Pattern;

import static ru.m210projects.Build.Net.Mmulti.*;

import com.badlogic.gdx.Screen;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.OnSceenDisplay.Console;
import ru.m210projects.Build.Pattern.BuildGame.NetMode;
import ru.m210projects.Build.Pattern.ScreenAdapters.GameAdapter;
import ru.m210projects.Build.Pattern.ScreenAdapters.InitScreen;
import ru.m210projects.Build.Pattern.ScreenAdapters.LoadingAdapter;

public class BuildEngine extends Engine {

	protected int ticks;
	protected long timerskipticks;
	protected float frametime;

	private final BuildGame game;

	public BuildEngine(BuildGame game, int ticks) throws Exception {
		super();
		this.game = game;
		this.ticks = ticks;
	}

	@Override
	public void inittimer(int tickspersecond) {
		super.inittimer(tickspersecond);

		timerskipticks = (timerfreq / timerticspersec) * ticks;
		updatesmoothticks();
	}

	public int getsmoothratio() {
//		return ((totalclock - game.pNet.ototalclock + ticks) << 16) / ticks;
//		return (int) (((System.nanoTime() - timernexttick) * 65536.0f) / (timerskipticks * 1000000.0f));
		return (int) ((frametime += BuildGdx.graphics.getDeltaTime() * 1000.0f * 65536.0f) / timerskipticks);
	}

	@Override
	public void faketimerhandler() {
		BuildNet net = game.pNet;
		if (net == null)
			return; // not initialized yet

		Screen current = game.getScreen();
		if (!(current instanceof GameAdapter) && !(current instanceof LoadingAdapter)
				&& !(current instanceof InitScreen))
			handleevents();

//		if (totalclock < net.ototalclock + ticks || !net.ready2send)
//			return;
//
//		net.ototalclock = totalclock;

		if (totalclock < net.ototalclock || !net.ready2send)
			return;

		net.ototalclock += ticks;
		updatesmoothticks();
		handleevents();
		GetInput(net);
	}

	public void updatesmoothticks() {
		game.pInt.requestUpdating();
		frametime = 0.0f;
	}

	@Override
	public void dragpoint(int pointhighlight, int dax, int day) {
		game.pInt.setwallinterpolate(pointhighlight, getWall()[pointhighlight]);
		getWall()[pointhighlight].setX(dax);
		getWall()[pointhighlight].setY(day);

		int cnt = MAXWALLS;
		int tempshort = pointhighlight; // search points CCW
		do {
			if (getWall()[tempshort].getNextwall() >= 0) {
				tempshort = getWall()[getWall()[tempshort].getNextwall()].getPoint2();
				game.pInt.setwallinterpolate(tempshort, getWall()[tempshort]);
				getWall()[tempshort].setX(dax);
				getWall()[tempshort].setY(day);
			} else {
				tempshort = pointhighlight; // search points CW if not searched all the way around
				do {
					if (getWall()[lastwall(tempshort)].getNextwall() >= 0) {
						tempshort = getWall()[lastwall(tempshort)].getNextwall();
						game.pInt.setwallinterpolate(tempshort, getWall()[tempshort]);
						getWall()[tempshort].setX(dax);
						getWall()[tempshort].setY(day);
					} else
						break;

					cnt--;
				} while ((tempshort != pointhighlight) && (cnt > 0));
				break;
			}
			cnt--;
		} while ((tempshort != pointhighlight) && (cnt > 0));
	}

	@Override
	public void handleevents() {
		super.handleevents();
		game.pInput.gpmanager.handler();
	}

	protected void GetInput(BuildNet net) {
		if (numplayers > 1)
			net.GetPackets();

		for (int i = connecthead; i >= 0; i = connectpoint2[i])
			if (i != myconnectindex && net.gNetFifoHead[myconnectindex] - 200 > net.gNetFifoHead[i])
				return;

		if (!game.pMenu.gShowMenu && !Console.IsShown()) {
			game.pInput.ctrlMouseHandler();
			game.pInput.ctrlJoyHandler();
		}
		game.pInput.ctrlGetInput(game.pNet.gInput);

		if ((net.gNetFifoHead[myconnectindex] & (net.MovesPerPacket - 1)) != 0) {
			net.gFifoInput[net.gNetFifoHead[myconnectindex] & 0xFF][myconnectindex]
					.Copy(net.gFifoInput[(net.gNetFifoHead[myconnectindex] - 1) & 0xFF][myconnectindex]);
			net.gNetFifoHead[myconnectindex]++;
			return;
		}

		net.gFifoInput[net.gNetFifoHead[myconnectindex] & 0xFF][myconnectindex].Copy(net.gInput);
		net.gNetFifoHead[myconnectindex]++;

		if (game.nNetMode == NetMode.Multiplayer && numplayers < 2) {
			for (int i = connecthead; i >= 0; i = connectpoint2[i]) {
				if (i != myconnectindex) {
					net.ComputerInput(i);
					net.gNetFifoHead[i]++;
				}
			}
			return;
		}

		net.GetNetworkInput();
	}
}

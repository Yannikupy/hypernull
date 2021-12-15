package ru.croccode.hypernull.bot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import ru.croccode.hypernull.domain.MatchMode;
import ru.croccode.hypernull.geometry.Offset;
import ru.croccode.hypernull.geometry.Point;
import ru.croccode.hypernull.geometry.Size;
import ru.croccode.hypernull.io.SocketSession;
import ru.croccode.hypernull.message.Hello;
import ru.croccode.hypernull.message.MatchOver;
import ru.croccode.hypernull.message.MatchStarted;
import ru.croccode.hypernull.message.Move;
import ru.croccode.hypernull.message.Register;
import ru.croccode.hypernull.message.Update;

public class StarterBot implements Bot {

	private static final Random rnd = new Random(System.currentTimeMillis());

	private final MatchMode mode;

	private Offset moveOffset;

	private int moveCounter = 0;

	private Size map_size;

	private boolean[][] map;

	private Integer id;

	private Point current_place;

	public StarterBot(MatchMode mode) {
		this.mode = mode;
	}


	@Override
	public Register onHello(Hello hello) {
		Register register = new Register();
		register.setMode(mode);
		register.setBotName("Yan-Borisov");
		return register;
	}

	@Override
	public void onMatchStarted(MatchStarted matchStarted) {
		id = matchStarted.getYourId();
		map_size = matchStarted.getMapSize();
		map = new boolean[matchStarted.getMapSize().width()][matchStarted.getMapSize().height()];
	}

	@Override
	public Move onUpdate(Update update) {
		current_place = update.getBots().get(id);
		Set<Point> blocks = update.getBlocks();
		for(Point block : blocks){
			map[block.x()][block.y()] = true;
		}
		Set<Point> coins = update.getCoins();
		if (moveOffset == null || moveCounter > 5 + rnd.nextInt(5)) {
			moveOffset = new Offset(
					rnd.nextInt(3) - 1,
					rnd.nextInt(3) - 1);
			moveCounter = 0;
		}
		if(map[current_place.apply(moveOffset, map_size).x()][current_place.apply(moveOffset, map_size).y()]){
			moveOffset = new Offset(
					-rnd.nextInt(3) - 1,
					-rnd.nextInt(3) - 1);
		}
		if(coins != null) {
			for (Point coin : coins) {
				moveOffset = new Offset(coin.x() - current_place.x(), coin.y() - current_place.y());
			}
		}
		moveOffset = new Offset(
				rnd.nextInt(3) - 1,
				rnd.nextInt(3) - 1);
		Move move = new Move();
		move.setOffset(moveOffset);
		System.out.println(Arrays.deepToString(map));
		return move;
	}
	@Override
	public void onMatchOver(MatchOver matchOver) {
		System.out.println("Match ended");
	}

	public static void main(String[] args) throws IOException {
		Socket socket = new Socket();
		socket.setTcpNoDelay(true);
		socket.setSoTimeout(300_000);
		socket.connect(new InetSocketAddress("localhost", 2021));

		SocketSession session = new SocketSession(socket);
		StarterBot bot = new StarterBot(MatchMode.FRIENDLY);
		new BotMatchRunner(bot, session).run();
	}
}

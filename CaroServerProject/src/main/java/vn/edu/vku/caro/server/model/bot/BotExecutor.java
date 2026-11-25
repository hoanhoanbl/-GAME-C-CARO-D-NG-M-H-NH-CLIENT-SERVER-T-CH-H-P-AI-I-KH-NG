package vn.edu.vku.caro.server.model.bot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotExecutor {
    public static final ExecutorService POOL = Executors.newFixedThreadPool(2);
}

package com.github.kuramastone.cobblemonChallenges;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ChallengeExecutors {
    public static final ExecutorService CHALLENGE_EXECUTOR =
        Executors.newFixedThreadPool(
            1,
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Challenges-Thread-%d")
                .build()
        );

    static {
        CHALLENGE_EXECUTOR.submit(() -> {
            CobbleChallengeMod.logger.info("Challenge executor thread started: {}", Thread.currentThread().getName());
        });
    }
}